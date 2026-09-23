"""Bounded reset and independent diagnostic fixes; removed before final delivery."""
from pathlib import Path

p=Path('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java');s=p.read_text()
if 'private void clearTaskInventory(' not in s:
    s=s.replace('p.setItemOnCursor(null);p.closeInventory();p.getInventory().clear();','clearTaskInventory(p);')
    needle='    private void furnish(Player p,Session s){'
    assert needle in s
    helper='''    /** Reset environment only: no stale cursor, preview or personal recipe input. */
    private void clearTaskInventory(Player p){
        p.setItemOnCursor(null);p.closeInventory();p.getInventory().clear();
        if(p.getOpenInventory().getTopInventory() instanceof CraftingInventory grid){
            grid.setMatrix(new ItemStack[grid.getMatrix().length]);grid.setResult(null);
        }
        // Closing a screen may settle its carried stack. Clear AFTER close too.
        p.setItemOnCursor(null);p.updateInventory();
    }
'''
    s=s.replace(needle,helper+needle)
    s=s.replace('s.transferStock=containerStock(s);s.startedAge=p.getTicksLived();s.ready=true;', 'p.updateInventory();s.transferStock=containerStock(s);s.startedAge=p.getTicksLived();s.ready=true;')
    p.write_text(s)

p=Path('tests/live-client.rs');s=p.read_text()
if 'AwaitItem(ItemKind)' not in s:
    s=s.replace('Close,Select,Wait(u8)}','Close,Select,Wait(u8),AwaitItem(ItemKind),AwaitCount(i32)}')
    start=s.index('fn run_op(');end=s.index('\nfn read_frame(',start)
    s=s[:start]+'''fn run_op(bot:&Client,f:&Frame,op:&mut Op,a:&mut[usize;8])->Result<bool,String>{
    let menu=bot.menu();let slots=menu.slots();let cursor=carried(bot);
    match op.clone(){
        Op::Take(kind)=>{
            if !cursor.is_empty(){return Err(format!("diagnostic Take({kind:?}) has unexpected cursor {cursor:?}"));}
            let Some(index)=player_range(&menu).find(|&i|slots[i].kind()==kind&&!slots[i].is_empty())else{return Ok(false);};
            a[6]=1;a[7]=index;*op=Op::AwaitItem(kind);return Ok(false);
        },
        Op::Left(slot)=>{a[6]=1;a[7]=slot;*op=Op::AwaitCount(0);return Ok(false);},
        Op::Right(slot)=>{
            if cursor.is_empty(){return Err("diagnostic right-place has an empty cursor".into());}
            a[6]=2;a[7]=slot;*op=Op::AwaitCount(cursor.count()-1);return Ok(false);
        },
        Op::Park=>{if !cursor.is_empty(){let index=player_range(&menu).find(|&i|slots[i].is_empty()).ok_or("no empty diagnostic inventory slot")?;a[6]=1;a[7]=index;*op=Op::AwaitCount(0);return Ok(false);}},
        Op::Align(g)=>{if !aim(f,g,true,a){return Ok(false);}},
        Op::Use=>a[4]=2,
        Op::Menu(expected)=>{let ok=match expected{1=>matches!(menu,Menu::Crafting{..}),2=>matches!(menu,Menu::Furnace{..}),3=>matches!(menu,Menu::Generic9x3{..}),_=>false};if !ok{return Ok(false);}},
        Op::Output(slot,kind)=>{
            if slots.get(slot).is_none_or(|i|i.is_empty()||i.kind()!=kind){return Ok(false);}
            if !cursor.is_empty(){return Err("diagnostic output click has a nonempty cursor".into());}
            a[6]=1;a[7]=slot;*op=Op::AwaitItem(kind);return Ok(false);
        },
        Op::AwaitItem(kind)=>{if cursor.is_empty()||cursor.kind()!=kind{return Ok(false);}},
        Op::AwaitCount(n)=>{let count=if cursor.is_empty(){0}else{cursor.count()};if count!=n{return Ok(false);}},
        Op::Close=>a[6]=5,Op::Select=>a[5]=1,
        Op::Wait(n)=>{if n>0{*op=Op::Wait(n-1);return Ok(false);}}
    }Ok(true)
}''' +s[end:]
    s=s.replace('actor:usize,finished:bool,','actor:usize,finished:bool,reset_check:bool,')
    s=s.replace('actor:0,finished:false,','actor:0,finished:false,reset_check:false,')
    s=s.replace('lesson:self.stage as u64+1}', 'lesson:self.stage as u64+1+if self.reset_check{100}else{0}}')
    old='if self.episode.is_none(){let p=bot.position();if(p.x-f.position[0]).abs()>0.5||(p.y-f.position[1]).abs()>0.5||(p.z-f.position[2]).abs()>0.5{return Ok(());}self.episode=Some(Episode::new(&l,f.clone())?);}'
    new='''if self.episode.is_none(){
            let p=bot.position();if(p.x-f.position[0]).abs()>0.5||(p.y-f.position[1]).abs()>0.5||(p.z-f.position[2]).abs()>0.5||!carried(bot).is_empty()||!matches!(bot.menu(),Menu::Player(_)){return Ok(());}
            if self.reset_check && f.evidence.stock.iter().sum::<u32>()!=2{return Err(format!("reset retained old crafted output: {:?}",f.evidence.stock));}
            self.episode=Some(Episode::new(&l,f.clone())?);
        }'''
    assert old in s;s=s.replace(old,new)
    old='                self.finished=true;'
    new='''                // The plank output is deliberately still on the cursor here.
                // Reusing this client for sticks checks the real reset boundary.
                if self.stage==8&&!self.reset_check {
                    eprintln!("DIAGNOSTIC RESET CHECK: same actor, crafted plank cursor -> fresh two-plank stick task");
                    self.reset_check=true;self.stage=9;self.lesson=None;self.episode=None;self.ops.clear();return Ok(());
                }
                self.finished=true;'''
    assert old in s;s=s.replace(old,new)
    old='if let Some(op)=self.ops.front_mut(){if run_op(bot,&control,op,&mut a)?{self.ops.pop_front();}}'
    new='''if let Some(op)=self.ops.front_mut(){
            let before=op.clone();let completed=run_op(bot,&control,op,&mut a)?;
            if completed||a[6]!=0{eprintln!("DIAGNOSTIC GUI actor={} task={} tick={} op={before:?} input={:?} cursor={:?}",self.actor,self.stage,f.tick,a,carried(bot));}
            if completed{self.ops.pop_front();}
        }'''
    assert old in s;s=s.replace(old,new)
    p.write_text(s)
print('Explicitly reset cursor/grid/preview state; diagnostic GUI steps await outcomes and test a dirty-cursor episode boundary.')
