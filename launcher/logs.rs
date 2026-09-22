use std::{fs::{self,File,OpenOptions},io::{self,Read,Write},path::{Path,PathBuf},thread};
fn path(base:&Path,i:usize)->PathBuf{base.with_extension(format!("log.{i}"))}
fn rotate(base:&Path)->io::Result<()>{let _=fs::remove_file(path(base,4));for i in (1..4).rev(){let a=path(base,i);if a.exists(){fs::rename(a,path(base,i+1))?;}}if base.exists(){fs::rename(base,path(base,1))?;}Ok(())}
pub fn pipe<R:Read+Send+'static>(mut reader:R,path:PathBuf)->thread::JoinHandle<()>{thread::spawn(move||{
    let result=(||->io::Result<()>{let mut f=OpenOptions::new().create(true).append(true).open(&path)?;let mut size=f.metadata()?.len();let mut b=[0;8192];let mut tail=Vec::new();let is_folia=path.file_name().is_some_and(|s|s.to_string_lossy().starts_with("folia."));loop{let n=reader.read(&mut b)?;if n==0{break;}if is_folia{tail.extend_from_slice(&b[..n]);if tail.len()>16384{tail.drain(..tail.len()-16384);}let text=String::from_utf8_lossy(&tail);if text.contains("Done (")&&text.contains("For help"){super::SERVER_READY.store(true,std::sync::atomic::Ordering::Relaxed);}}if size+n as u64>8*1024*1024{drop(f);rotate(&path)?;f=File::create(&path)?;size=0;}f.write_all(&b[..n])?;size+=n as u64;}f.flush()})();if let Err(e)=result{eprintln!("log writer failed: {e}");super::LOGGER_FAILED.store(true,std::sync::atomic::Ordering::Relaxed);super::STOP.store(true,std::sync::atomic::Ordering::Relaxed);}
})}
pub fn prune_old_folia_logs(root:&Path){
    // Never touch the live latest.log, a world file, or an arbitrary directory.
    let Ok(entries)=fs::read_dir(root.join("server/logs"))else{return;};
    let mut files=entries.filter_map(|e|e.ok()).filter(|e|e.file_type().map(|t|t.is_file()).unwrap_or(false)&&e.file_name().to_string_lossy().ends_with(".log.gz")).collect::<Vec<_>>();
    files.sort_by_key(|e|e.metadata().and_then(|m|m.modified()).ok());
    let remove=files.len().saturating_sub(14);for f in files.into_iter().take(remove){let _=fs::remove_file(f.path());}
}
