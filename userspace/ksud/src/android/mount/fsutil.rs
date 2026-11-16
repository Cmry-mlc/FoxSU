//! Low-level mount helpers shared by the OverlayFS and Magic Mount backends.
//!
//! Ported from ZySU's `sumhp/src/mount/mount_fs.cpp`, trimmed to what the two
//! backends here need: bind/overlay/tmpfs mounting, SELinux context cloning,
//! mountinfo parsing and the kernel umount list registration.

use std::ffi::CString;
use std::fs;
use std::os::unix::fs::{FileTypeExt, MetadataExt, PermissionsExt};
use std::path::Path;

use anyhow::{Context, Result, anyhow, bail};
use log::{debug, warn};
use rustix::fd::AsFd;
use rustix::fs::CWD;
use rustix::mount::{
    FsMountFlags, FsOpenFlags, MountAttrFlags, MoveMountFlags, OpenTreeFlags, fsconfig_create,
    fsconfig_set_string, fsmount, fsopen, move_mount, open_tree,
};
use rustix::mount::mount_move;

use crate::android::ksucalls;

/// Mount source used for every overlay/tmpfs we create. Also the marker that
/// tells our own mounts apart from the stock filesystems in `/proc/self/mountinfo`.
pub const MOUNT_SOURCE: &str = "KSU";

fn cpath(path: &Path) -> Result<CString> {
    CString::new(path.as_os_str().as_encoded_bytes())
        .with_context(|| format!("path contains a NUL byte: {}", path.display()))
}

pub fn errno_message() -> String {
    std::io::Error::last_os_error().to_string()
}

/// `mkdir -p`
pub fn ensure_dir(path: &Path) -> Result<()> {
    fs::create_dir_all(path).with_context(|| format!("create dir {}", path.display()))?;
    Ok(())
}

/// A mount point must exist and be an empty directory: attaching over a
/// non-empty dir would shadow real content once the tempfs is moved away.
pub fn prepare_empty_mountpoint(path: &Path) -> Result<()> {
    if !path.is_absolute() || path.parent().is_none() {
        bail!("refusing unsafe mountpoint {}", path.display());
    }
    ensure_dir(path)?;
    let mut entries = fs::read_dir(path).with_context(|| format!("read {}", path.display()))?;
    if entries.next().is_some() {
        bail!("mountpoint is not empty: {}", path.display());
    }
    Ok(())
}

// ---------------------------------------------------------------- mounts

fn mount_raw(
    source: Option<&Path>,
    target: &Path,
    fstype: Option<&str>,
    flags: libc::c_ulong,
    data: Option<&str>,
) -> Result<()> {
    let source_c = source.map(cpath).transpose()?;
    let target_c = cpath(target)?;
    let fstype_c = fstype.map(CString::new).transpose()?;
    let data_c = data.map(CString::new).transpose()?;
    let rc = unsafe {
        libc::mount(
            source_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
            target_c.as_ptr(),
            fstype_c.as_ref().map_or(std::ptr::null(), |c| c.as_ptr()),
            flags,
            data_c
                .as_ref()
                .map_or(std::ptr::null(), |c| c.as_ptr().cast()),
        )
    };
    if rc != 0 {
        bail!(
            "mount({} -> {}) failed: {}",
            source.map(|p| p.display().to_string()).unwrap_or_default(),
            target.display(),
            errno_message()
        );
    }
    Ok(())
}

pub fn bind_mount(src: &Path, dst: &Path) -> Result<()> {
    mount_raw(Some(src), dst, None, libc::MS_BIND, None)
}

/// Clone a whole mount tree, including nested sub-mounts, and attach it at `dst`.
/// Preferred over `MS_BIND | MS_REC` because `open_tree` keeps the tree intact
/// even when the source is shadowed by an overlay.
pub fn rbind_mount(src: &Path, dst: &Path) -> Result<()> {
    let tree = open_tree(
        CWD,
        src,
        OpenTreeFlags::OPEN_TREE_CLONE | OpenTreeFlags::OPEN_TREE_CLOEXEC | OpenTreeFlags::AT_RECURSIVE,
    );
    if let Ok(tree) = tree {
        let result = move_mount(
            tree.as_fd(),
            "",
            CWD,
            dst,
            MoveMountFlags::MOVE_MOUNT_F_EMPTY_PATH,
        );
        if result.is_ok() {
            return Ok(());
        }
        debug!(
            "open_tree clone bind {} -> {} failed: {}, falling back to mount(2)",
            src.display(),
            dst.display(),
            errno_message()
        );
    }
    mount_raw(
        Some(src),
        dst,
        None,
        libc::MS_BIND | libc::MS_REC,
        None,
    )
}

pub fn mount_tmpfs(source: &str, target: &Path) -> Result<()> {
    mount_raw(
        Some(Path::new(source)),
        target,
        Some("tmpfs"),
        libc::MS_NOSUID | libc::MS_NODEV,
        None,
    )
}

