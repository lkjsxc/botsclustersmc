from pathlib import Path
p=Path('learning/src/control.rs');s=p.read_text()
if 'identity:PolicyId' not in s:
    s=s.replace('cohort:Cohort<Transition>,clocks:', 'identity:PolicyId,cohort:Cohort<Transition>,clocks:')
    s=s.replace('Ok(Self{policy:Arc::new(b.checkpoint.model)', 'let identity=policy_id(&b.checkpoint);\n        Ok(Self{identity,policy:Arc::new(b.checkpoint.model)')
    s=s.replace('pub fn id(&self)->PolicyId{PolicyId{version:self.policy.version,signature:self.policy.fingerprint()}}', 'pub fn id(&self)->PolicyId{self.identity}')
    s=s.replace('self.policy=Arc::new(b.checkpoint.model.clone());', 'self.identity=policy_id(&b.checkpoint);self.policy=Arc::new(b.checkpoint.model.clone());')
    p.write_text(s)
p=Path('app/sensor.rs');s=p.read_text().replace('features:&[f32;23]', 'features:&[f32;87]').replace('features: &[f32;23]', 'features: &[f32;87]').replace('[617..640]', '[617..704]').replace('/31.', '/63.')
p.write_text(s)
p=Path('app/main.rs');s=p.read_text().replace('.join_delay(Duration::from_millis(1500))', '.join_delay(Duration::from_millis(rt.cfg.join_delay_ms))');p.write_text(s)
