//! botsclustersmc: server-side outcomes, randomly initialized shared-policy PPO.
#[path="core/lib.rs"] mod learning;
mod academy;mod settings;mod metrics;mod sensor;mod act;mod agent;mod engine;
use std::{sync::atomic::Ordering,time::Duration};
use azalea::{account::Account,bot::DefaultBotPlugins,DefaultPlugins,swarm::{SwarmBuilder,DefaultSwarmPlugins},app::PluginGroup};
fn main()->Result<(),Box<dyn std::error::Error>>{
    let cfg=settings::Settings::load().map_err(std::io::Error::other)?;
    use azalea::bevy_tasks::{ComputeTaskPool,AsyncComputeTaskPool,IoTaskPool,TaskPoolBuilder};
    ComputeTaskPool::get_or_init(||TaskPoolBuilder::new().num_threads(2).build());
    AsyncComputeTaskPool::get_or_init(||TaskPoolBuilder::new().num_threads(1).build());
    IoTaskPool::get_or_init(||TaskPoolBuilder::new().num_threads(1).build());
    engine::signals();let (rt,learner)=engine::initialize(cfg).map_err(std::io::Error::other)?;
    let executor=tokio::runtime::Builder::new_multi_thread().worker_threads(2).max_blocking_threads(4).enable_all().build()?;
    executor.block_on(async move {
        let local=tokio::task::LocalSet::new();
        local.run_until(async move {
            let mut builder=SwarmBuilder::new_without_plugins()
                .add_plugins((DefaultPlugins,DefaultBotPlugins.build()
                    .disable::<azalea::pathfinder::PathfinderPlugin>()
                    .disable::<azalea::accept_resource_packs::AcceptResourcePacksPlugin>(),DefaultSwarmPlugins))
                .set_handler(agent::handler)
                .join_delay(Duration::from_millis(rt.cfg.join_delay_ms))
                .reconnect_after(Some(Duration::from_secs(20)));
            for id in 0..rt.cfg.bots{let name=format!("{}{:02}",rt.cfg.prefix,id);builder=builder.add_account_with_state(Account::offline(&name),agent::State::new(id,name,rt.cfg.seed));}
            let server=rt.cfg.server.clone();let run=builder.start(server.as_str());
            tokio::pin!(run);
            loop {tokio::select!{
                _=&mut run=>{
                    if !engine::stop_requested(){engine::fail("client ECS exited before a requested shutdown");}
                    break;
                },
                _=tokio::time::sleep(Duration::from_millis(250))=>{if engine::STOP.load(Ordering::Relaxed){break;}}
            }}
        }).await;
    });
    engine::request_stop();let result=learner.join().map_err(|_|std::io::Error::other("learner thread panicked"))?;
    result.map_err(std::io::Error::other)?;Ok(())
}

#[cfg(test)] mod protocol_tests;
