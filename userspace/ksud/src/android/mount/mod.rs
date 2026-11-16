//! Built-in module mount controller: OverlayFS + Magic Mount.
//!
//! Ported from ZySU's `sumhp` (`mount/backend.cpp`, `overlayfs.cpp`,
//! `magic_mount.cpp`) minus the SUMH backend, which needs a kernel module our
//! project does not ship. The controller replaces the metamodule dependency:
//! `ksud` mounts enabled modules itself during `post-fs-data`, and the backend
//! can be switched globally or per module from the manager.

pub mod cli;
mod fsutil;
mod magic;
mod overlay;

use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};
use std::time::{SystemTime, UNIX_EPOCH};

use anyhow::{Context, Result, anyhow, bail};
use log::{info, warn};
use serde::{Deserialize, Serialize};

use crate::android::module::{metamodule, read_module_prop};
use crate::android::utils;
use crate::defs;

pub use fsutil::MOUNT_SOURCE;

const DEFAULT_PARTITIONS: [&str; 6] = ["system", "vendor", "product", "system_ext", "odm", "oem"];
const MAX_BOOT_ATTEMPTS: i32 = 3;

pub const MODE_AUTO: &str = "auto";
pub const MODE_OVERLAY: &str = "overlay";
pub const MODE_MAGIC: &str = "magic";
pub const MODE_NONE: &str = "none";

// ---------------------------------------------------------------- config

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    #[serde(default = "default_true")]
    pub enabled: bool,
    /// `auto` | `overlay` | `magic` | `none`；非 auto 时强制所有模块使用该后端。
    #[serde(default = "default_auto")]
    pub backend: String,
    #[serde(default = "default_true")]
    pub overlayfs: bool,
    #[serde(default = "default_true")]
    pub magic_mount: bool,
    #[serde(default = "default_source")]
    pub mount_source: String,
    #[serde(default = "default_work_dir")]
    pub work_dir: String,
    /// 额外纳入管理的分区，默认六个 GKI 分区。
    #[serde(default)]
    pub partitions: Vec<String>,
    /// 逐模块模式覆盖：id -> auto/overlay/magic/none
    #[serde(default)]
    pub modules: BTreeMap<String, String>,
}

fn default_true() -> bool {
    true
}
fn default_auto() -> String {
    MODE_AUTO.to_string()
}
fn default_source() -> String {
    MOUNT_SOURCE.to_string()
}
fn default_work_dir() -> String {
    defs::MOUNT_WORK_DIR.to_string()
}

impl Default for Config {
    fn default() -> Self {
        Self {
            enabled: true,
            backend: default_auto(),
            overlayfs: true,
            magic_mount: true,
            mount_source: default_source(),
            work_dir: default_work_dir(),
            partitions: Vec::new(),
            modules: BTreeMap::new(),
        }
    }
}

impl Config {
    pub fn load() -> Self {
        let Ok(content) = fs::read_to_string(defs::MOUNT_CONFIG_PATH) else {
            return Self::default();
        };
        match serde_json::from_str::<Config>(&content) {
            Ok(config) => config,
            Err(e) => {
                warn!("invalid {}: {e}, using defaults", defs::MOUNT_CONFIG_PATH);
                Self::default()
            }
        }
    }

    pub fn save(&self) -> Result<()> {
        let path = Path::new(defs::MOUNT_CONFIG_PATH);
        if let Some(parent) = path.parent() {
            fsutil::ensure_dir(parent)?;
        }
        let tmp = path.with_extension("json.tmp");
        let content = serde_json::to_string_pretty(self)?;
        fs::write(&tmp, content).with_context(|| format!("write {}", tmp.display()))?;
        fs::rename(&tmp, path).with_context(|| format!("rename to {}", path.display()))?;
        Ok(())
    }

    /// 默认六个分区 + 用户追加的分区，去重且保持顺序。
    pub fn managed_partitions(&self) -> Vec<String> {
        let mut out: Vec<String> = DEFAULT_PARTITIONS.iter().map(|p| p.to_string()).collect();
        for part in &self.partitions {
            if !part.is_empty() && !out.contains(part) {
                out.push(part.clone());
            }
        }
        out
    }

    pub fn module_mode(&self, id: &str) -> &str {
        self.modules
            .get(id)
            .map(String::as_str)
            .unwrap_or(MODE_AUTO)
    }
}

// ---------------------------------------------------------------- modules

#[derive(Debug, Clone)]
pub struct ModuleEntry {
    pub id: String,
    pub path: PathBuf,
    pub name: String,
}

