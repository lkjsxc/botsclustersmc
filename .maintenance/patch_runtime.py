"""Finite diagnostic corrections; never part of the normal training executable."""
from pathlib import Path
p=Path('tests/live-client.rs');s=p.read_text()
if 'let mut control=f.clone();' not in s:
    start=s.index('        let mut a=IDLE;\n        if let Some(op)=self.ops.front_mut()')
    end=s.index('\n        act::apply(bot,&a,bot.menu().slots().len());',start)
    body=s[start:end].replace('&f,','&control,').replace('f.position','control.position')
    body=body.replace('if l.goal[2]-control.position[2]>0.55{a[0]=1;}', 'if l.goal[2]-control.position[2]>0.55 && self.ticks%16==0{a[0]=1;}')
    body=body.replace('if dist>0.55{if aim(&control,l.goal,false,&mut a){a[0]=1;}if self.stage==4{a[3]=1;}}', 'if dist>0.55{if aim(&control,l.goal,false,&mut a) && (dist>2.0 || self.ticks%16==0){a[0]=1;}if self.stage==4 && dist>1.5{a[3]=1;}}')
    prefix='''        // A diagnostic controller may use the current client input-device view.
        // Success STILL comes exclusively from the preceding authoritative gate.
        // Near a stop target use four-tick pulses, then allow twelve ticks to settle.
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
    p.write_text(s)

# Keep observer acceptance independent: a fixture failure must not suppress its
# commands/network checks, but the combined diagnostic still exits nonzero.
p=Path('tests/live-fixtures.py');s=p.read_text()
if 'failed_modes=[]' not in s:
    s=s.replace("    for mode in ['fixtures','observer']:","    failed_modes=[]\n    for mode in ['fixtures','observer']:")
    s=s.replace("        if test.returncode:raise RuntimeError(f'{mode} failed with exit {test.returncode}')","        if test.returncode:failed_modes.append(f'{mode} exited {test.returncode}')")
    s=s.replace("    result=0\nfinally:","    if failed_modes:raise RuntimeError('; '.join(failed_modes))\n    result=0\nfinally:")
    p.write_text(s)
print('Updated only scripted diagnostic feedback/pulses and independent observer coverage; learning actions and success criteria unchanged.')