pub fn make_private(target: &Path) -> Result<()> {
    mount_raw(
        None,
        target,
        None,
        libc::MS_PRIVATE | libc::MS_REC,
        None,
    )
}

pub fn remount_bind_readonly(target: &Path) -> Result<()> {
    mount_raw(
        None,
        target,
        None,
        libc::MS_REMOUNT | libc::MS_BIND | libc::MS_RDONLY,
        None,
    )
}

pub fn move_mount_path(from: &Path, to: &Path) -> Result<()> {
    mount_move(from, to).map_err(|e| anyhow!("move {} -> {}: {e}", from.display(), to.display()))
}

pub fn detach(target: &Path) -> Result<()> {
    let target_c = cpath(target)?;
    let rc = unsafe { libc::umount2(target_c.as_ptr(), libc::MNT_DETACH) };
    if rc != 0 {
        bail!("umount {} failed: {}", target.display(), errno_message());
    }
    Ok(())
}

/// Mount an OverlayFS view of `layers` (topmost first) plus `lowest` at `dest`.
/// Uses the new mount API first: the classic `mount(2)` path caps `lowerdir` at
/// one page, which a handful of modules already exceed.
pub fn mount_overlay(
    layers: &[String],
    lowest: Option<&str>,
    upper: Option<(&Path, &Path)>,
    dest: &Path,
) -> Result<()> {
    let mut dirs: Vec<String> = layers.to_vec();
    if let Some(lowest) = lowest {
        dirs.push(lowest.to_string());
    }
    if dirs.is_empty() {
        bail!("overlay {} has no lowerdir", dest.display());
    }
    if dirs.len() == 1 {
        // A read-only OverlayFS needs at least two lower layers.
        let filler = crate::defs::MOUNT_RUN_DIR.to_string() + "overlay_empty";
        let filler_path = Path::new(&filler);
        ensure_dir(filler_path)?;
        dirs.push(filler);
    }
    let lowerdir = dirs.join(":");
    let writable = upper.filter(|(u, w)| u.exists() && w.exists());

    let fs = fsopen("overlay", FsOpenFlags::FSOPEN_CLOEXEC);
    if let Ok(fs) = fs {
        let configured = fsconfig_set_string(fs.as_fd(), "lowerdir", lowerdir.as_str())
            .and_then(|_| {
                if let Some((upper, work)) = writable {
                    fsconfig_set_string(
                        fs.as_fd(),
                        "upperdir",
                        upper.to_string_lossy().as_ref(),
                    )?;
                    fsconfig_set_string(fs.as_fd(), "workdir", work.to_string_lossy().as_ref())?;
                }
                fsconfig_set_string(fs.as_fd(), "source", MOUNT_SOURCE)?;
                fsconfig_create(fs.as_fd())
            });
        if configured.is_ok()
            && let Ok(mfd) = fsmount(
                fs.as_fd(),
                FsMountFlags::FSMOUNT_CLOEXEC,
                MountAttrFlags::empty(),
            )
        {
            let moved = move_mount(
                mfd.as_fd(),
                "",
                CWD,
                dest,
                MoveMountFlags::MOVE_MOUNT_F_EMPTY_PATH,
            );
            if moved.is_ok() {
                return Ok(());
            }
            debug!(
                "fsmount attach {} failed: {}, falling back to mount(2)",
                dest.display(),
                errno_message()
            );
        }
    }

    let mut data = format!("lowerdir={lowerdir}");
    if let Some((upper, work)) = writable {
        data.push_str(&format!(
            ",upperdir={},workdir={}",
            upper.display(),
            work.display()
        ));
    }
    mount_raw(
        Some(Path::new(MOUNT_SOURCE)),
        dest,
        Some("overlay"),
        0,
        Some(&data),
    )
}

// ---------------------------------------------------------------- attributes

const SELINUX_XATTR: &str = "security.selinux";

pub fn get_selinux_context(path: &Path) -> Option<String> {
    let path_c = cpath(path).ok()?;
    let name = CString::new(SELINUX_XATTR).ok()?;
    let mut buf = [0u8; 256];
    let len = unsafe {
        libc::lgetxattr(
            path_c.as_ptr(),
            name.as_ptr(),
            buf.as_mut_ptr().cast(),
            buf.len() - 1,
        )
    };
    if len <= 0 {
        return None;
    }
    let text = String::from_utf8_lossy(&buf[..len as usize]).to_string();
    Some(text.trim_end_matches('\0').to_string())
}

pub fn set_selinux_context(path: &Path, context: &str) -> bool {
    let Ok(path_c) = cpath(path) else {
        return false;
    };
    let Ok(name) = CString::new(SELINUX_XATTR) else {
        return false;
    };
    let Ok(value) = CString::new(context) else {
        return false;
    };
    unsafe {
        libc::lsetxattr(
            path_c.as_ptr(),
            name.as_ptr(),
            value.as_ptr().cast(),
            context.len() + 1,
            0,
        ) == 0
    }
}