fn module_has_content(module: &ModuleEntry, partitions: &[String]) -> bool {
    partitions
        .iter()
        .any(|part| module.path.join(part).is_dir())
}

/// Files/symlinks directly inside a partition tree. OverlayFS only stacks on
/// leaf directories, so a module that puts `system/build.prop` in place has to
/// go through Magic Mount.
fn dir_has_direct_files(dir: &Path, partitions: &[String], system_root: bool) -> bool {
    if !dir.is_dir() {
        return false;
    }
    let Ok(entries) = fs::read_dir(dir) else {
        return false;
    };
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        let is_dir = entry.file_type().map(|t| t.is_dir()).unwrap_or(false);
        if system_root && partitions.iter().any(|p| *p == name) && is_dir {
            // 这些分区在根目录单独处理，不算在 system 的直属文件里
            continue;
        }
        if !is_dir {
            return true;
        }
    }
    false
}

fn module_needs_magic(module: &ModuleEntry, partitions: &[String]) -> bool {
    if dir_has_direct_files(&module.path.join("system"), partitions, true) {
        return true;
    }
    partitions.iter().any(|part| {
        part != "system"
            && (dir_has_direct_files(&module.path.join(part), partitions, false)
                || dir_has_direct_files(&module.path.join("system").join(part), partitions, false))
    })
}

/// 启用中的模块，按 id 排序（与 ksud 其它地方一致）。
pub fn enumerate_modules(config: &Config) -> Vec<ModuleEntry> {
    let partitions = config.managed_partitions();
    let root = Path::new(defs::MODULE_DIR);
    let Ok(entries) = fs::read_dir(root) else {
        return Vec::new();
    };
    let mut modules = Vec::new();
    for entry in entries.flatten() {
        let path = entry.path();
        if !path.is_dir() {
            continue;
        }
        if path.join(defs::DISABLE_FILE_NAME).exists()
            || path.join(defs::REMOVE_FILE_NAME).exists()
            || path.join("skip_mount").exists()
        {
            continue;
        }
        if !path.join("module.prop").is_file() {
            continue;
        }
        let Ok(props) = read_module_prop(&path) else {
            continue;
        };
        if metamodule::is_metamodule(&props) {
            continue;
        }
        let Some(id) = path.file_name().and_then(|name| name.to_str()) else {
            continue;
        };
        let module = ModuleEntry {
            id: id.to_string(),
            path: path.clone(),
            name: props
                .get("name")
                .cloned()
                .filter(|name| !name.trim().is_empty())
                .unwrap_or_else(|| id.to_string()),
        };
        if !module_has_content(&module, &partitions) {
            continue;
        }
        modules.push(module);
    }
    modules.sort_by(|a, b| a.id.cmp(&b.id));
    modules
}

fn fallback_backend(module: &ModuleEntry, config: &Config, partitions: &[String]) -> &'static str {
    if config.overlayfs && fsutil::overlay_supported() && !module_needs_magic(module, partitions) {
        return MODE_OVERLAY;
    }
    if config.magic_mount {
        return MODE_MAGIC;
    }
    MODE_NONE
}

pub fn resolve_backend(module: &ModuleEntry, config: &Config) -> &'static str {
    let partitions = config.managed_partitions();
    let requested = if config.backend != MODE_AUTO && !config.backend.is_empty() {
        config.backend.clone()
    } else {
        config.module_mode(&module.id).to_string()
    };
    match requested.as_str() {
        MODE_OVERLAY => {
            if config.overlayfs
                && fsutil::overlay_supported()
                && !module_needs_magic(module, &partitions)
            {
                MODE_OVERLAY
            } else {
                fallback_backend(module, config, &partitions)
            }
        }
        MODE_MAGIC => {
            if config.magic_mount {
                MODE_MAGIC
            } else {
                fallback_backend(module, config, &partitions)
            }
        }
        MODE_NONE => MODE_NONE,
        _ => fallback_backend(module, config, &partitions),
    }
}

// ---------------------------------------------------------------- state

#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MountState {
    #[serde(default)]
    pub ok: bool,
    #[serde(default)]
    pub backend: String,
    #[serde(default)]
    pub modules: usize,
    #[serde(default)]
    pub mounts: usize,
    #[serde(default)]
    pub detail: String,
    #[serde(default)]
    pub at: u64,
}

impl MountState {
    pub fn load() -> Self {
        fs::read_to_string(defs::MOUNT_STATE_PATH)
            .ok()
            .and_then(|content| serde_json::from_str(&content).ok())
            .unwrap_or_default()
    }

