//! OverlayFS backend.
//!
//! Only depth-1 leaves under a partition root are ever overlaid (`/system/app`
//! and friends, never `/system` itself): overlaying a partition root would
//! shadow its bind sub-mounts (`/system/vendor`, `/vendor/qcrild`, ...) and
//! break the device. Ported from ZySU's `overlayfs.cpp` minus the SUMH-specific
//! xattr hiding.

use std::collections::BTreeMap;
use std::fs;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, bail};
use log::{debug, info, warn};

use super::fsutil;
use super::{Config, MODE_OVERLAY, ModuleEntry, resolve_backend};

fn journal_path() -> &'static str {
    crate::defs::MOUNT_OVERLAY_JOURNAL
}

fn read_journal() -> Vec<String> {
    fs::read_to_string(journal_path())
        .ok()
        .and_then(|content| serde_json::from_str::<Vec<String>>(&content).ok())
        .unwrap_or_default()
}

fn write_journal(paths: &[String]) -> Result<()> {
    fsutil::ensure_dir(Path::new(crate::defs::MOUNT_RUN_DIR))?;
    let tmp = format!("{}.tmp", journal_path());
    fs::write(&tmp, serde_json::to_string_pretty(paths)?)?;
    fs::rename(&tmp, journal_path())?;
    Ok(())
}

fn push_journal(path: &str) -> Result<()> {
    let mut paths = read_journal();
    if !paths.iter().any(|entry| entry == path) {
        paths.push(path.to_string());
        write_journal(&paths)?;
    }
    Ok(())
}

fn drop_journal(path: &str) {
    let mut paths = read_journal();
    if let Some(index) = paths.iter().position(|entry| entry == path) {
        paths.remove(index);
        let _ = write_journal(&paths);
    }
}

/// `/product` and friends are often symlinks into `/system`; the real mount
/// target is what we have to overlay.
fn partition_mount_point(partition: &str) -> PathBuf {
    let path = Path::new("/").join(partition);
    if path.is_symlink() {
        return fs::canonicalize(&path).unwrap_or(path);
    }
    path
}

fn is_sub_partition(name: &str, partitions: &[String]) -> bool {
    partitions.iter().any(|part| part == name && part != "system")
}

fn add_leaf(ops: &mut BTreeMap<String, Vec<String>>, target: &Path, layer: &Path) {
    let Ok(meta) = fs::symlink_metadata(target) else {
        warn!(
            "overlay: target missing, skipping {} (from {})",
            target.display(),
            layer.display()
        );
        return;
    };
    if !meta.is_dir() {
        warn!(
            "overlay: target is not a directory, skipping {} (from {})",
            target.display(),
            layer.display()
        );
        return;
    }
    ops.entry(target.to_string_lossy().to_string())
        .or_default()
        .push(layer.to_string_lossy().to_string());
}

/// 分区根下的每个子目录各自是一层 overlay，分区根本身不碰。
fn plan_partition_root(ops: &mut BTreeMap<String, Vec<String>>, subroot: &Path, mount: &Path) {
    let Ok(entries) = fs::read_dir(subroot) else {
        return;
    };
    for entry in entries.flatten() {
        if !entry.file_type().map(|t| t.is_dir()).unwrap_or(false) {
            continue;
        }
        add_leaf(ops, &mount.join(entry.file_name()), &entry.path());
    }
}

fn plan_overlays(modules: &[ModuleEntry], config: &Config) -> BTreeMap<String, Vec<String>> {
    let partitions = config.managed_partitions();
    let mut ops: BTreeMap<String, Vec<String>> = BTreeMap::new();

    for module in modules {
        let sysroot = module.path.join("system");
        if let Ok(entries) = fs::read_dir(&sysroot) {
            for entry in entries.flatten() {
                let name = entry.file_name().to_string_lossy().to_string();
                let is_dir = entry.file_type().map(|t| t.is_dir()).unwrap_or(false);
                if !is_dir {
                    // 分区根下的直属文件只能走 magic，解析阶段已经过滤过
                    continue;
                }
                if is_sub_partition(&name, &partitions) && Path::new("/").join(&name).exists() {
                    // 模块同时提供了顶层分区目录时，以顶层为准
                    if module.path.join(&name).is_dir() {
                        continue;
                    }
                    if Path::new("/system").join(&name).is_symlink() {
                        continue;
                    }
                    plan_partition_root(
                        &mut ops,
                        &entry.path(),
                        &partition_mount_point(&name),
                    );
                } else {
                    add_leaf(
                        &mut ops,
                        &Path::new("/system").join(&name),
                        &entry.path(),
                    );
                }
            }
        }

        for partition in &partitions {
            if partition == "system" {
                continue;
            }
            let top = module.path.join(partition);
            if top.is_dir() {
                plan_partition_root(&mut ops, &top, &partition_mount_point(partition));
            }
        }
    }

    ops
}

