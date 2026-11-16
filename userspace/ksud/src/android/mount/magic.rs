//! Magic Mount backend.
//!
//! Lazy-tmpfs variant ported from ZySU's `magic_mount.cpp`: a directory only
//! gets a tmpfs skeleton when one of its direct children has to be added,
//! removed or change type. Everything else is a plain per-file bind mount, so
//! the mount count stays proportional to what the modules actually touch.
//!
//! Unlike OverlayFS this backend can replace individual files at a partition
//! root (`/system/build.prop`), which is why modules that place files there are
//! routed here.

use std::collections::BTreeMap;
use std::ffi::OsStr;
use std::fs;
use std::os::unix::fs::symlink;
use std::path::{Path, PathBuf};

use anyhow::{Context, Result, anyhow, bail};
use log::{debug, info, warn};

use super::fsutil;
use super::{Config, ModuleEntry};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum NodeType {
    Regular,
    Directory,
    Symlink,
    Whiteout,
}

#[derive(Debug, Clone)]
struct Node {
    kind: NodeType,
    children: BTreeMap<String, Node>,
    /// 该节点对应的模块内路径；分区根等「透传」节点为空。
    module_path: PathBuf,
    /// `.replace` / `trusted.overlay.opaque`：丢弃下层与 stock 内容
    replace: bool,
}

impl Node {
    fn empty_dir() -> Self {
        Self {
            kind: NodeType::Directory,
            children: BTreeMap::new(),
            module_path: PathBuf::new(),
            replace: false,
        }
    }
}

fn classify(path: &Path) -> Option<NodeType> {
    if fsutil::is_whiteout(path) {
        return Some(NodeType::Whiteout);
    }
    let meta = fs::symlink_metadata(path).ok()?;
    let file_type = meta.file_type();
    if file_type.is_dir() {
        Some(NodeType::Directory)
    } else if file_type.is_symlink() {
        Some(NodeType::Symlink)
    } else if file_type.is_file() {
        Some(NodeType::Regular)
    } else {
        // 设备节点 / socket / fifo：不参与挂载
        None
    }
}

fn merge_node(dst: &mut Node, src: Node) {
    if dst.kind != NodeType::Directory || src.kind != NodeType::Directory {
        // 先出现的模块优先，与 OverlayFS 的 lowerdir 顺序保持一致
        return;
    }
    dst.replace |= src.replace;
    for (name, child) in src.children {
        match dst.children.get_mut(&name) {
            Some(existing) => merge_node(existing, child),
            None => {
                dst.children.insert(name, child);
            }
        }
    }
}

fn collect_into(
    parent: &mut Node,
    dir: &Path,
    module_root: Option<&Path>,
    partitions: &[String],
) -> Result<bool> {
    let Ok(entries) = fs::read_dir(dir) else {
        return Ok(false);
    };
    let mut has = false;
    for entry in entries.flatten() {
        let name = entry.file_name().to_string_lossy().to_string();
        if name == ".replace" {
            continue;
        }
        let mut path = entry.path();
        let mut kind = match classify(&path) {
            Some(kind) => kind,
            None => continue,
        };
        // 模块里的 system/vendor 常是符号链接，指向模块内真实目录
        if kind == NodeType::Symlink && partitions.iter().any(|part| *part == name)
            && let (Some(root), Ok(target)) = (module_root, fs::canonicalize(&path))
            && target.starts_with(root)
            && target.is_dir()
        {
            path = target;
            kind = NodeType::Directory;
        }
        let replace = kind == NodeType::Directory && fsutil::is_opaque_directory(&path);

        match parent.children.get_mut(&name) {
            Some(existing) => {
                if existing.kind == NodeType::Directory && !existing.replace && kind == NodeType::Directory
                {
                    collect_into(existing, &path, module_root, partitions)?;
                }
            }
            None => {
                let mut node = Node {
                    kind,
                    children: BTreeMap::new(),
                    module_path: path.clone(),
                    replace,
                };
                if kind == NodeType::Directory {
                    collect_into(&mut node, &path, module_root, partitions)?;
                }
                parent.children.insert(name, node);
            }
        }
        has = true;
    }
    Ok(has)
}

/// 合并所有 magic 模块的文件树。`system/<分区>` 会按设备实际布局搬到根下。
fn collect_module_files(modules: &[ModuleEntry], partitions: &[String]) -> Result<Option<Node>> {
    let mut root = Node::empty_dir();
    let mut system = Node::empty_dir();
    let mut has = false;

    for module in modules {
        let sysroot = module.path.join("system");
        if sysroot.is_dir() {
            has |= collect_into(&mut system, &sysroot, Some(&module.path), partitions)?;
        }
        for partition in partitions.iter().filter(|part| *part != "system") {
            let top = module.path.join(partition);
            if !top.is_dir() {
                continue;
            }
            let node = root
                .children
                .entry(partition.clone())
                .or_insert_with(Node::empty_dir);
            collect_into(node, &top, Some(&module.path), partitions)?;
            has = true;
        }
    }

    // system/vendor、system/product ... 在设备上以独立分区挂载时搬到根下
    for partition in partitions.iter().filter(|part| *part != "system") {
        if !Path::new("/").join(partition).exists() {
            continue;
        }
        if let Some(node) = system.children.remove(partition) {
            match root.children.get_mut(partition) {
                Some(existing) => merge_node(existing, node),
                None => {
                    root.children.insert(partition.clone(), node);
                }
            }
        }
    }

    if system.children.is_empty() {
        if !has {
            return Ok(None);
        }
    } else {
        has = true;
    }
    root.children.insert("system".to_string(), system);

    Ok(has.then_some(root))
}