/// Copy mode, ownership and SELinux label so a tmpfs skeleton looks like the
/// directory it replaces.
pub fn clone_attr(src: &Path, dst: &Path) {
    let Ok(meta) = fs::symlink_metadata(src) else {
        return;
    };
    if !meta.file_type().is_symlink() {
        let _ = fs::set_permissions(dst, fs::Permissions::from_mode(meta.mode() & 0o7777));
    }
    let dst_c = match cpath(dst) {
        Ok(value) => value,
        Err(_) => return,
    };
    unsafe {
        libc::chown(dst_c.as_ptr(), meta.uid(), meta.gid());
    }
    if let Some(context) = get_selinux_context(src) {
        set_selinux_context(dst, &context);
    }
}


/// `.replace` (Magisk) or `trusted.overlay.opaque` (OverlayFS) marks a
/// directory whose lower layers must be dropped.
pub fn is_opaque_directory(path: &Path) -> bool {
    if path.join(".replace").exists() {
        return true;
    }
    let Ok(path_c) = cpath(path) else {
        return false;
    };
    let Ok(name) = CString::new("trusted.overlay.opaque") else {
        return false;
    };
    let mut buf = [0u8; 8];
    let len = unsafe {
        libc::lgetxattr(
            path_c.as_ptr(),
            name.as_ptr(),
            buf.as_mut_ptr().cast(),
            buf.len(),
        )
    };
    len > 0 && matches!(buf[0], b'y' | b'Y')
}

/// A whiteout is a character device with rdev 0:0.
pub fn is_whiteout(path: &Path) -> bool {
    let Ok(meta) = fs::symlink_metadata(path) else {
        return false;
    };
    meta.file_type().is_char_device() && meta.rdev() == 0
}

// ---------------------------------------------------------------- mountinfo

#[derive(Debug, Clone)]
pub struct MountInfo {
    pub mount_point: String,
    pub source: String,
    #[allow(dead_code)]
    pub fstype: String,
}

fn decode_octal(input: &str) -> String {
    let bytes = input.as_bytes();
    let mut out = Vec::with_capacity(bytes.len());
    let mut index = 0;
    while index < bytes.len() {
        if bytes[index] == b'\\' && index + 3 < bytes.len() {
            let digits = &input[index + 1..index + 4];
            if let Ok(value) = u8::from_str_radix(digits, 8) {
                out.push(value);
                index += 4;
                continue;
            }
        }
        out.push(bytes[index]);
        index += 1;
    }
    String::from_utf8_lossy(&out).to_string()
}

pub fn read_mountinfo() -> Vec<MountInfo> {
    let Ok(content) = fs::read_to_string("/proc/self/mountinfo") else {
        return Vec::new();
    };
    let mut out = Vec::new();
    for line in content.lines() {
        let Some((pre, post)) = line.split_once(" - ") else {
            continue;
        };
        let fields: Vec<&str> = pre.split_whitespace().collect();
        if fields.len() < 5 {
            continue;
        }
        let mut post_fields = post.split_whitespace();
        let fstype = post_fields.next().unwrap_or_default();
        let source = post_fields.next().unwrap_or_default();
        out.push(MountInfo {
            mount_point: decode_octal(fields[4]),
            source: decode_octal(source),
            fstype: fstype.to_string(),
        });
    }
    out
}

/// Mount points strictly below `root`, parents before children.
pub fn child_mount_points(root: &str) -> Vec<String> {
    let prefix = format!("{}/", root.trim_end_matches('/'));
    let mut paths: Vec<String> = read_mountinfo()
        .into_iter()
        .map(|m| m.mount_point)
        .filter(|p| p.starts_with(&prefix))
        .collect();
    paths.sort();
    paths.dedup();
    paths
}

pub fn mounts_with_source(source: &str) -> Vec<String> {
    let mut paths: Vec<String> = read_mountinfo()
        .into_iter()
        .filter(|m| m.source == source)
        .map(|m| m.mount_point)
        .collect();
    paths.sort();
    paths.dedup();
    paths
}

pub fn overlay_supported() -> bool {
    let Ok(content) = fs::read_to_string("/proc/filesystems") else {
        return false;
    };
    content
        .lines()
        .any(|line| line.split_whitespace().any(|part| part == "overlay"))
}

// ---------------------------------------------------------------- umount list

/// Register a mount point with the kernel so app profiles that unmount modules
/// hide it. Missing kernel support only logs: the mount itself stays valid.
pub fn register_umount(path: &str) -> bool {
    match ksucalls::umount_list_add(path, 0) {
        Ok(()) => true,
        Err(e) => {
            warn!("register umount {path} failed: {e}");
            false
        }
    }
}

pub fn unregister_umount(path: &str) -> bool {
    match ksucalls::umount_list_del(path) {
        Ok(()) => true,
        Err(e) => {
            debug!("unregister umount {path} failed: {e}");
            false
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn decodes_mountinfo_escapes() {
        assert_eq!(decode_octal("/a\\040b"), "/a b");
        assert_eq!(decode_octal("/plain"), "/plain");
    }
}