/// 选出该叶子下的模块层（模块顺序 = 字典序，第一个模块在最上层）。
/// `.replace` / `trusted.overlay.opaque` 会让下层与 stock 一起被丢弃。
fn select_layers(layers: &[String]) -> (Vec<String>, bool) {
    let mut selected = Vec::new();
    let mut include_stock = true;
    for layer in layers {
        let path = Path::new(layer);
        if !path.is_dir() {
            include_stock = false;
            break;
        }
        selected.push(layer.clone());
        if fsutil::is_opaque_directory(path) {
            include_stock = false;
            break;
        }
    }
    (selected, include_stock)
}

/// 重新接回被 overlay 遮住的子挂载：chdir 之后 stock 目录仍可通过 `.` 访问。
fn mount_leaf(target: &str, layers: &[String]) -> Result<()> {
    let target_path = Path::new(target);
    let children = fsutil::child_mount_points(target);
    let (selected, include_stock) = select_layers(layers);
    if selected.is_empty() {
        bail!("no usable overlay layers for {target}");
    }

    std::env::set_current_dir(target_path)
        .with_context(|| format!("chdir {target}"))?;
    let result = (|| -> Result<()> {
        fsutil::mount_overlay(
            &selected,
            include_stock.then_some("."),
            None,
            target_path,
        )?;
        fsutil::register_umount(target);
        push_journal(target)?;
        if include_stock {
            for mount_point in children {
                let relative = &mount_point[target.len()..];
                let stock_child = format!(".{relative}");
                if !Path::new(&stock_child).exists() {
                    continue;
                }
                if let Err(e) =
                    fsutil::rbind_mount(Path::new(&stock_child), Path::new(&mount_point))
                {
                    warn!("overlay: re-attach {mount_point} failed: {e:#}");
                }
            }
        }
        Ok(())
    })();
    let _ = std::env::set_current_dir("/");

    if let Err(e) = &result {
        let _ = fsutil::detach(target_path);
        drop_journal(target);
        return Err(anyhow::anyhow!("{e:#}"));
    }
    debug!("overlay: mounted {target} ({} layers)", selected.len());
    Ok(())
}

pub fn mount_modules(modules: &[ModuleEntry], config: &Config) -> Result<usize> {
    let planned: Vec<ModuleEntry> = modules
        .iter()
        .filter(|module| resolve_backend(module, config) == MODE_OVERLAY)
        .cloned()
        .collect();
    if planned.is_empty() {
        return Ok(0);
    }

    let ops = plan_overlays(&planned, config);
    if ops.is_empty() {
        info!("overlay: no leaf targets planned");
        return Ok(0);
    }

    let mut mounted = 0;
    let mut failures = Vec::new();
    for (target, layers) in &ops {
        match mount_leaf(target, layers) {
            Ok(()) => mounted += 1,
            Err(e) => failures.push(format!("{target}: {e:#}")),
        }
    }
    if failures.is_empty() {
        info!("overlay: {mounted} leaves mounted");
        Ok(mounted)
    } else {
        bail!(
            "overlay: {mounted} leaves mounted, {} failed: {}",
            failures.len(),
            failures.join("; ")
        )
    }
}

pub fn active_mounts() -> Vec<String> {
    let live = fsutil::mounts_with_source(fsutil::MOUNT_SOURCE);
    read_journal()
        .into_iter()
        .filter(|path| live.iter().any(|entry| entry == path))
        .collect()
}

pub fn unmount_all() -> Result<()> {
    let mut paths = read_journal();
    if paths.is_empty() {
        return Ok(());
    }
    // 深度优先：先拆子挂载，再拆父层
    paths.sort();
    paths.reverse();

    let mut remaining = Vec::new();
    for path in &paths {
        if let Err(e) = fsutil::detach(Path::new(path)) {
            let message = format!("{e:#}");
            if message.contains("Invalid argument") || message.contains("No such file") {
                // 已经不在了，属于正常情况
            } else {
                warn!("overlay: detach {path} failed: {message}");
                remaining.push(path.clone());
            }
        }
        fsutil::unregister_umount(path);
    }
    if remaining.is_empty() {
        let _ = fs::remove_file(journal_path());
        Ok(())
    } else {
        write_journal(&remaining)?;
        bail!("{} overlay mounts could not be detached", remaining.len())
    }
}