    fn save(&self) -> Result<()> {
        fsutil::ensure_dir(Path::new(defs::MOUNT_RUN_DIR))?;
        let tmp = format!("{}.tmp", defs::MOUNT_STATE_PATH);
        fs::write(&tmp, serde_json::to_string_pretty(self)?)?;
        fs::rename(&tmp, defs::MOUNT_STATE_PATH)?;
        Ok(())
    }
}

fn now_seconds() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0)
}

// ---------------------------------------------------------------- boot guard

fn read_trimmed(path: &str) -> Option<String> {
    fs::read_to_string(path)
        .ok()
        .map(|value| value.trim().to_string())
}

fn current_boot_id() -> String {
    read_trimmed("/proc/sys/kernel/random/boot_id").unwrap_or_default()
}

fn boot_attempts() -> i32 {
    read_trimmed(defs::MOUNT_BOOT_ATTEMPTS)
        .and_then(|value| value.parse().ok())
        .unwrap_or(0)
}

fn write_boot_attempts(attempts: i32) {
    let _ = fsutil::ensure_dir(Path::new(defs::MOUNT_RUN_DIR));
    let _ = fs::write(defs::MOUNT_BOOT_ATTEMPTS, format!("{attempts}\n"));
}

pub fn boot_disabled() -> bool {
    Path::new(defs::MOUNT_DISABLED).exists()
}

/// 同一 boot 内只挂一次；marker 里记 boot_id，重启自动失效。
fn claim_boot_marker() -> bool {
    let current = current_boot_id();
    if read_trimmed(defs::MOUNT_BOOT_MARKER).is_some_and(|value| value == current) {
        return false;
    }
    let _ = fsutil::ensure_dir(Path::new(defs::MOUNT_RUN_DIR));
    let _ = fs::write(defs::MOUNT_BOOT_MARKER, current);
    true
}

/// 开机完成由 init_event 调用：清掉失败计数与保护开关。
pub fn on_boot_completed() {
    write_boot_attempts(0);
    let _ = fs::remove_file(defs::MOUNT_DISABLED);
}

pub fn reset_recovery() -> Result<()> {
    write_boot_attempts(0);
    let _ = fs::remove_file(defs::MOUNT_DISABLED);
    let _ = fs::remove_file(defs::MOUNT_BOOT_MARKER);
    Ok(())
}

// ---------------------------------------------------------------- apply

#[derive(Debug, Default)]
pub struct ApplyReport {
    pub ok: bool,
    pub backend: String,
    pub modules: usize,
    pub mounts: usize,
    pub detail: String,
}

fn boot_completed() -> bool {
    utils::getprop("sys.boot_completed").as_deref() == Some("1")
}

pub fn apply(force: bool) -> Result<ApplyReport> {
    let config = Config::load();
    let mut report = ApplyReport::default();

    if !config.enabled {
        report.detail = "mount controller disabled".to_string();
        return Ok(report);
    }
    if metamodule::has_metamodule() && !force {
        report.detail = "metamodule installed; built-in mount controller stays out of the way"
            .to_string();
        info!("{}", report.detail);
        return Ok(report);
    }
    if boot_completed() && !force {
        bail!("mounting must happen during boot; reboot or pass --force");
    }
    if boot_disabled() && !force {
        bail!("mounting disabled by bootloop protection; run `ksud mount reset`");
    }
    if !force && !claim_boot_marker() {
        bail!("module mount already ran this boot");
    }

    let attempts = boot_attempts();
    if attempts >= MAX_BOOT_ATTEMPTS && !force {
        let _ = fs::write(defs::MOUNT_DISABLED, "\n");
        bail!(
            "bootloop protection tripped after {attempts} unconfirmed boots; \
             run `ksud mount reset` after fixing the module"
        );
    }
    write_boot_attempts(attempts + 1);

    let modules = enumerate_modules(&config);
    report.modules = modules.len();

    let mut overlay_modules = Vec::new();
    let mut magic_modules = Vec::new();
    for module in &modules {
        let backend = resolve_backend(module, &config);
        info!("module={} backend={backend}", module.id);
        match backend {
            MODE_OVERLAY => overlay_modules.push(module.clone()),
            MODE_MAGIC => magic_modules.push(module.clone()),
            _ => {}
        }
    }

    // 从干净状态开始：先拆掉上一次（本次 boot 内重试 / 手动 apply）的挂载。
    let _ = unmount_all(&config);

    let mut ok = true;
    if !magic_modules.is_empty() {
        match magic::mount_modules(&magic_modules, &config) {
            Ok(count) => info!("magic mount: {} modules, {count} mounts", magic_modules.len()),
            Err(e) => {
                warn!("magic mount failed: {e:#}");
                ok = false;
            }
        }
    }
    if !overlay_modules.is_empty() {
        match overlay::mount_modules(&overlay_modules, &config) {
            Ok(count) => info!(
                "overlay mount: {} modules, {count} mounts",
                overlay_modules.len()
            ),
            Err(e) => {
                warn!("overlay mount failed: {e:#}");
                ok = false;
            }
        }
    }

    report.backend = format!(
        "hybrid(overlay={},magic={})",
        overlay_modules.len(),
        magic_modules.len()
    );
    report.mounts = overlay::active_mounts().len() + magic::active_mounts().len();
    report.ok = ok;
    report.detail = if ok {
        "ok".to_string()
    } else {
        "some backends reported errors, see mount log".to_string()
    };

    MountState {
        ok,
        backend: report.backend.clone(),
        modules: report.modules,
        mounts: report.mounts,
        detail: report.detail.clone(),
        at: now_seconds(),
    }
    .save()?;

    Ok(report)
}