// ---------------------------------------------------------------- journal

fn journal_path() -> &'static str {
    crate::defs::MOUNT_MAGIC_JOURNAL
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

fn live_mounts() -> Vec<String> {
    let mut paths: Vec<String> = fsutil::read_mountinfo()
        .into_iter()
        .map(|m| m.mount_point)
        .collect();
    paths.sort();
    paths.dedup();
    paths
}

// ---------------------------------------------------------------- walk

#[derive(Debug, Default)]
struct Walk {
    committed: Vec<String>,
    files: usize,
    tmpfs_dirs: usize,
    symlinks: usize,
}

fn ensure_skeleton(real: &Path, work: &Path, module_path: &Path) -> Result<()> {
    fs::create_dir_all(work).with_context(|| format!("mkdir skeleton {}", work.display()))?;
    let src = if real.is_dir() { real } else { module_path };
    if !src.exists() {
        bail!("no attribute source for skeleton {}", work.display());
    }
    fsutil::clone_attr(src, work);
    Ok(())
}

fn mirror_entry(real_dir: &Path, work_dir: &Path, name: &OsStr) -> Result<()> {
    let src = real_dir.join(name);
    let dst = work_dir.join(name);
    let meta = fs::symlink_metadata(&src)
        .with_context(|| format!("lstat {}", src.display()))?;
    if meta.is_dir() {
        fs::create_dir_all(&dst).with_context(|| format!("mkdir {}", dst.display()))?;
        fsutil::clone_attr(&src, &dst);
        for entry in fs::read_dir(&src)?.flatten() {
            mirror_entry(&src, &dst, &entry.file_name())?;
        }
    } else if meta.file_type().is_symlink() {
        let target = fs::read_link(&src)?;
        symlink(target, &dst).with_context(|| format!("symlink {}", dst.display()))?;
    } else if meta.is_file() {
        fs::File::create(&dst).with_context(|| format!("create {}", dst.display()))?;
        fsutil::bind_mount(&src, &dst)?;
    }
    Ok(())
}

fn mount_node(
    node: &mut Node,
    real: &Path,
    work: &Path,
    has_tmpfs: bool,
    walk: &mut Walk,
) -> Result<()> {
    match node.kind {
        NodeType::Whiteout => Ok(()),
        NodeType::Symlink => {
            if node.module_path.as_os_str().is_empty() {
                bail!("symlink node without module backing: {}", real.display());
            }
            let target = fs::read_link(&node.module_path)
                .with_context(|| format!("readlink {}", node.module_path.display()))?;
            symlink(target, work).with_context(|| format!("symlink {}", work.display()))?;
            walk.symlinks += 1;
            Ok(())
        }
        NodeType::Regular => {
            if node.module_path.as_os_str().is_empty() {
                bail!("regular node without module backing: {}", real.display());
            }
            let target = if has_tmpfs { work } else { real };
            if has_tmpfs {
                fs::File::create(target).with_context(|| format!("create {}", target.display()))?;
            } else if !target.is_file() {
                bail!(
                    "direct bind requires an existing regular file: {}",
                    target.display()
                );
            }
            fsutil::bind_mount(&node.module_path, target)?;
            if !has_tmpfs {
                walk.committed.push(target.to_string_lossy().to_string());
            }
            fsutil::remount_bind_readonly(target)?;
            if !has_tmpfs {
                fsutil::make_private(target)?;
            }
            walk.files += 1;
            Ok(())
        }
        NodeType::Directory => mount_directory(node, real, work, has_tmpfs, walk),
    }
}

