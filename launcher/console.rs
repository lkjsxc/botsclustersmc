//! Operator-local IPC only. No TCP control API, RCON, or bot operator rights.
use std::{fs,io::{Read,Write},os::unix::{fs::PermissionsExt,net::{UnixListener,UnixStream}},path::Path,time::Duration};
use super::install::Result;
pub fn listen(root:&Path)->Result<UnixListener>{
    let path=root.join(".runtime/console.sock");if path.exists(){fs::remove_file(&path).map_err(|e|e.to_string())?;}
    let listener=UnixListener::bind(&path).map_err(|e|format!("console socket: {e}; use a shorter installation path if necessary"))?;
    fs::set_permissions(path,fs::Permissions::from_mode(0o600)).map_err(|e|e.to_string())?;
    listener.set_nonblocking(true).map_err(|e|e.to_string())?;Ok(listener)
}
fn read_line(stream:&mut UnixStream)->Result<String>{
    stream.set_read_timeout(Some(Duration::from_secs(2))).map_err(|e|e.to_string())?;
    let mut out=Vec::new();let mut b=[0];for _ in 0..4096{let n=stream.read(&mut b).map_err(|e|e.to_string())?;if n==0||b[0]==b'\n'{return String::from_utf8(out).map_err(|e|e.to_string());}out.push(b[0]);}Err("console command length limit".into())
}
pub fn poll(listener:&UnixListener,stdin:&mut impl Write){
    // At most one local command per supervisor iteration.
    if let Ok((mut stream,_))=listener.accept(){let result=(||->Result<()>{let command=read_line(&mut stream)?;
        if command.contains(['\r','\0'])||command.trim().is_empty(){return Err("invalid command".into());}
        if command.trim()=="stop"{super::STOP.store(true,std::sync::atomic::Ordering::Relaxed);}else{writeln!(stdin,"{command}").map_err(|e|e.to_string())?;stdin.flush().map_err(|e|e.to_string())?;}
        Ok(())})();let message=match result{Ok(())=>"delivered; inspect Folia logs for the command result".to_string(),Err(e)=>format!("error: {e}")};let _=writeln!(stream,"{message}");}
}
pub fn send(root:&Path,command:&str)->Result<()>{
    if command.len()>4000||command.contains(['\n','\r','\0']){return Err("send one command, at most 4000 bytes".into());}
    let mut stream=UnixStream::connect(root.join(".runtime/console.sock")).map_err(|e|format!("no running local console: {e}"))?;
    writeln!(stream,"{command}").map_err(|e|e.to_string())?;let reply=read_line(&mut stream)?;
    if reply.starts_with("error:"){return Err(reply);}println!("{reply}");Ok(())
}
