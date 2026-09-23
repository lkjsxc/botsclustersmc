"""Finite diagnostic corrections; never part of the normal training executable."""
from pathlib import Path
p=Path('tests/live-client.rs');s=p.read_text()
if 'let mut control=f.clone();' not in s:
    start=s.index('        let mut a=IDLE;\n        if let Some(op)=self.ops.front_mut()')
    end=s.index('\n        act::apply(bot,&a,bot.menu().slots().len());',start)
    body=s[start:end].replace('&f,','&control,').replace('f.position','control.position')
    body=body.replace('if l.goal[2]-control.position[2]>0.55{a[0]=1;}', 'if l.goal[2]-control.position[2]>0.55 && self.ticks%16==0{a[0]=1;}')
    body=body.replace('if dist>0.55{if aim(&control,l.goal,false,&mut a){a[0]=1;}if self.stage==4{a[3]=1;}}', 'if dist>0.55{if aim(&control,l.goal,false,&mut a) && (dist>2.0 || self.ticks%16==0){a[0]=1;}if self.stage==4 && dist>1.5{a[3]=1;}}')
    prefix='''        // Diagnostic control uses the current input-device view; scoring above
        // still uses only the authoritative server frame and unchanged task gate.
        // A four-tick movement pulse followed by twelve idle ticks avoids stale
        // telemetry making the irreversible forward-only diagnostic overshoot.
        let mut control=f.clone();let p=bot.position();let direction=bot.direction();
        control.position=[p.x,p.y,p.z];control.yaw=direction.y_rot();control.pitch=direction.x_rot();
'''
    body+='''
        if self.stage<=4 && f.tick<240 && self.ticks%16==0 {
            eprintln!("DIAGNOSTIC TRACE task={} client_tick={} server_tick={} server={:?} client={:?} yaw={:.2} input={:?}",self.stage,self.ticks,f.tick,f.position,control.position,control.yaw,a);
        }'''
    s=s[:start]+prefix+body+s[end:]
    old='if s.ticks%4==0&&s.ticks>12{s.fixture(&bot)?;}act::tick_camera(&bot,&s.last);'
    new='if s.ticks%4==0&&s.ticks>12{s.fixture(&bot)?;}if s.stage==0 && s.ticks%16==4 && s.last[0]!=0{act::stop(&bot);s.last=IDLE;}act::tick_camera(&bot,&s.last);'
    assert old in s;s=s.replace(old,new)

