"""Reviewed compatibility fixes, never used by normal startup."""
from pathlib import Path
p=Path('tests/live-client.rs');s=p.read_text()
old='Event::Spawn=>{s.ready=true;s.ticks=0;},'
new='Event::Spawn=>{bot.set_client_information(azalea::ClientInformation{view_distance:if s.observer{16}else{3},..Default::default()});s.ready=true;s.ticks=0;},'
if old in s:s=s.replace(old,new)
else:assert new in s
s=s.replace('.left_click(53)', '.left_click(53usize)').replace('.left_click(18)', '.left_click(18usize)')
if 'impl Default for State' not in s:
    needle='#[derive(Component,Clone)]struct State(Arc<Mutex<Check>>);'
    assert needle in s
    s=s.replace(needle,needle+'\nimpl Default for State{fn default()->Self{Self(Arc::new(Mutex::new(Check::new(false))))}}')
p.write_text(s)

p=Path('launcher/main.rs');s=p.read_text()
if 'fn course_summary(' not in s:
    start=s.index('            println!("Academy stage=')
    end=s.index('\n        }\n        println!("Raw status:',start)
    s=s[:start]+'            print!("{}",course_summary(&c,j.get("run_id")?.text()?)?);'+s[end:]
    s+='''
/// Format the actual v2 runtime schema without consulting obsolete v1 fields.
fn course_summary(c:&json::Json,run:&str)->Result<String>{
    if c.get("schema")?.integer()?!=3 || c.get("run_id")?.text()?!=run{return Err("incompatible or stale course status".into());}
    let stage=c.get("stage")?.integer()?;
    if stage>=18{return Err("invalid course stage".into());}
    let mut out=format!("Academy stage={}/18 ({}) phase={}\\n",stage+1,c.get("name")?.text()?,c.get("phase")?.text()?);
    out+=&format!("course_completed={:?}; a historical pass does NOT certify cooperative living\\n",c.get("course_completed")?);
    out+=&format!("cohort_samples={} sealed_actors={} local_untrained_samples={} unfinished_actions={}\\n",c.get("cohort_samples")?.integer()?,c.get("sealed_actors")?.integer()?,c.get("local_untrained_samples")?.integer()?,c.get("unfinished_actions")?.integer()?);
    let scores=c.get("scores")?.array()?;
    if scores.is_empty()||scores.len()>64{return Err("invalid course population".into());}
    for (id,row) in scores.iter().enumerate(){
        if row.get("id")?.integer()?!=id as u64||row.get("task")?.integer()?>=18{return Err("invalid course row identity".into());}
        let number=|name:&str|->Result<f64>{match row.get(name)?{json::Json::Number(s)=>{let n=s.parse::<f64>().map_err(|_|"invalid course ratio")?;if n.is_finite()&&(0.0..=1.0).contains(&n){Ok(n)}else{Err("invalid course ratio".into())}},_=>Err("expected numeric course ratio".into())}};
        out+=&format!("agent {}: task={} state={} attempts={} success_EMA={:.1}% difficulty={:.2} current_exam={}/{}\\n",id,row.get("task")?.integer()?,row.get("state")?.text()?,row.get("attempts")?.integer()?,number("success_rate")?*100.,number("difficulty")?,row.get("exam_successes")?.integer()?,row.get("exam_trials")?.integer()?);
    }
    out+="Promotion requires every actor: 14/16 current trials and 3/4 for EACH earlier skill. EMA is not an exam score.\\n";
    Ok(out)
}
#[cfg(test)]mod course_status_tests{
    use super::*;
    fn data()->String{r#"{"schema":3,"run_id":"run","stage":0,"name":"forward-stop","phase":"training","course_completed":false,"cohort_samples":123,"sealed_actors":1,"local_untrained_samples":4,"unfinished_actions":1,"scores":[{"id":0,"task":0,"state":"practice","attempts":42,"success_rate":0.75,"difficulty":0.4,"exam_trials":0,"exam_successes":0}]}"#.to_string()}
    #[test]fn public_status_formats_actual_v2_fields(){let text=course_summary(&json::parse(&data()).unwrap(),"run").unwrap();assert!(text.contains("stage=1/18"));assert!(text.contains("success_EMA=75.0%"));assert!(text.contains("cohort_samples=123"));assert!(!text.contains("foundations_passed"));}
    #[test]fn rejects_foreign_and_malformed_status(){assert!(course_summary(&json::parse(&data()).unwrap(),"other").is_err());let bad=data().replace("0.75","1.75");assert!(course_summary(&json::parse(&bad).unwrap(),"run").is_err());}
}
'''
    p.write_text(s)
print('Diagnostic types and v2 public status output corrected with regression coverage.')
