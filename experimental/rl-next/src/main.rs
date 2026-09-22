use bcmc_rl_next::{Result,Rng,math::{Distribution,clipped_objective,normalize_advantages,guarded_update,sampled_kl,TrustConfig},tasks::TASKS};

#[derive(Clone)]
struct Agent {logits:Vec<Vec<f64>>,values:Vec<f64>,steps:u64,rng:Rng}
#[derive(Clone)]
struct Sample {context:usize,action:usize,old_logp:f64,reward:f64,advantage:f64}
struct Report {seed:u64,initial:f64,final_success:f64,retries:usize,updates:usize,samples:usize,max_kl:f64}
fn distribution(agent:&Agent,context:usize)->Result<Distribution>{Distribution::new(&[agent.logits[context].clone()],&[vec![true;4]],None)}
// Synthetic environment reward. The optimizer receives only sampled (state,
// action,reward) tuples; it never receives this function or a target action.
fn reward(context:usize,action:usize)->f64{(action==(context*7+3)%4) as u8 as f64}
fn exact_expected_reward(agent:&Agent)->Result<f64>{let mut sum=0.0;for c in 0..agent.logits.len(){let d=distribution(agent,c)?;
    for (a,p) in d.probabilities()[0].iter().enumerate(){sum+=p*reward(c,a);}}Ok(sum/agent.logits.len() as f64)}
fn benchmark(seed:u64)->Result<Report>{
    let contexts=8;let batch_size=256;let updates=120;
    let mut agent=Agent{logits:vec![vec![0.0;4];contexts],values:vec![0.0;contexts],steps:0,rng:Rng(seed)};
    let initial=exact_expected_reward(&agent)?;let mut retries=0;let mut max_kl:f64=0.0;
    for _ in 0..updates {
        let mut batch=Vec::with_capacity(batch_size);
        for _ in 0..batch_size {
            let c=agent.rng.index(contexts)?;let d=distribution(&agent,c)?;let chosen=d.sample(&mut agent.rng,false)?;
            let r=reward(c,chosen.actions[0]);batch.push(Sample{context:c,action:chosen.actions[0],old_logp:chosen.log_probability,reward:r,advantage:r-agent.values[c]});
        }
        let mut advantages=batch.iter().map(|s|s.advantage).collect::<Vec<_>>();normalize_advantages(&mut advantages)?;
        for (s,a) in batch.iter_mut().zip(advantages){s.advantage=a;}
        let old=batch.iter().map(|s|s.old_logp).collect::<Vec<_>>();
        let cfg=TrustConfig{learning_rate:1.0,maximum_kl:0.03,retries:6,shrink:0.5};
        let report=guarded_update(&mut agent,cfg,|agent,lr|{
            for _ in 0..4 {
                let mut gradient=vec![vec![0.0;4];contexts];let mut value_gradient=vec![0.0;contexts];
                for s in &batch {
                    let d=distribution(agent,s.context)?;let lp=d.log_probability(&[s.action])?;
                    let objective=clipped_objective(s.old_logp,lp,s.advantage,0.2)?;
                    let score=d.score_gradient(&[s.action],objective.d_log_probability)?;let (_,entropy)=d.entropy(true);
                    for a in 0..4{gradient[s.context][a]+=(score[0][a]-0.002*entropy[0][a])/batch_size as f64;}
                    value_gradient[s.context]+=(agent.values[s.context]-s.reward)/batch_size as f64;
                }
                for c in 0..contexts{for a in 0..4{agent.logits[c][a]-=lr*gradient[c][a];}agent.values[c]-=lr*value_gradient[c];}
                agent.steps+=1;
            }
            Ok(())
        },|agent|{let mut new=Vec::with_capacity(batch.len());for s in &batch{new.push(distribution(agent,s.context)?.log_probability(&[s.action])?);}sampled_kl(&old,&new)})?;
        retries+=report.attempts-1;max_kl=max_kl.max(report.final_kl);
    }
    let final_success=exact_expected_reward(&agent)?;
    if final_success<0.90{return Err("synthetic reward-only learning check did not reach 90% expected return");}
    Ok(Report{seed,initial,final_success,retries,updates,samples:updates*batch_size,max_kl})
}
fn run()->Result<()> {
    let arg=std::env::args().nth(1).unwrap_or_else(||"--help".into());
    match arg.as_str(){
        "benchmark"=>{
            let mut reports=Vec::new();for seed in [7,19,42,81,1337]{reports.push(benchmark(seed)?);}
            println!("{{\"kind\":\"synthetic-contextual-bandit-only\",\"minecraft_evidence\":false,\"algorithm\":\"categorical PPO with guarded full-batch KL\",\"contexts\":8,\"actions\":4,\"batch_size\":256,\"epochs\":4,\"results\":[");
            for (i,r) in reports.iter().enumerate(){println!("{{\"seed\":{},\"initial_expected_return\":{:.8},\"final_expected_return\":{:.8},\"updates\":{},\"environment_samples\":{},\"retries\":{},\"max_accepted_sampled_kl\":{:.8}}}{}",r.seed,r.initial,r.final_success,r.updates,r.samples,r.retries,r.max_kl,if i+1==reports.len(){""}else{","});}
            println!("]}}");
        }
        "stages"=>{println!("TASK CONTRACTS ONLY: no Minecraft adapter is included in this command.");
            for (i,t) in TASKS.iter().enumerate(){let s=t.spec();println!("{i:02} {} [{}; {} server ticks]\n   {}",s.name,s.group,s.limit_ticks,s.furnished);}}
        "--help"|"-h"=>println!("bcmc-rl-check: experimental component verification, NOT a Minecraft bot runtime\n\nCommands:\n  benchmark  Run five deterministic reward-only synthetic PPO checks\n  stages     Print advanced task contracts and explicit furnished resources\n\nThis executable does not alter worlds, checkpoints, or v0.3.1 binaries."),
        _=>return Err("unknown command; use --help"),
    } Ok(())
}
fn main(){if let Err(e)=run(){eprintln!("bcmc-rl-check: {e}");std::process::exit(1);}}

#[cfg(test)]
mod tests {
    use super::*;
    #[test] fn reward_only_optimizer_learns_without_target_actions(){let r=benchmark(7).unwrap();assert_eq!(r.initial,0.25);assert!(r.final_success>=0.9);assert!(r.max_kl<=0.03);}
}
