package org.botsclustersmc.training;

import org.botsclustersmc.core.*;
import org.botsclustersmc.plugin.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.block.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Training-only entry point; never package this class into the deployment plugin. */
public final class TrainingPlugin extends RuntimePlugin {
    private final LessonOutcomes outcomes=new LessonOutcomes();
    private final CraftingOutcomes craftingOutcomes=new CraftingOutcomes();
    private StartupCoverage startupCoverage;
    private Course course;private Learner learner;private Adam restoredOptimizer;private int count,islandSize;
    private final Map<Long,ArenaLayout> arenas=new ConcurrentHashMap<>();private final AtomicInteger prepared=new AtomicInteger(),preparing=new AtomicInteger(),nextArena=new AtomicInteger();
    private final LongAdder buffered=new LongAdder(),episodesEnded=new LongAdder(),examTransitions=new LongAdder();
    private final Object saveLock=new Object();private volatile boolean closing;private long savedUpdate=-1,lastSave;
    @Override public boolean training(){return true;}
    @Override protected Policy initialPolicy()throws Exception{
        if(!Boolean.getBoolean("bcmc.training")||!Files.isRegularFile(Path.of(".botsclustersmc-training"),LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Training plugin only runs in a launcher-owned isolated server (-Dbcmc.training=true).");
        count=bounded("count",128,1,maximum);Path checkpoint=getDataFolder().toPath().resolve("training.bcmc");
        if(Files.exists(checkpoint,LinkOption.NOFOLLOW_LINKS)){
            TrainingState state=TrainingState.read(checkpoint);course=Course.decode(state.course(),count);restoredOptimizer=state.optimizer();startupCoverage=new StartupCoverage(count,true);getLogger().info("Restored exact optimizer/model: updates="+state.policy().updates()+", samples="+state.policy().samples()+", optimizer-step="+restoredOptimizer.step());return state.policy();
        }
        if(Files.exists(getDataFolder().toPath().resolve("policy.bcmc"),LinkOption.NOFOLLOW_LINKS))
            throw new IllegalStateException("Missing training.bcmc in a directory containing a policy; restore a complete stopped backup or use a fresh Academy.");
        course=new Course(count,seed);restoredOptimizer=new Adam();startupCoverage=new StartupCoverage(count,false);return Policy.initialize(seed);
    }
    @Override protected void initialize(){
        RuntimeBudget budget=RuntimeBudget.automatic(Runtime.getRuntime().availableProcessors());
        islandSize=ArenaLayout.islandSize(count,bounded("region-threads",budget.regionThreads(),1,128));
        learner=new Learner(policy,restoredOptimizer,bounded("learner-threads",budget.learnerThreads(),1,128),bounded("rollout-queue",Math.max(64,count*2),8,16384),bounded("batch-samples",512,32,65536),bounded("max-policy-lag",Math.max(64,count/4),1,10000),p->policy=p,this::fail);
        io.scheduleAtFixedRate(()->{try{coordinate();}catch(Throwable e){fail(e);}},0,100,TimeUnit.MILLISECONDS);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,t->{
            if(failed.get()!=null||closing)return;World world=Bukkit.getWorld("world");if(world==null){fail(new IllegalStateException("owned world missing"));return;}
            for(int i=0;i<4&&preparing.get()<16;i++){
                int actor=nextArena.getAndIncrement();if(actor>=count)return;ArenaLayout a=ArenaLayout.forActor(actor,count,islandSize);preparing.incrementAndGet();
                Location at=new Location(world,a.x()+7.5,65,a.z()+3.5);
                LoadedChunks.use(this,at,chunk->{try{chunk.addPluginChunkTicket(this);TrainingEnvironment.build(world,a);arenas.put((long)actor,a);
                        requestSpawn(actor,at,new Goal(Task.FORWARD_STOP,at.getX(),65,at.getZ()+3,0,.2,600));prepared.incrementAndGet();
                    }catch(Throwable e){fail(e);}finally{preparing.decrementAndGet();}
                },error->{preparing.decrementAndGet();fail(error);});
            }
        },1,1);
    }
    @Override protected void spawned(Npc npc){npc.context=new TrainingEnvironment.Session(Objects.requireNonNull(arenas.get(npc.id)));npc.resetting=false;npc.ready=false;ready(npc);}
    @Override public boolean ready(Npc npc){
        TrainingEnvironment.Session session=(TrainingEnvironment.Session)npc.context;
        if(session.lesson!=null)return true;
        if(course.needsExam(npc.id)){Policy snapshot=policy;course.beginExam(npc.id,snapshot.updates());session.examPolicy=snapshot;}
        Course.Lesson lesson=course.issue(npc.id);if(lesson==null)return false;
        startupCoverage.record(npc.id,course.stage(npc.id),lesson);
        session.lesson=lesson;TrainingEnvironment.reset(this,npc,session,lesson);return false;
    }
    @Override public Policy policyFor(Npc npc){TrainingEnvironment.Session s=(TrainingEnvironment.Session)npc.context;return s.lesson!=null&&s.lesson.kind()==Course.Kind.EXAM?Objects.requireNonNull(s.examPolicy,"missing frozen actor policy"):policy;}
    @Override public boolean greedy(Npc npc){return false;}
    @Override public boolean canPickup(Npc npc,String token){return npc.token().equals(token);}
    @Override public boolean pickupEnabled(Npc npc){return true;}
    @Override public boolean canChange(Npc npc,Block b){
        ArenaLayout a=arenas.get(npc.id);return a!=null&&b.getWorld()==npc.anchor.getWorld()&&b.getY()>=65&&b.getY()<70&&a.contains(b.getX()+.5,b.getY(),b.getZ()+.5)&&WorldActions.owned(b.getLocation());
    }
    private void flush(Npc npc,TrainingEnvironment.Session session){
        if(session.fragment.isEmpty())return;int n=session.fragment.size();
        learner.offer(new Trajectory(npc.id,session.lesson.serial(),session.sequence++,session.fragment));session.fragment.clear();buffered.add(-n);
    }
    @Override public boolean observed(Npc npc,Npc.Applied previous,Frame next){
        TrainingEnvironment.Session s=(TrainingEnvironment.Session)npc.context;if(s.lesson==null)throw new IllegalStateException("missing lesson");
        int ticks=Math.toIntExact(next.tick()-previous.frame().tick());
        course.recordEffort(npc.id,s.lesson.serial(),ticks);
        s.behaviorPolicies.observe(previous.result().policyVersion());
        boolean success=TrainingEnvironment.success(npc,s,previous,next);
        boolean terminal=success||next.tick()-npc.episodeStart>=npc.goal.horizon()||!s.arena.contains(next.x(),next.y(),next.z());
        double potential=TrainingEnvironment.potential(npc,s);float reward=(float)((success?3:terminal?-.3:0)-.0005*ticks/4.0+VTrace.discount(ticks,terminal)*(terminal?0:potential)-s.potential);s.potential=potential;
        if(npc.goal.task()==Task.AIM_HOLD){
            double angular=Math.max(Math.abs(Sensors.angle(next.yaw()-previous.frame().yaw())),Math.abs(next.pitch()-previous.frame().pitch()))/ticks;
            reward+=(float)AimPractice.controlReward(next.yawError(),next.pitchError(),angular,ticks);
        }
        if(HarvestPractice.applies(npc.goal.task()))reward+=(float)TrainingEnvironment.harvestReward(npc,s,next,ticks);
        if(s.lesson.kind()!=Course.Kind.EXAM){
            s.fragment.add(new Transition(previous.frame().observation(),previous.frame().mask(),previous.result().actions(),previous.result().logProbability(),previous.result().policyVersion(),reward,ticks,next.observation(),next.mask(),terminal));buffered.increment();
            if(s.fragment.size()>=32||terminal)flush(npc,s);
        }else{
            if(previous.result().policyVersion()!=course.examVersion(npc.id))throw new IllegalStateException("exam policy changed");examTransitions.increment();
        }
        if(!terminal)return true;
        if(s.lesson.kind()==Course.Kind.PRACTICE&&s.craftingMissing>=0)
            craftingOutcomes.record(s.lesson.task(),s.craftingMissing,success);
        course.finish(npc.id,s.lesson.serial(),success);outcomes.record(s.lesson.task(),s.lesson.kind(),success,s.behaviorPolicies.snapshot());episodesEnded.increment();s.lesson=null;if(course.examVersion(npc.id)<0)s.examPolicy=null;npc.discardPending();return false;
    }
    @Override public void interrupted(Npc npc){
        if(npc.context instanceof TrainingEnvironment.Session s&&s.lesson!=null){if(s.lesson.kind()!=Course.Kind.EXAM)flush(npc,s);course.abandon(npc.id);s.lesson=null;s.examPolicy=null;}
    }
    private void coordinate()throws Exception{
        if(closing||failed.get()!=null)return;
        long now=System.nanoTime();if(policy.updates()!=savedUpdate&&(now-lastSave)>TimeUnit.SECONDS.toNanos(30)){save();lastSave=now;}
    }
    private void save()throws Exception{
        synchronized(saveLock){TrainingState state=learner.snapshot(course.encode());state.write(getDataFolder().toPath().resolve("training.bcmc"));savedUpdate=state.policy().updates();}
    }
    @Override public String observerProgress(){
        Course.Metrics m=course.metrics();
        return String.format(Locale.ROOT,"Stages %s | completion EMA %.1f%% | full-probe EMA %.1f%% | frozen exams %d/%d passed | completed actors %d. Practice is not certification.",Arrays.toString(course.population()),100*m.practiceMean(),100*m.probeMean(),course.passedExams(),course.exams(),course.completedAgents());
    }
    @Override public String observerAgent(long id){
        Course.Progress p=course.progress(id);
        return String.format(Locale.ROOT,"stage %d | training episodes %d (completion EMA %.1f%%) | full probes %d (EMA %.1f%%) | %s",p.stage(),p.practiceEpisodes(),100*p.practiceSuccess(),p.probes(),100*p.probeSuccess(),p.exam()?"frozen exam "+p.examCases()+"/"+(16+4*p.stage())+" policy "+p.examPolicy():practiceLabel(id));
    }
    @Override public String observerHud(long id){Course.Progress p=course.progress(id);return String.format(Locale.ROOT,"stage %d | probe %.0f%% | %s",p.stage(),100*p.probeSuccess(),p.exam()?"EXAM "+p.examCases()+"/"+(16+4*p.stage()):practiceLabel(id));}
    private String practiceLabel(long id) {
        Course.Lesson lesson=course.currentLesson(id);
        if(lesson==null)return "preparing";
        if(lesson.kind()==Course.Kind.PROBE)return "full-difficulty probe";
        return StationPractice.operation(lesson)?"practice: complete at open station":"practice";
    }
    @Override public double observerRank(long id){Course.Progress p=course.progress(id);return p.stage()+p.probeSuccess()*.5;}
    @Override protected Map<String,Object> extraStatus(){
        if(learner==null)return Map.of();Map<String,Object> s=new LinkedHashMap<>();
        Course.Metrics m=course.metrics();s.put("practice_success_ema",m.practiceMean());s.put("probe_success_ema",m.probeMean());s.put("best_probe_success_ema",m.bestProbe());s.put("exam_ready_agents",m.ready());s.put("prepared_arenas",prepared.get());s.put("island_size",islandSize);s.put("islands",(count+islandSize-1)/islandSize);s.put("course_task",course.task());s.put("course_max_task",course.maximumTask());s.put("course_task_population",Arrays.toString(course.population()));s.put("course_exam_agents",course.examAgents());s.put("course_completed_agents",course.completedAgents());s.put("course_regressions",course.regressions());s.put("course_running",course.running());s.put("course_episodes",course.episodes());s.put("course_successes",course.successes());
        Course.StageMetrics[] stages=course.stageMetrics();
        s.put("cohort_training_ema",Arrays.toString(Arrays.stream(stages).mapToDouble(Course.StageMetrics::trainingEma).toArray()));
        s.put("cohort_probe_ema",Arrays.toString(Arrays.stream(stages).mapToDouble(Course.StageMetrics::probeEma).toArray()));
        s.put("cohort_training_episodes",Arrays.toString(Arrays.stream(stages).mapToLong(Course.StageMetrics::trainingEpisodes).toArray()));
        s.put("cohort_probes",Arrays.toString(Arrays.stream(stages).mapToLong(Course.StageMetrics::probes).toArray()));
        s.put("historical_certified_actors",Arrays.toString(Arrays.stream(stages).mapToInt(Course.StageMetrics::historicalCertificates).toArray()));
        LessonOutcomes.Totals[] done=outcomes.snapshot();
        s.put("training_trials_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::trainingTrials).toArray()));
        s.put("training_successes_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::trainingSuccesses).toArray()));
        s.put("probe_trials_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::probeTrials).toArray()));
        s.put("probe_successes_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::probeSuccesses).toArray()));
        ProbePolicies.Totals[] policyUse=Arrays.stream(done).map(LessonOutcomes.Totals::policyUse).toArray(ProbePolicies.Totals[]::new);
        s.put("probe_policy_scope","completed-probes-applied-behavior-versions-this-process");
        s.put("probe_single_policy_trials",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::singleTrials).toArray()));
        s.put("probe_single_policy_successes",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::singleSuccesses).toArray()));
        s.put("probe_mixed_policy_trials",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::mixedTrials).toArray()));
        s.put("probe_mixed_policy_successes",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::mixedSuccesses).toArray()));
        s.put("probe_behavior_decisions",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::decisions).toArray()));
        s.put("probe_policy_changes",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::policyChanges).toArray()));
        s.put("probe_maximum_policy_span",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::maximumVersionSpan).toArray()));
        s.put("probe_last_minimum_policy",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::lastMinimum).toArray()));
        s.put("probe_last_maximum_policy",Arrays.toString(Arrays.stream(policyUse).mapToLong(ProbePolicies.Totals::lastMaximum).toArray()));
        s.put("exam_trials_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::examTrials).toArray()));
        s.put("exam_successes_this_process",Arrays.toString(Arrays.stream(done).mapToLong(LessonOutcomes.Totals::examSuccesses).toArray()));
        s.put("course_exams",course.exams());s.put("course_passed_exams",course.passedExams());s.put("course_completed",course.completed());s.put("course_abandoned",course.abandoned());s.put("learner_state",learner.state());s.put("learner_queue",learner.queued());
        s.put("learner_offered_samples",learner.offered.sum());s.put("learner_rejected_samples",learner.rejected.sum());s.put("learner_stale_samples",learner.stale.sum());s.put("actor_buffered_samples",buffered.sum());s.put("exam_transitions",examTransitions.sum());
        Course.Effort effort=course.effort();
        s.put("review_allocation","observed-ticks");
        s.put("foundation_ticks_this_process",effort.foundationTicks());
        s.put("frontier_ticks_this_process",effort.frontierTicks());
        s.put("review_ticks_this_process",effort.reviewTicks());
        s.put("exam_ticks_this_process",effort.examTicks());
        s.put("task_balance","bounded-batch-loss");
        s.put("learned_task_samples_this_process",Arrays.toString(learner.taskSamples()));
        TaskBalance last=learner.updateBalance();
        s.put("update_task_samples",Arrays.toString(last==null?new int[TaskBalance.TASKS+1]:last.counts()));
        s.put("update_task_weights",Arrays.toString(last==null?new double[TaskBalance.TASKS+1]:last.weights()));
        CraftingOutcomes.Totals crafting=craftingOutcomes.snapshot();
        s.put("crafting_practice_trials_by_task_and_missing",Arrays.toString(crafting.trials()));
        s.put("crafting_practice_successes_by_task_and_missing",Arrays.toString(crafting.successes()));
        s.put("crafting_practice_bucket_width",CraftingOutcomes.BUCKETS);
        s.put("station_success","task-completion");
        if(startupCoverage!=null)s.putAll(startupCoverage.status());
        ActivationHealth.Measurement activation=learner.activationHealth();
        if(activation!=null)s.putAll(activation.status());
        s.put("learner_algorithm","vtrace-guarded-adam");s.put("aim_curriculum","progressive-settling");s.put("harvest_curriculum","sustained-contact-cost");s.put("station_curriculum","completion-preserving-resets");s.put("update_samples",learner.updateSamples);s.put("update_learning_rate",learner.learningRate);s.put("update_mean_policy_kl",learner.meanPolicyKl);s.put("update_max_policy_kl",learner.maxPolicyKl);s.put("update_backtracks",learner.guardBacktracks.sum());s.put("update_rejected_samples",learner.guardRejectedSamples.sum());s.put("batch_wait_ns",learner.batchWaitNanos.sum());s.put("learner_compute_ns",learner.computeNanos.sum());s.put("gradient_norm",learner.gradientNorm);s.put("value_loss",learner.valueLoss);s.put("entropy",learner.entropy);s.put("importance_mean",learner.importance);return s;
    }
    @Override protected void closing()throws Exception{
        closing=true;if(learner==null)return;learner.close();if(!learner.awaitTermination(30000))throw new IllegalStateException("Learner has not drained; last complete checkpoint retained");
        if(failed.get()==null){save();writeStatus();getLogger().info("Saved canonical training.bcmc: updates="+policy.updates()+", samples="+policy.samples()+", buffered-untrained="+buffered.sum()+", unfinished-episodes="+course.running());}
    }
    @EventHandler public void joined(PlayerJoinEvent event){event.getPlayer().getScheduler().run(this,t->{event.getPlayer().setGameMode(GameMode.SPECTATOR);event.getPlayer().setViewDistance(12);event.getPlayer().sendMessage("Training observer: /bots status | /bots watch <id>. NPCs are not logged-in players.");},null);}
    @EventHandler public void blockBreak(BlockBreakEvent event){event.setCancelled(true);}
    @EventHandler public void blockPlace(BlockPlaceEvent event){event.setCancelled(true);}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        return super.onCommand(sender,command,label,args);
    }
}