pub fn unmount_all(config: &Config) -> Result<()> {
    let mut errors = Vec::new();
    if let Err(e) = overlay::unmount_all() {
        errors.push(format!("overlay: {e:#}"));
    }
    if let Err(e) = magic::unmount_all(config) {
        errors.push(format!("magic: {e:#}"));
    }
    if errors.is_empty() {
        Ok(())
    } else {
        Err(anyhow!(errors.join("; ")))
    }
}

// ---------------------------------------------------------------- status

#[derive(Debug, Serialize)]
pub struct ModuleStatus {
    pub id: String,
    pub name: String,
    pub mode: String,
    pub backend: String,
    pub mounted: bool,
}

pub fn module_statuses(config: &Config) -> Vec<ModuleStatus> {
    enumerate_modules(config)
        .into_iter()
        .map(|module| {
            let mode = if config.backend != MODE_AUTO {
                config.backend.clone()
            } else {
                config.module_mode(&module.id).to_string()
            };
            let backend = resolve_backend(&module, config).to_string();
            ModuleStatus {
                id: module.id,
                name: module.name,
                mode,
                mounted: backend != MODE_NONE,
                backend,
            }
        })
        .collect()
}

pub fn mounted_paths() -> Vec<String> {
    let mut paths = overlay::active_mounts();
    paths.extend(magic::active_mounts());
    paths.sort();
    paths.dedup();
    paths
}

#[derive(Debug, Serialize)]
pub struct Status {
    pub enabled: bool,
    pub mounted: bool,
    pub backend: String,
    pub applied: String,
    pub overlay_supported: bool,
    pub magic_supported: bool,
    pub modules: usize,
    pub mounts: usize,
    pub metamodule: bool,
    pub disabled: bool,
    pub detail: String,
}

pub fn status() -> Status {
    let config = Config::load();
    let state = MountState::load();
    let paths = mounted_paths();
    Status {
        enabled: config.enabled,
        mounted: !paths.is_empty(),
        backend: config.backend.clone(),
        applied: state.backend.clone(),
        overlay_supported: fsutil::overlay_supported(),
        magic_supported: true,
        modules: enumerate_modules(&config).len(),
        mounts: paths.len(),
        metamodule: metamodule::has_metamodule(),
        disabled: boot_disabled(),
        detail: state.detail.clone(),
    }
}

// ---------------------------------------------------------------- mutations

pub fn set_enabled(enabled: bool) -> Result<()> {
    let mut config = Config::load();
    config.enabled = enabled;
    config.save()
}

pub fn set_backend(backend: &str) -> Result<()> {
    let backend = match backend {
        MODE_AUTO | MODE_OVERLAY | MODE_MAGIC | MODE_NONE => backend,
        other => bail!("unknown backend: {other}"),
    };
    let mut config = Config::load();
    config.backend = backend.to_string();
    config.save()
}

pub fn set_module_mode(id: &str, mode: &str) -> Result<()> {
    let mode = match mode {
        MODE_AUTO | MODE_OVERLAY | MODE_MAGIC | MODE_NONE => mode,
        other => bail!("unknown mode: {other}"),
    };
    if !Path::new(defs::MODULE_DIR).join(id).is_dir() {
        bail!("module not found: {id}");
    }
    let mut config = Config::load();
    if mode == MODE_AUTO {
        config.modules.remove(id);
    } else {
        config.modules.insert(id.to_string(), mode.to_string());
    }
    config.save()
}

