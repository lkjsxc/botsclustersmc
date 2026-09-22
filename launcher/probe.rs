use std::{io::{Read,Write},net::{TcpStream,ToSocketAddrs},time::Duration};
use super::{json::{self,Json},install::Result};
fn var(n:u32,out:&mut Vec<u8>){let mut n=n;loop{let b=(n&127) as u8;n>>=7;out.push(if n==0{b}else{b|128});if n==0{break;}}}
fn read_var(r:&mut impl Read)->Result<u32>{let mut n=0;for shift in [0,7,14,21,28]{let mut b=[0];r.read_exact(&mut b).map_err(|e|e.to_string())?;if shift==28&&b[0]&0xf0!=0{return Err("oversized protocol VarInt".into());}n|=((b[0]&127) as u32)<<shift;if b[0]&128==0{return Ok(n);}}Err("invalid VarInt".into())}
fn packet(s:&mut impl Write,bytes:&[u8])->Result<()>{let mut out=vec![];var(bytes.len() as u32,&mut out);out.extend_from_slice(bytes);s.write_all(&out).map_err(|e|e.to_string())}
pub fn status(address:&str)->Result<Json>{
    let socket=address.to_socket_addrs().map_err(|e|e.to_string())?.next().ok_or("address did not resolve")?;
    let mut s=TcpStream::connect_timeout(&socket,Duration::from_secs(2)).map_err(|e|e.to_string())?;s.set_read_timeout(Some(Duration::from_secs(3))).map_err(|e|e.to_string())?;s.set_write_timeout(Some(Duration::from_secs(3))).map_err(|e|e.to_string())?;
    let host=socket.ip().to_string();let mut handshake=vec![0];var(u32::MAX,&mut handshake);var(host.len() as u32,&mut handshake);handshake.extend_from_slice(host.as_bytes());handshake.extend_from_slice(&socket.port().to_be_bytes());var(1,&mut handshake);packet(&mut s,&handshake)?;packet(&mut s,&[0])?;
    let len=read_var(&mut s)? as usize;if !(2..=1_048_576).contains(&len){return Err("status packet length outside bound".into());}let mut b=vec![0;len];s.read_exact(&mut b).map_err(|e|e.to_string())?;let mut reader=&b[..];if read_var(&mut reader)?!=0{return Err("not a status response".into());}let n=read_var(&mut reader)? as usize;if n!=reader.len(){return Err("invalid status JSON length".into());}json::parse(std::str::from_utf8(reader).map_err(|e|e.to_string())?)
}
#[cfg(test)]mod tests{use super::*;
    #[test]fn varints(){for n in [0,1,127,128,255,65535,2147483647,u32::MAX]{let mut b=vec![];var(n,&mut b);assert_eq!(read_var(&mut &b[..]).unwrap(),n);}}
    #[test]fn rejects_oversized(){assert!(read_var(&mut &[255,255,255,255,31][..]).is_err());assert!(read_var(&mut &[128][..]).is_err());}
}
