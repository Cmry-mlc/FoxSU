//! `ksud mount ...` — built-in module mount controller.

use anyhow::Result;
use clap::Subcommand;
use log::info;

use crate::android::mount::{self as mount_ctl, Config};

#[derive(clap::Args, Debug)]
pub struct MountArgs {
    #[command(subcommand)]
    pub command: MountCommand,
}

#[derive(Subcommand, Debug)]
pub enum MountCommand {
    /// 查看挂载状态
    Status {
        /// 输出 JSON
        #[arg(long)]
        json: bool,
    },
    /// 立即挂载（仅开机阶段有效，-f 可强制）
    Apply {
        /// 跳过开机阶段 / metamodule / 防砖检查
        #[arg(long, short)]
        force: bool,
    },
    /// 卸载所有由本控制器建立的挂载
    Unmount,
    /// 开启 / 关闭内置挂载控制器
    Enable { on: bool },
    /// 设置全局后端：auto / overlay / magic / none
    Backend { backend: String },
    /// 逐模块设置
    Module {
        #[command(subcommand)]
        command: ModuleCommand,
    },
    /// 列出已挂载的路径
    List {
        #[arg(long)]
        json: bool,
    },
    /// 清除防砖标记与开机失败计数
    Reset,
}

#[derive(Subcommand, Debug)]
pub enum ModuleCommand {
    /// 列出模块及其生效后端
    List {
        #[arg(long)]
        json: bool,
    },
    /// 设置单个模块的模式：auto / overlay / magic / none
    Set { id: String, mode: String },
}

pub fn run(args: MountArgs) -> Result<()> {
    match args.command {
        MountCommand::Status { json } => {
            let status = mount_ctl::status();
            if json {
                println!("{}", serde_json::to_string_pretty(&status)?);
            } else {
                println!("enabled={}", u8::from(status.enabled));
                println!("mounted={}", u8::from(status.mounted));
                println!("backend={}", status.backend);
                println!("applied={}", status.applied);
                println!("overlay_supported={}", u8::from(status.overlay_supported));
                println!("magic_supported={}", u8::from(status.magic_supported));
                println!("modules={}", status.modules);
                println!("mounts={}", status.mounts);
                println!("metamodule={}", u8::from(status.metamodule));
                println!("disabled={}", u8::from(status.disabled));
                println!("detail={}", status.detail);
            }
            Ok(())
        }
        MountCommand::Apply { force } => {
            let report = mount_ctl::apply(force)?;
            info!(
                "mount apply: ok={} backend={} modules={} mounts={}",
                report.ok, report.backend, report.modules, report.mounts
            );
            println!("ok={}", u8::from(report.ok));
            println!("backend={}", report.backend);
            println!("modules={}", report.modules);
            println!("mounts={}", report.mounts);
            println!("detail={}", report.detail);
            Ok(())
        }
        MountCommand::Unmount => {
            mount_ctl::unmount_all(&Config::load())?;
            println!("ok=1");
            Ok(())
        }
        MountCommand::Enable { on } => {
            mount_ctl::set_enabled(on)?;
            println!("enabled={}", u8::from(on));
            Ok(())
        }
        MountCommand::Backend { backend } => {
            mount_ctl::set_backend(&backend)?;
            println!("backend={backend}");
            Ok(())
        }
        MountCommand::Module { command } => match command {
            ModuleCommand::List { json } => {
                let config = Config::load();
                let modules = mount_ctl::module_statuses(&config);
                if json {
                    println!("{}", serde_json::to_string_pretty(&modules)?);
                } else {
                    for module in modules {
                        println!(
                            "{}\t{}\t{}\t{}",
                            module.id, module.mode, module.backend, module.name
                        );
                    }
                }
                Ok(())
            }
            ModuleCommand::Set { id, mode } => {
                mount_ctl::set_module_mode(&id, &mode)?;
                println!("{id}={mode}");
                Ok(())
            }
        },
        MountCommand::List { json } => {
            let paths = mount_ctl::mounted_paths();
            if json {
                println!("{}", serde_json::to_string_pretty(&paths)?);
            } else {
                for path in paths {
                    println!("{path}");
                }
            }
            Ok(())
        }
        MountCommand::Reset => {
            mount_ctl::reset_recovery()?;
            println!("ok=1");
            Ok(())
        }
    }
}
