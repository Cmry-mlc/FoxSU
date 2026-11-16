//! SUSFS guard LKM：加载 / 卸载 / 开机自动加载。
//!
//! 内核没有内建 SUSFS 时，可以从 ksud 内嵌的 `{kmi}_susfs_guard_lkm.ko`
//! 动态加载一个守卫模块，拿到 SUSFS 的隐藏能力。加载方式和 `late_load`
//! 加载 kernelsu.ko 完全一致：从内存做符号重定位后走 init_module，
//! 不需要设备上有 insmod，也不依赖内核导出的 kallsyms。
//!
//! 模块本身（kernel/susfs）会在 init 时检测内核是否已内建 SUSFS，
//! 有则直接 -EEXIST 拒绝，所以这里先查一次，避免给用户报一个看不懂的错。

use std::ffi::CString;
use std::path::Path;

use anyhow::{Context, Result, ensure};
use log::{info, warn};

use crate::android::module::module_config::{ConfigType, get_config_value, set_config_value};
use crate::android::susfs::api::features::show;

/// 与 kernel/susfs 的 obj-m 同名，也是 /sys/module 下的目录名。
const MODULE_SYSFS: &str = "/sys/module/susfs_guard_lkm";

/// 自动加载配置挂在 ksud 的持久化配置里，和上游 KernelSU-Next 的做法一致。
const CONFIG_MODULE_ID: &str = "internal.ksud.susfslkm";
const CONFIG_ENABLED_KEY: &str = "autoload.enabled";
const CONFIG_KMI_KEY: &str = "autoload.kmi";

fn validate_kmi(kmi: &str) -> Result<()> {
    ensure!(
        !kmi.is_empty()
            && kmi.len() <= 64
            && kmi
                .bytes()
                .all(|b| b.is_ascii_alphanumeric() || matches!(b, b'.' | b'_' | b'-')),
        "Invalid KMI '{kmi}'"
    );
    Ok(())
}

/// 模块可能自己隐藏 /proc/modules，但 /sys/module 下的目录始终在。
pub fn is_loaded() -> bool {
    Path::new(MODULE_SYSFS).exists()
}

/// 内核里已经有 SUSFS（内建或别的 LKM），此时加载必然失败。
fn susfs_already_available() -> Option<String> {
    show::version().ok().filter(|v| v != "Unsupported")
}

fn resolve_kmi(kmi: Option<&str>) -> Result<String> {
    match kmi {
        Some(kmi) => {
            validate_kmi(kmi)?;
            Ok(kmi.to_string())
        }
        None => crate::boot_patch::get_current_kmi()
            .context("Failed to detect kernel KMI; pass --kmi <KMI> explicitly"),
    }
}

/// 加载模块。已经可用时直接返回，不重复 insmod。
pub fn load(kmi: Option<&str>) -> Result<()> {
    ensure!(
        cfg!(target_arch = "aarch64"),
        "the SUSFS guard LKM is only available for arm64"
    );

    if is_loaded() {
        println!("SUSFS guard LKM is already loaded");
        return Ok(());
    }
    if let Some(version) = susfs_already_available() {
        println!("SUSFS {version} is already provided by the kernel, nothing to load");
        return Ok(());
    }

    let kmi = resolve_kmi(kmi)?;
    let asset_name = format!("{kmi}_susfs_guard_lkm.ko");
    let module_data = crate::assets::get_asset(&asset_name)
        .with_context(|| format!("No SUSFS guard LKM bundled for KMI {kmi}"))?;

    info!("loading SUSFS guard LKM for KMI {kmi}");
    let params = CString::new("").expect("empty module parameters are valid");
    ksuinit::load_module(&module_data, &params)
        .with_context(|| format!("Failed to load SUSFS guard LKM for KMI {kmi}"))?;

    // init 返回 0 只代表内核接受了模块；模块自己还会逐层挂钩子，
    // 有致命层失败时它会在 init 里回滚并让加载失败，所以这里再确认一次。
    ensure!(is_loaded(), "SUSFS guard LKM load returned success but {MODULE_SYSFS} is missing");
    println!("Loaded SUSFS guard LKM for KMI {kmi}");
    Ok(())
}