fn mount_directory(
    node: &mut Node,
    real: &Path,
    work: &Path,
    has_tmpfs: bool,
    walk: &mut Walk,
) -> Result<()> {
    let real_is_dir = fs::symlink_metadata(real)
        .map(|m| m.is_dir())
        .unwrap_or(false);
    let module_backed = !node.module_path.as_os_str().is_empty();
    let replace = node.replace;
    let mut children = std::mem::take(&mut node.children);

    let mut needs_skeleton = !has_tmpfs && replace && module_backed;
    if !has_tmpfs && !needs_skeleton {
        for (name, child) in children.iter() {
            let real_child = real.join(name);
            let need = match child.kind {
                NodeType::Symlink => true,
                NodeType::Regular => !real_child.is_file(),
                NodeType::Whiteout => fs::symlink_metadata(&real_child).is_ok(),
                NodeType::Directory => !fs::symlink_metadata(&real_child)
                    .map(|m| m.is_dir())
                    .unwrap_or(false),
            };
            if need {
                if !module_backed {
                    bail!(
                        "cannot add, delete or change the type of a partition-root entry: {}",
                        real_child.display()
                    );
                }
                needs_skeleton = true;
                break;
            }
        }
    }

    let now_tmpfs = needs_skeleton || has_tmpfs;
    if now_tmpfs {
        ensure_skeleton(real, work, &node.module_path)?;
    }
    if needs_skeleton {
        // 先自绑定，skeleton 才能被 MS_MOVE 搬到真实路径
        fsutil::bind_mount(work, work)
            .with_context(|| format!("self bind {}", work.display()))?;
    }

    if now_tmpfs && real_is_dir && !replace {
        for entry in fs::read_dir(real)?.flatten() {
            let name = entry.file_name();
            let key = name.to_string_lossy().to_string();
            if let Some(mut child) = children.remove(&key) {
                mount_node(&mut child, &real.join(&name), &work.join(&name), true, walk)?;
            } else {
                mirror_entry(real, work, &name)?;
            }
        }
    }

    for (name, mut child) in children {
        mount_node(&mut child, &real.join(&name), &work.join(&name), now_tmpfs, walk)?;
    }

    if needs_skeleton {
        fsutil::remount_bind_readonly(work)?;
        fsutil::move_mount_path(work, real)?;
        walk.committed.push(real.to_string_lossy().to_string());
        fsutil::make_private(real)?;
        walk.tmpfs_dirs += 1;
        debug!("magic: skeleton at {}", real.display());
    }
    Ok(())
}

// ---------------------------------------------------------------- apply

pub fn mount_modules(modules: &[ModuleEntry], config: &Config) -> Result<usize> {
    let partitions = config.managed_partitions();
    let Some(mut root) = collect_module_files(modules, &partitions)? else {
        info!("magic: no module files to mount");
        return Ok(0);
    };

    let _ = unmount_all(config);

    let work = PathBuf::from(&config.work_dir);
    fsutil::prepare_empty_mountpoint(&work)?;
    fsutil::mount_tmpfs(&config.mount_source, &work)?;
    fsutil::make_private(&work)?;

    let mut walk = Walk::default();
    walk.committed.push(work.to_string_lossy().to_string());
    write_journal(&walk.committed)?;

    let staged = work.to_string_lossy().to_string();
    let result = mount_node(&mut root, Path::new("/"), &work, false, &mut walk);

    match result {
        Ok(()) => {
            // 搬走的 skeleton 各自持有 tmpfs 引用，暂存挂载可以撤销了
            if let Err(e) = fsutil::detach(&work) {
                warn!("magic: detach staging tmpfs failed: {e:#}");
            } else {
                walk.committed.retain(|path| *path != staged);
            }
        }
        Err(e) => {
            warn!("magic: mount traversal failed: {e:#}");
            let _ = rollback(&walk.committed);
            let _ = fs::remove_file(journal_path());
            return Err(e);
        }
    }

    write_journal(&walk.committed)?;
    for path in &walk.committed {
        fsutil::register_umount(path);
    }
    info!(
        "magic: files={} skeletons={} symlinks={}",
        walk.files, walk.tmpfs_dirs, walk.symlinks
    );
    Ok(walk.committed.len())
}

fn rollback(paths: &[String]) -> Result<()> {
    let mut ordered = paths.to_vec();
    ordered.sort();
    ordered.reverse();
    for path in &ordered {
        if let Err(e) = fsutil::detach(Path::new(path)) {
            debug!("magic: rollback detach {path}: {e:#}");
        }
        fsutil::unregister_umount(path);
    }
    let _ = fs::remove_file(journal_path());
    Ok(())
}

pub fn active_mounts() -> Vec<String> {
    let live = live_mounts();
    read_journal()
        .into_iter()
        .filter(|path| live.iter().any(|entry| entry == path))
        .collect()
}

pub fn unmount_all(_config: &Config) -> Result<()> {
    let paths = read_journal();
    if paths.is_empty() {
        return Ok(());
    }
    let mut ordered = paths;
    ordered.sort();
    ordered.reverse();

    let mut remaining = Vec::new();
    for path in &ordered {
        match fsutil::detach(Path::new(path)) {
            Ok(()) => {}
            Err(e) => {
                let message = format!("{e:#}");
                if message.contains("Invalid argument") || message.contains("No such file") {
                    // 已不存在
                } else {
                    warn!("magic: detach {path} failed: {message}");
                    remaining.push(path.clone());
                }
            }
        }
        fsutil::unregister_umount(path);
    }
    if remaining.is_empty() {
        let _ = fs::remove_file(journal_path());
        Ok(())
    } else {
        write_journal(&remaining)?;
        Err(anyhow!("{} magic mounts could not be detached", remaining.len()))
    }
}
