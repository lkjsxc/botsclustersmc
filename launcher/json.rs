//! Small bounded JSON reader for official download metadata and local status.
//! It is not a shell/eval parser. Duplicate keys and malformed Unicode fail.
use std::collections::BTreeMap;
#[derive(Debug,Clone,PartialEq)]pub enum Json{Null,Bool(bool),Number(String),String(String),Array(Vec<Json>),Object(BTreeMap<String,Json>)}
impl Json{
    pub fn get(&self,key:&str)->Result<&Json,String>{match self{Self::Object(m)=>m.get(key).ok_or_else(||format!("missing JSON field {key}")),_=>Err("expected object".into())}}
    pub fn text(&self)->Result<&str,String>{match self{Self::String(s)=>Ok(s),_=>Err("expected JSON string".into())}}
    pub fn integer(&self)->Result<u64,String>{match self{Self::Number(s)=>s.parse().map_err(|_|"expected nonnegative integer".into()),_=>Err("expected JSON number".into())}}
    pub fn array(&self)->Result<&[Json],String>{match self{Self::Array(a)=>Ok(a),_=>Err("expected JSON array".into())}}
}
pub fn parse(s:&str)->Result<Json,String>{if s.len()>4*1024*1024{return Err("JSON exceeds 4 MiB limit".into());}let mut p=Parser{b:s.as_bytes(),i:0};let v=p.value(0)?;p.ws();if p.i!=p.b.len(){return Err("trailing JSON input".into());}Ok(v)}
struct Parser<'a>{b:&'a[u8],i:usize}
impl Parser<'_>{
    fn ws(&mut self){while self.i<self.b.len()&&matches!(self.b[self.i],b' '|b'\n'|b'\t'|b'\r'){self.i+=1;}}
    fn take(&mut self)->Result<u8,String>{if let Some(&b)=self.b.get(self.i){self.i+=1;Ok(b)}else{Err("unexpected end of JSON".into())}}
    fn expect(&mut self,b:u8)->Result<(),String>{if self.take()?==b{Ok(())}else{Err("unexpected JSON token".into())}}
    fn word(&mut self,tail:&[u8])->Result<(),String>{for &b in tail{self.expect(b)?;}Ok(())}
    fn hex4(&mut self)->Result<u32,String>{let mut n=0;for _ in 0..4{let c=self.take()?;n=n*16+(c as char).to_digit(16).ok_or("invalid Unicode escape")?;}Ok(n)}
    fn string(&mut self)->Result<String,String>{let mut out=Vec::new();loop{match self.take()?{
        b'"'=>break,
        b'\\'=>match self.take()?{b'"'=>out.push(b'"'),b'\\'=>out.push(b'\\'),b'/'=>out.push(b'/'),b'b'=>out.push(8),b'f'=>out.push(12),b'n'=>out.push(10),b'r'=>out.push(13),b't'=>out.push(9),b'u'=>{
            let mut u=self.hex4()?;if (0xd800..=0xdbff).contains(&u){self.expect(b'\\')?;self.expect(b'u')?;let low=self.hex4()?;if !(0xdc00..=0xdfff).contains(&low){return Err("unpaired Unicode surrogate".into());}u=0x10000+((u-0xd800)<<10)+(low-0xdc00);}
            let c=char::from_u32(u).ok_or("invalid Unicode scalar")?;let mut buf=[0;4];out.extend_from_slice(c.encode_utf8(&mut buf).as_bytes());
        },_=>return Err("invalid JSON escape".into())},
        b if b<32=>return Err("unescaped control character".into()),b=>out.push(b),
    }}String::from_utf8(out).map_err(|_|"non-UTF8 JSON string".into())}
    fn value(&mut self,depth:usize)->Result<Json,String>{if depth>64{return Err("JSON nesting limit".into());}self.ws();let start=self.i;Ok(match self.take()?{
        b'n'=>{self.word(b"ull")?;Json::Null},b't'=>{self.word(b"rue")?;Json::Bool(true)},b'f'=>{self.word(b"alse")?;Json::Bool(false)},
        b'"'=>Json::String(self.string()?),
        b'['=>{let mut a=vec![];self.ws();if self.b.get(self.i)==Some(&b']'){self.i+=1;Json::Array(a)}else{loop{a.push(self.value(depth+1)?);self.ws();match self.take()?{b']'=>break,b','=>{},_=>return Err("array separator".into())}}Json::Array(a)}},
        b'{'=>{let mut m=BTreeMap::new();self.ws();if self.b.get(self.i)==Some(&b'}'){self.i+=1;Json::Object(m)}else{loop{self.ws();self.expect(b'"')?;let key=self.string()?;self.ws();self.expect(b':')?;let v=self.value(depth+1)?;if m.insert(key,v).is_some(){return Err("duplicate JSON key".into());}self.ws();match self.take()?{b'}'=>break,b','=>{},_=>return Err("object separator".into())}}Json::Object(m)}},
        b'-'|b'0'..=b'9'=>{
            self.i=start;if self.b[self.i]==b'-'{self.i+=1;}
            if self.b.get(self.i)==Some(&b'0'){self.i+=1;}else{let first=self.i;while self.b.get(self.i).is_some_and(u8::is_ascii_digit){self.i+=1;}if self.i==first{return Err("invalid number".into());}}
            if self.b.get(self.i)==Some(&b'.'){self.i+=1;let first=self.i;while self.b.get(self.i).is_some_and(u8::is_ascii_digit){self.i+=1;}if self.i==first{return Err("invalid fraction".into());}}
            if matches!(self.b.get(self.i),Some(b'e'|b'E')){self.i+=1;if matches!(self.b.get(self.i),Some(b'+'|b'-')){self.i+=1;}let first=self.i;while self.b.get(self.i).is_some_and(u8::is_ascii_digit){self.i+=1;}if self.i==first{return Err("invalid exponent".into());}}
            Json::Number(std::str::from_utf8(&self.b[start..self.i]).map_err(|_|"invalid number encoding")?.to_string())
        },_=>return Err("invalid JSON value".into())
    })}
}
#[cfg(test)]mod tests{use super::*;
    #[test]fn valid_nested_unicode(){let j=parse(r#"{"a":[1,true,null,"\uD83D\uDE00"],"b":"日本語"}"#).unwrap();assert_eq!(j.get("a").unwrap().array().unwrap()[3].text().unwrap(),"😀");assert_eq!(j.get("b").unwrap().text().unwrap(),"日本語");}
    #[test]fn malformed_fails(){for s in ["", "[1,]", "{\"x\":0,\"x\":1}","01","-","1.","1e+","true x",r#""\uD800""#,r#""\uDC00""#]{assert!(parse(s).is_err(),"{s}");}}
    #[test]fn exact_large_integer(){assert_eq!(parse("18446744073709551615").unwrap().integer().unwrap(),u64::MAX);}
}
