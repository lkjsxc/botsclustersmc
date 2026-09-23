//! Test-only environment preparation using the production installer/config writer.
#[allow(dead_code)]#[path="../launcher/json.rs"] mod json;
#[allow(dead_code)]#[path="../launcher/install.rs"] mod install;
#[allow(dead_code)]#[path="../launcher/config.rs"] mod config;
use std::{env,path::PathBuf,process::Command};
fn main()->Result<(),Box<dyn std::error::Error>>{
    let root=PathBuf::from(env::var("BCMC_ROOT")?);
    if !root.join(".bcmc-diagnostic-only").is_file()||root.join("state/training.bcmc").exists(){return Err("not an isolated diagnostic directory".into());}
    let cfg=config::Config::load()?;cfg.write(&root)?;
    let java=install::java(&root)?;let jar=install::folia(&root)?;
    install::command(Command::new("bash").arg(root.join("scripts/build-bridge.sh")).env("BCMC_ROOT",&root).env("BCMC_JAVA",&java).env("BCMC_FOLIA_JAR",&jar).env("BCMC_CURRICULUM","true"))?;
    println!("DIAGNOSTIC_JAVA={}",java.display());println!("DIAGNOSTIC_JAR={}",jar.display());Ok(())
}
