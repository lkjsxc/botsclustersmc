use std::{env,fs,net::Ipv4Addr,path::Path};
use super::install::{self,Result};
pub fn number(name:&str,default:u64,min:u64,max:u64)->Result<u64>{let x=env::var(name).unwrap_or(default.to_string()).parse::<u64>().map_err(|_|format!("{name} must be an integer"))?;if x<min||x>max{return Err(format!("{name} must be in {min}..={max}"));}Ok(x)}
#[derive(Debug)]pub struct Config{pub bind:Ipv4Addr,pub port:u16,pub heap:u64,pub threads:u64,pub bots:u64,pub view:u64,pub sim:u64,pub disk:u64,pub seconds:u64,pub mode:String,pub curriculum:bool}
impl Config{
    pub fn load()->Result<Self>{
        let bind=env::var("BIND_ADDRESS").unwrap_or("0.0.0.0".into()).parse::<Ipv4Addr>().map_err(|_|"BIND_ADDRESS must be an IPv4 address")?;
        if !bind.is_loopback()&&env::var("OFFLINE_ACCESS_ACK").as_deref()!=Ok("true"){return Err("Offline mode has NO identity authentication. Set OFFLINE_ACCESS_ACK=true only after restricting the configured server port to a trusted VPN/LAN/firewall.".into());}
        let mode=env::var("BCMC_MODE").unwrap_or("train".into());if !["train","eval","random"].contains(&mode.as_str()){return Err("BCMC_MODE must be train/eval/random".into());}
        Ok(Self{port:number("SERVER_PORT",25565,1,65535)? as u16,curriculum:env::var("BCMC_CURRICULUM").as_deref()==Ok("true"),bind,heap:number("JAVA_HEAP_GB",6,2,8)?,threads:number("FOLIA_THREADS",6,1,10)?,bots:number("BOTS",64,1,64)?,view:number("VIEW_DISTANCE",3,3,8)?,sim:number("SIMULATION_DISTANCE",3,3,8)?,disk:number("MIN_FREE_GB",10,2,100)?,seconds:number("RUN_SECONDS",0,0,864000)?,mode})
    }
    pub fn local_address(&self)->String{format!("{}:{}",if self.bind.is_unspecified(){Ipv4Addr::LOCALHOST}else{self.bind},self.port)}
    pub fn write(&self,root:&Path)->Result<()>{
        if !self.curriculum || !root.join(".botsclustersmc-academy-v2").is_file(){return Err("Only the isolated Academy is supported; use ./start.sh".into());}
        let server=root.join("server");let config=server.join("config");fs::create_dir_all(&config).map_err(|e|e.to_string())?;
        if env::var("EULA").as_deref()!=Ok("true"){
            let existing=fs::read_to_string(server.join("eula.txt")).unwrap_or_default();
            if !existing.lines().any(|l|l.trim()=="eula=true"){return Err("Read https://aka.ms/MinecraftEULA and launch with EULA=true to accept it yourself. No automatic acceptance.".into());}
        }
        install::atomic(&server.join("eula.txt"),b"# Accepted explicitly by this server operator.\neula=true\n")?;
        let seed=number("SEED",71683,0,u64::MAX)?;
        let size=number("WORLD_DIAMETER",512,512,8192)?;
        let mut properties=format!("# Managed by botsclustersmc: edit .env, not this generated file.\nserver-port={}\nserver-ip={}\nonline-mode=false\nenforce-secure-profile=false\nprevent-proxy-connections=false\nwhite-list=false\nenforce-whitelist=false\nmax-players={}\ngamemode=survival\nforce-gamemode=false\ndifficulty=easy\nhardcore=false\npvp=false\nallow-flight=false\nallow-nether=false\nspawn-protection=0\nview-distance={}\nsimulation-distance={}\nmax-world-size={}\nlevel-name=world\nlevel-seed={}\nlevel-type=minecraft\\:normal\ngenerate-structures=true\nspawn-monsters=true\nspawn-animals=true\nspawn-npcs=true\nenable-command-block=false\nenable-rcon=false\nenable-status=true\nmanagement-server-enabled=false\npause-when-empty-seconds=-1\nenable-query=false\nenable-jmx-monitoring=false\naccept-transfers=false\nplayer-idle-timeout=0\nrate-limit=0\nnetwork-compression-threshold=256\nentity-broadcast-range-percentage=100\nsync-chunk-writes=true\nuse-native-transport=true\nmotd=botsclustersmc - reinforcement learning research\n",self.port,self.bind,self.bots+16,self.view,self.sim,size/2,seed);
        if self.curriculum {
            if !root.join(".botsclustersmc-academy-v2").is_file(){return Err("academy ownership marker missing; use academy.sh".into());}
            properties=properties.replace("level-name=world\n","level-name=bcmc_academy_v2\n")
                .replace("level-type=minecraft\\:normal","level-type=minecraft\\:flat")
                .replace("generate-structures=true","generate-structures=false")
                .replace("spawn-monsters=true","spawn-monsters=false")
                .replace("spawn-animals=true","spawn-animals=false")
                .replace("spawn-npcs=true","spawn-npcs=false")
                .replace("difficulty=easy","difficulty=peaceful");
            properties.push_str(r#"generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":156},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"lakes":false,"features":false,"structure_overrides":[]}
"#);
        }
        install::atomic(&server.join("server.properties"),properties.as_bytes())?;
        // Only these owned files are regenerated. Worlds and unrelated plugin
        // files are never replaced, wiped, or migrated by the launcher.
        let global=format!("# Managed by botsclustersmc\nthreaded-regions:\n  threads: {}\nchunk-system:\n  io-threads: 1\n  worker-threads: 2\nchunk-loading-basic:\n  player-max-chunk-generate-rate: 8.0\n  player-max-chunk-load-rate: 30.0\n  player-max-chunk-send-rate: 30.0\nchunk-loading-advanced:\n  player-max-concurrent-chunk-generates: 1\n  player-max-concurrent-chunk-loads: 2\nmisc:\n  region-file-cache-size: 128\n",self.threads);
        install::atomic(&config.join("paper-global.yml"),global.as_bytes())?;
        install::atomic(&server.join("bukkit.yml"),b"# Managed by botsclustersmc\nsettings:\n  allow-end: false\n  connection-throttle: -1\nspawn-limits:\n  monsters: 35\n  animals: 8\n  water-animals: 3\n  water-ambient: 8\n  water-underground-creature: 3\n  axolotls: 3\n  ambient: 8\n")?;
        install::atomic(&server.join("spigot.yml"),b"# Managed observer visibility; does not change actor simulation distance.\nworld-settings:\n  default:\n    entity-tracking-range:\n      players: 128\n      animals: 48\n      monsters: 48\n      misc: 32\n      display: 128\n      other: 64\n")?;
        Ok(())
    }
}
