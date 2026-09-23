"""Reviewed integration corrections; executed after prepare.py in the workbench."""
from pathlib import Path
import re
p=Path('learning/src/ppo.rs');s=p.read_text();s=re.sub(r'(->\s*Transition)\{ticks:4,',r'\1{',s);s=s.replace('network::{Model, log_probability}','network::Model');p.write_text(s)
p=Path('learning/src/next/math/trust.rs');s=p.read_text();a=s.index('#[cfg(test)]');s=s[:a]+s[a:].replace('use super::super::Rng;','use super::super::super::Rng;');p.write_text(s)
p=Path('learning/src/next/cohort.rs');s=p.read_text().replace('[0,33]','[0,65]').replace('[0, 33]','[0, 65]')
a=s.index('    #[test] fn all_32_actors');b=s.index('    #[test] fn rejects_wrong_version',a)
s=s[:a]+'''    #[test] fn all_64_actors_contribute_and_no_samples_vanish(){
        let mut c=Cohort::new(r(),64,64,300).unwrap();
        for i in (0..64).rev(){c.push(packet(i,0,32,false),|_|Ok(())).unwrap();}
        assert_eq!(c.len(),2048);assert!(!c.ready());
        for i in 0..63{c.push(packet(i,1,40,true),|_|Ok(())).unwrap();}
        assert_eq!(c.outstanding_actors(),[63]);assert!(c.take_ready().is_err());
        c.push(packet(63,1,40,true),|_|Ok(())).unwrap();
        let b=c.take_ready().unwrap();assert_eq!(b.len(),4608);assert_eq!(b.fragments,128);assert_eq!(b.trajectories.len(),64);
        for(i,ts)in b.trajectories.iter().enumerate(){assert_eq!(ts.len(),72);assert!(ts.iter().all(|x|*x==i as u32));}
        assert!(c.take_ready().is_err());c.commit(Round{policy_version:8,generation:2}).unwrap();
        assert_eq!(c.len(),0);assert_eq!(c.outstanding_actors().len(),64);
    }
'''+s[b:];p.write_text(s)
p=Path('learning/src/curriculum.rs');s=p.read_text();s=re.sub(r'id\s*<\s*32\s*&&\s*stage','id < 64 && stage',s);p.write_text(s)
for name in ['app/settings.rs','launcher/config.rs']:
 p=Path(name);s=p.read_text();s=re.sub(r'(number\("BOTS",\s*)32(\s*,\s*1,\s*)32\)',r'\g<1>64\g<2>64)',s);p.write_text(s)
for name in ['academy.sh','scripts/runtime-config.sh']:
 p=Path(name);s=p.read_text();s=re.sub(r'BOTS\s*<=\s*32','BOTS<=64',s);p.write_text(s)
p=Path('bridge/src/org/botsclustersmc/lab/EnvironmentConfig.java');s=p.read_text();s=re.sub(r'bots\s*>\s*32','bots>64',s);p.write_text(s)
p=Path('app/agent.rs');s=p.read_text().replace('engine::policy_id(&model)!=lesson.ticket.policy','model.version!=lesson.ticket.policy.version || !Arc::ptr_eq(&model,self.academy.policy.as_ref().ok_or("missing issued policy")?)');p.write_text(s)
print('Corrected nested imports, 64-actor accounting/boundaries and immutable policy identity checks.')