pub fn unload() -> Result<()> {
    if !is_loaded() {
        println!("SUSFS guard LKM is not loaded");
        return Ok(());
    }
    rustix::system::delete_module(c"susfs_guard_lkm", 0)
        .context("Failed to unload SUSFS guard LKM")?;
    println!("Unloaded SUSFS guard LKM");
    Ok(())
}

/// 给管理器解析的状态行，格式固定：`key=value`，每行一个。
pub fn status() {
    let loaded = is_loaded();
    println!("loaded={}", u8::from(loaded));
    println!("arch={}", std::env::consts::ARCH);
    match susfs_already_available() {
        Some(version) => {
            println!("available=1");
            println!("version={version}");
        }
        None => {
            println!("available=0");
            println!("version=");
        }
    }
    println!("autoload={}", u8::from(autoload_enabled()));
    if let Some(kmi) = autoload_kmi() {
        println!("autoload_kmi={kmi}");
    }
}

pub fn autoload_enabled() -> bool {
    get_config_value(CONFIG_MODULE_ID, CONFIG_ENABLED_KEY, ConfigType::Persist)
        .ok()
        .flatten()
        .is_some_and(|v| matches!(v.trim(), "1" | "true" | "yes"))
}

pub fn autoload_kmi() -> Option<String> {
    get_config_value(CONFIG_MODULE_ID, CONFIG_KMI_KEY, ConfigType::Persist)
        .ok()
        .flatten()
        .filter(|v| validate_kmi(v).is_ok())
}

pub fn set_autoload(enabled: bool, kmi: Option<&str>) -> Result<()> {
    if !enabled {
        set_config_value(CONFIG_MODULE_ID, CONFIG_ENABLED_KEY, "false", ConfigType::Persist)
            .context("Failed to disable SUSFS auto-load")?;
        println!("SUSFS auto-load disabled");
        return Ok(());
    }

    ensure!(
        cfg!(target_arch = "aarch64"),
        "the SUSFS guard LKM is only available for arm64"
    );

    let kmi = resolve_kmi(kmi)?;
    let asset_name = format!("{kmi}_susfs_guard_lkm.ko");
    ensure!(
        crate::assets::get_asset(&asset_name).is_ok(),
        "No SUSFS guard LKM bundled for KMI {kmi}"
    );

    set_config_value(CONFIG_MODULE_ID, CONFIG_KMI_KEY, &kmi, ConfigType::Persist)
        .context("Failed to save SUSFS auto-load KMI")?;
    set_config_value(CONFIG_MODULE_ID, CONFIG_ENABLED_KEY, "true", ConfigType::Persist)
        .context("Failed to enable SUSFS auto-load")?;
    println!("SUSFS auto-load enabled (KMI {kmi})");
    Ok(())
}

/// post-fs-data 阶段调用：开关打开、模块还没加载、内核也没有内建 SUSFS 时才动手。
/// 任何失败都只记日志，不能影响开机流程。
pub fn load_if_autoload_enabled() {
    if !autoload_enabled() || is_loaded() || susfs_already_available().is_some() {
        return;
    }
    let kmi = autoload_kmi()
        .or_else(|| crate::boot_patch::get_current_kmi().ok());
    let Some(kmi) = kmi else {
        warn!("SUSFS auto-load is enabled but no usable KMI is known");
        return;
    };
    if let Err(e) = load(Some(&kmi)) {
        warn!("SUSFS auto-load failed for KMI {kmi}: {e:#}");
    }
}

/// 供管理器展示的可选 KMI 列表：只列真正打包进 ksud 的模块。
pub fn supported_kmi() -> Vec<String> {
    crate::assets::list_supported_susfs_kmi()
}
