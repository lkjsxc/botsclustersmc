use std::{env,fs,io::Write,path::{Path,PathBuf},process::Command};
use super::json::{self,Json};
pub const MC:&str="1.21.11";
pub type Result<T>=std::result::Result<T,String>;
pub fn command(c:&mut Command)->Result<()>{let label=format!("{c:?}");let status=c.status().map_err(|e|format!("{label}: {e}"))?;if status.success(){Ok(())}else{Err(format!("{label} exited with {status}"))}}
pub fn atomic(path:&Path,bytes:&[u8])->Result<()>{let tmp=path.with_extension("tmp");let mut f=fs::File::create(&tmp).map_err(|e|e.to_string())?;f.write_all(bytes).map_err(|e|e.to_string())?;f.sync_all().map_err(|e|e.to_string())?;fs::rename(tmp,path).map_err(|e|e.to_string())}
fn curl()->Command{let mut c=Command::new("curl");c.args(["--fail","--location","--silent","--show-error","--retry","3","--connect-timeout","20","--max-time","900","--proto","=https","--proto-redir","=https","--tlsv1.2","--user-agent"]);c.arg(env::var("BCMC_USER_AGENT").unwrap_or("botsclustersmc/0.3 (https://github.com/lkjsxc/botsclustersmc)".into()));c}
pub fn metadata(url:&str)->Result<Json>{let out=curl().arg(url).output().map_err(|e|e.to_string())?;if !out.status.success(){return Err(format!("metadata download failed for {url}: {}",String::from_utf8_lossy(&out.stderr)));}json::parse(std::str::from_utf8(&out.stdout).map_err(|e|e.to_string())?)}
fn valid_hash(hash:&str)->bool{hash.len()==64&&hash.bytes().all(|b|b.is_ascii_hexdigit())}
pub fn verify(path:&Path,hash:&str)->Result<()>{if !valid_hash(hash){return Err("invalid SHA-256 metadata".into());}let o=Command::new("sha256sum").arg(path).output().map_err(|e|e.to_string())?;let s=String::from_utf8_lossy(&o.stdout);if !o.status.success()||s.split_whitespace().next().map(|x|x.eq_ignore_ascii_case(hash))!=Some(true){return Err(format!("SHA-256 mismatch: {}",path.display()));}Ok(())}
pub fn download(url:&str,hash:&str,dest:&Path)->Result<()>{if !url.starts_with("https://")||!valid_hash(hash){return Err("only HTTPS downloads with SHA-256 are accepted".into());}if dest.exists(){return verify(dest,hash);}let tmp=dest.with_extension("download");command(curl().arg("--output").arg(&tmp).arg(url))?;verify(&tmp,hash)?;fs::rename(tmp,dest).map_err(|e|e.to_string())}
#[derive(Clone,Debug)]pub struct Build{pub id:u64,pub url:String,pub sha:String,pub channel:String}
pub fn select_folia(j:&Json,requested:Option<u64>,experimental:bool)->Result<Build>{
    let a=j.array()?;let mut found=vec![];
    for b in a {let id=b.get("id")?.integer()?;let channel=b.get("channel")?.text()?;
        if requested.is_some_and(|x|x!=id)||(!experimental&&channel!="STABLE"){continue;}
        let d=b.get("downloads")?.get("server:default")?;
        found.push(Build{id,url:d.get("url")?.text()?.into(),sha:d.get("checksums")?.get("sha256")?.text()?.into(),channel:channel.into()});
    }
    found.into_iter().max_by_key(|x|x.id).ok_or_else(||format!("No matching {} Folia {MC} build. No version fallback. Read docs/TROUBLESHOOTING.md.",if experimental{"official"}else{"STABLE"}))
}
pub fn folia(root:&Path)->Result<PathBuf>{
    let cache=root.join("runtime");fs::create_dir_all(&cache).map_err(|e|e.to_string())?;
    let lock=cache.join("folia.lock");let jar=cache.join("folia.jar");
    if lock.exists(){let s=fs::read_to_string(&lock).map_err(|e|e.to_string())?;let lines=s.lines().collect::<Vec<_>>();if lines.len()!=5||lines[0]!=MC{return Err("invalid Folia lock; restore it, do not change Minecraft version".into());}download(lines[2],lines[3],&jar)?;return Ok(jar);}
    if jar.exists(){return Err("runtime/folia.jar exists without a version/hash lock; refusing untracked binary".into());}
    // A fresh clone uses the reviewed server pin, never a silently newer build.
    let pin=root.join("pins/folia.lock");
    if pin.exists() {
        let bytes=fs::read(&pin).map_err(|e|e.to_string())?;
        let text=std::str::from_utf8(&bytes).map_err(|e|e.to_string())?;
        let lines=text.lines().collect::<Vec<_>>();
        if lines.len()!=5||lines[0]!=MC||!valid_hash(lines[3])||!lines[2].starts_with("https://fill-data.papermc.io/") {return Err("invalid checked-in Folia pin".into());}
        atomic(&lock,&bytes)?;
        return folia(root);
    }
    Err("pins/folia.lock is missing; restore the complete repository".into())
}
fn java_major(java:&Path)->Result<u32>{let o=Command::new(java).arg("-version").output().map_err(|e|e.to_string())?;let s=format!("{}{}",String::from_utf8_lossy(&o.stderr),String::from_utf8_lossy(&o.stdout));if !o.status.success(){return Err("Java -version failed".into());}let version=s.split('"').nth(1).ok_or("unrecognized Java version output")?;version.split('.').next().ok_or("invalid Java version")?.parse().map_err(|_|"invalid Java major".into())}
pub fn java(root:&Path)->Result<PathBuf>{
    if let Ok(custom)=env::var("JAVA_BIN"){let p=PathBuf::from(custom);if java_major(&p)?!=21{return Err("JAVA_BIN must be Java 21 for this pinned runtime".into());}return Ok(p);}
    let system=PathBuf::from("java");if java_major(&system)==Ok(21){return Ok(system);}
    let dest=root.join("runtime/java");let bin=dest.join("bin/java");if bin.exists(){if java_major(&bin)?==21{return Ok(bin);}return Err("cached Java is not version 21".into());}
    let arch=match env::consts::ARCH{"x86_64"=>"x64","aarch64"=>"aarch64",_=>return Err("only Linux x86_64 and aarch64 are supported".into())};
    let cache=root.join("runtime");fs::create_dir_all(&cache).map_err(|e|e.to_string())?;
    let lock=cache.join("java.lock");
    let (url,hash)=if lock.exists(){let s=fs::read_to_string(&lock).map_err(|e|e.to_string())?;let l=s.lines().collect::<Vec<_>>();if l.len()!=2{return Err("invalid Java lock".into());}(l[0].to_string(),l[1].to_string())}else{
        let j=metadata(&format!("https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture={arch}&image_type=jdk&os=linux&vendor=eclipse"))?;
        let p=j.array()?.first().ok_or("no Java 21 build returned")?.get("binary")?.get("package")?;
        let url=p.get("link")?.text()?.to_string();let hash=p.get("checksum")?.text()?.to_string();
        if url.contains(['\n','\r'])||!url.starts_with("https://")||!valid_hash(&hash){return Err("invalid Java metadata".into());}
        atomic(&lock,format!("{url}\n{hash}\n").as_bytes())?;(url,hash)
    };
    let archive=cache.join("java.tar.gz");download(&url,&hash,&archive)?;
    let staging=cache.join("java-staging");if staging.exists(){fs::remove_dir_all(&staging).map_err(|e|e.to_string())?;}fs::create_dir_all(&staging).map_err(|e|e.to_string())?;
    command(Command::new("tar").arg("-xzf").arg(&archive).arg("--strip-components=1").arg("-C").arg(&staging))?;
    if java_major(&staging.join("bin/java"))?!=21{return Err("downloaded Java failed version check".into());}
    fs::rename(staging,&dest).map_err(|e|e.to_string())?;Ok(bin)
}
#[cfg(test)]mod tests{use super::*;
    #[test]fn fixed_version_build_filter(){let data=json::parse(r#"[{"id":2,"channel":"STABLE","downloads":{"server:default":{"url":"https://example/a","checksums":{"sha256":"abc"}}}},{"id":8,"channel":"ALPHA","downloads":{"server:default":{"url":"https://example/b","checksums":{"sha256":"abc"}}}}]"#).unwrap();assert_eq!(select_folia(&data,None,false).unwrap().id,2);assert!(select_folia(&data,Some(8),false).is_err());assert_eq!(select_folia(&data,None,true).unwrap().id,8);}
    #[test]fn digest_syntax(){assert!(valid_hash(&"a".repeat(64)));assert!(!valid_hash(&"z".repeat(64)));assert!(!valid_hash(""));}
}