# Independent full-difficulty environments execute concurrently. A genuine task
# failure remains a failure, but no longer prevents checking every other stage.
if 'static FINISHED:' not in s:
    s=s.replace('atomic::{AtomicBool,Ordering}', 'atomic::{AtomicBool,AtomicUsize,Ordering}')
    s=s.replace('static DONE:AtomicBool=AtomicBool::new(false);','static DONE:AtomicBool=AtomicBool::new(false);\nstatic FINISHED:AtomicUsize=AtomicUsize::new(0);')
    s=s.replace('struct Check{ticks:u64,ready:bool,stage:usize,','struct Check{ticks:u64,ready:bool,stage:usize,actor:usize,finished:bool,')
    s=s.replace('Self{ticks:0,ready:false,stage:0,','Self{ticks:0,ready:false,stage:0,actor:0,finished:false,')
    s=s.replace('session:Session{run:11,actor:0,generation:1,','session:Session{run:11,actor:self.actor as u8,generation:1,')
    s=s.replace('format!("BCMCLAB3 {} {} 0 {} ', 'format!("BCMCLAB3 {} {} {} {} ')
    s=s.replace('RUN.get().unwrap(),l.token(),l.stage,l.start[0]', 'RUN.get().unwrap(),l.token(),self.actor,l.stage,l.start[0]')
    s=s.replace('root().join(".runtime/lab/request-0.txt")', 'root().join(format!(".runtime/lab/request-{}.txt",self.actor))')
    s=s.replace('root().join(".runtime/lab/frame-0.txt")', 'root().join(format!(".runtime/lab/frame-{}.txt",l.choice.session.actor))')
    s=s.replace('if count(3)?!=0{', 'if count(3)?!=l.choice.session.actor as u32{')
    s=s.replace('.step(0,&l,f.clone(),&self.last,0.997)', '.step(self.actor,&l,f.clone(),&self.last,0.997)')
    start=s.index('                if !outcome.success{return Err(format!("fixture')
    end=s.index('\n            }\n        }\n',start)
    s=s[:start]+'''                if !outcome.success {
                    let message=format!("fixture {} failed {}: frame={f:?} operation={:?} menu={:?}",self.stage,outcome.reason,self.ops.front(),bot.menu());
                    eprintln!("DIAGNOSTIC TASK FAILURE: {message}");
                    let mut error=ERROR.get().unwrap().lock().unwrap();if error.is_none(){*error=Some(message);}
                } else {
                    eprintln!("DIAGNOSTIC PASS stage={} ticks={} counters={:?} stock={:?} target_stock={} occupancy={}",self.stage,f.tick,f.evidence.counters,f.evidence.stock,f.evidence.target_stock,f.evidence.occupied_targets);
                }
                self.finished=true;
                if FINISHED.fetch_add(1,Ordering::SeqCst)+1==18 {
                    if ERROR.get().unwrap().lock().unwrap().is_none(){eprintln!("PASS: all 18 full-difficulty fixtures reached through separately scripted literal inputs; NOT learned behavior");}
                    else{eprintln!("FAIL: all 18 fixtures completed; retain every failed task above");}
                    DONE.store(true,Ordering::Relaxed);
                }
                return Ok(());''' + s[end:]
    s=s.replace('if s.ready&&bot.exists(){s.ticks+=1;', 'if s.ready&&!s.finished&&bot.exists(){s.ticks+=1;')
    old='let server=env::var("BOT_SERVER")?;let name=if observer{"bcmcObserver"}else{"bcmc00"};let state=State(Arc::new(Mutex::new(Check::new(observer))));'
    assert old in s;s=s.replace(old,'let server=env::var("BOT_SERVER")?;')
    old='let builder=SwarmBuilder::new_without_plugins()'
    assert old in s;s=s.replace(old,'let mut builder=SwarmBuilder::new_without_plugins()')
    old='.set_handler(handler).reconnect_after(None).add_account_with_state(Account::offline(name),state);'
    new='''.set_handler(handler).reconnect_after(None).join_delay(Duration::from_millis(400));
            for actor in 0..if observer{1}else{18}{
                let name=if observer{"bcmcObserver".to_string()}else{format!("bcmc{actor:02}")};
                let check=Check{actor,stage:actor,..Check::new(observer)};
                builder=builder.add_account_with_state(Account::offline(&name),State(Arc::new(Mutex::new(check))));
            }'''
    assert old in s;s=s.replace(old,new)
    # A real server inventory may arrive after the teleport acknowledgement.
    old='let index=player_range(&menu).find(|&i|slots[i].kind()==*kind&&!slots[i].is_empty()).ok_or_else(||format!("missing raw test ingredient {kind:?}; menu={menu:?}"))?;'
    new='let Some(index)=player_range(&menu).find(|&i|slots[i].kind()==*kind&&!slots[i].is_empty()) else{return Ok(false);};'
    assert old in s;s=s.replace(old,new)
p.write_text(s)

p=Path('tests/live-fixtures.py');s=p.read_text()
if 'failed_modes=[]' not in s:
    s=s.replace("    for mode in ['fixtures','observer']:","    failed_modes=[]\n    for mode in ['fixtures','observer']:")
    s=s.replace("        if test.returncode:raise RuntimeError(f'{mode} failed with exit {test.returncode}')","        if test.returncode:failed_modes.append(f'{mode} exited {test.returncode}')")
    s=s.replace("    result=0\nfinally:","    if failed_modes:raise RuntimeError('; '.join(failed_modes))\n    result=0\nfinally:")
    p.write_text(s)
print('Full-difficulty diagnostic matrix and observer checks are independent; normal learned actors and task gates unchanged.')
