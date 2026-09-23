"""Finite reviewed corrections before final mainline publication."""
from pathlib import Path

def edit(name, old, new):
    p=Path(name);s=p.read_text()
    if new in s:return
    assert old in s,(name,old)
    p.write_text(s.replace(old,new))

# Compute an immutable policy identity once per publication, not for every action.
edit('learning/src/control.rs','pub policy:Arc<Model>,pub course:Curriculum,','pub policy:Arc<Model>,identity:PolicyId,pub course:Curriculum,')
edit('learning/src/control.rs','Ok(Self{policy:Arc::new(b.checkpoint.model),','let identity=policy_id(&b.checkpoint);\n        Ok(Self{identity,policy:Arc::new(b.checkpoint.model),')
edit('learning/src/control.rs','pub fn id(&self)->PolicyId{PolicyId{version:self.policy.version,signature:self.policy.fingerprint()}}','pub fn id(&self)->PolicyId{self.identity}')
edit('learning/src/control.rs','self.policy=Arc::new(b.checkpoint.model.clone());','self.identity=policy_id(&b.checkpoint);self.policy=Arc::new(b.checkpoint.model.clone());')

# Personal inventory and workbench both have 46 slots. Length is not menu type.
edit('app/sensor.rs','use azalea_inventory::ItemStack;','use azalea_inventory::{ItemStack,Menu};')
edit('app/sensor.rs','pub slots:usize,pub view:AgentView','pub slots:usize,pub menu_kind:usize,pub view:AgentView')
edit('app/sensor.rs','if slots.len()!=46{1.0}else{0.0}','if matches!(menu,Menu::Player(_)){0.0}else{1.0}')
edit('app/sensor.rs','Observation{frame:f,obs,mask,slots:n,view:','Observation{frame:f,obs,mask,slots:n,menu_kind:menu_kind(&menu),view:')
edit('app/sensor.rs','o.frame[617..704].copy_from_slice(features);','o.frame[617..704].copy_from_slice(features);\n    o.frame[687+o.menu_kind]=1.0;')
p=Path('app/sensor.rs');s=p.read_text()
if 'fn menu_kind(' not in s:
    s+='''\n/// Observable GUI identity, never a recipe answer or teacher-selected slot.
fn menu_kind(menu:&Menu)->usize{match menu{Menu::Player(_)=>0,Menu::Crafting(_)=>1,Menu::Furnace(_)=>2,Menu::Generic9x3(_)|Menu::Generic9x6(_)=>3,_=>4}}
#[cfg(test)]mod menu_tests{
    use super::*;
    #[test]fn equal_length_menus_are_distinct_observations(){let a=Menu::Player(Default::default());let b=Menu::Crafting(Default::default());assert_eq!(a.slots().len(),b.slots().len());assert_ne!(menu_kind(&a),menu_kind(&b));assert_eq!(menu_kind(&Menu::Furnace(Default::default())),2);}
}
'''
    s=s.replace('curriculum uses its 23 padded','the expanded curriculum and GUI identity use its reserved').replace('/// values. The frame stack','/// tail values. The frame stack')
    p.write_text(s)
edit('app/engine.rs','botsclustersmc-academy-v2-18tasks-conditional-gui-timed-gae\\n','botsclustersmc-academy-v2-18tasks-conditional-gui-timed-gae-menu-types\\n')

# Item merging must not lend an old/foreign drop a current episode receipt.
p=Path('bridge/src/org/botsclustersmc/lab/BotsClustersMCLab.java');s=p.read_text()
if 'void merging(ItemMergeEvent' not in s:
    at=s.index('    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void pickup(')
    s=s[:at]+'''    // Each target pickup retains its individual episode provenance until removal.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true) public void merging(ItemMergeEvent e){if(e.getEntity().getWorld()==world)e.setCancelled(true);}
'''+s[at:];p.write_text(s)

# Inactive conditional slots are canonical zero even when mechanical slot zero
# is masked. The likelihood, sampler and transition validator must agree.
p=Path('learning/src/ppo.rs');s=p.read_text()
old='for (&n,&a) in m.heads.iter().zip(&t.actions) {\n        if a>=n || !t.mask[off+a] || !t.mask[off..off+n].iter().any(|v|*v) {'
new='for (h,(&n,&a)) in m.heads.iter().zip(&t.actions).enumerate() {\n        let ignored=m.heads.as_slice()==super::HEADS && h==7 && !(1..=3).contains(&t.actions[6]);\n        if a>=n || (!ignored && !t.mask[off+a]) || (ignored && a!=0) || !t.mask[off..off+n].iter().any(|v|*v) {'
if old in s:s=s.replace(old,new)
else:assert new in s
if 'fn canonical_inactive_slot_survives_native_ppo_validation' not in s:
    s+='''\n#[cfg(test)]mod conditional_tests{
    use super::*;
    #[test]fn canonical_inactive_slot_survives_native_ppo_validation(){
        let m=Model::new(1,4,super::super::HEADS.to_vec(),1);let mut mask=vec![true;super::super::ACTIONS];
        let parent=super::super::ACTIONS-90-6;for i in 1..6{mask[parent+i]=false;}mask[super::super::ACTIONS-90]=false;
        let d=m.decide(&[0.2],&mask,&mut Rng(3),false);assert_eq!(d.actions[6],0);assert_eq!(d.actions[7],0);
        let t=Transition{obs:vec![0.2],mask,actions:d.actions,old_logp:d.logp,value:d.value,next_value:0.,reward:1.,terminal:true,elapsed_ticks:4,truncated:false,episode:1,tick:4};
        assert!(prepare(vec![Rollout{version:0,steps:vec![t]}],&m,&Params::default()).is_ok());
    }
}
'''
p.write_text(s)
print('Removed per-action whole-model hashing; exposed real menu identity; kept drop provenance and conditional validation coherent.')
