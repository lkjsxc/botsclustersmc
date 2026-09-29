#!/usr/bin/env python3
"""Finite, predeclared warm slot-projection comparison. Never operate on the live Academy."""
from concurrent.futures import ThreadPoolExecutor
import re, time, traceback
from click_slots_support import *


def git(root, *args):
    return subprocess.check_output(['git', *args], cwd=root, text=True).strip()


class Study:
    def __init__(self):
        self.source = git(ROOT, 'rev-parse', 'HEAD')
        self.roots = dict(control=CONTROL, candidate=ROOT)
        self.runtime = {arm: root/'dist/training.jar' for arm, root in self.roots.items()}
        self.checkpoint = dict(control=OLD_INPUT/'protected-training.bcmc', candidate=INPUT/'protected-training.bcmc')
        self.policy = dict(control=OLD_INPUT/'protected-policy.bcmc', candidate=INPUT/'protected-policy.bcmc')
        self.files = [*self.runtime.values(), *self.checkpoint.values(), *self.policy.values(),
            OLD_INPUT/'source-training.bcmc', OLD_INPUT/'source-runtime.jar', INPUT/'expansion.json']
        self.identities = {str(path): sha(path) for path in self.files}
        require(sha(self.runtime['control']) == CONTROL_RUNTIME, 'Changed reference runtime')
        require(sha(self.checkpoint['control']) == CONTROL_INPUT, 'Changed reference checkpoint')
        require(sha(OLD_INPUT/'source-training.bcmc') == '495baa13beb07860d8448f80438ad79f876c3a2a2aaca5de48a575b16edf0855', 'Changed original source')
        self.guard()

    def guard(self):
        require(git(ROOT, 'rev-parse', 'HEAD') == self.source and not git(ROOT, 'status', '--porcelain'), 'Changed candidate source')
        require(git(CONTROL, 'rev-parse', 'HEAD') == CONTROL_SOURCE and not git(CONTROL, 'status', '--porcelain'), 'Changed reference source')
        for path in self.files:
            require(sha(path) == self.identities[str(path)], 'Changed fixed input/runtime: '+str(path))

    def identity(self, arm, checkpoint, out, label, initial):
        runtime = self.runtime[arm]
        if arm == 'control':
            command = ['java', '-Xmx256m', '-cp', runtime, CONTROL/'tests/studies/FocusAudit.java',
                OLD_INPUT, checkpoint, 'initial' if initial else 'continued', 'protected']
        else:
            command = ['java', '-Xmx256m', '-cp', runtime,
                ROOT/'tests/transfer/org/botsclustersmc/diagnostic/SlotExpansion.java',
                'initial' if initial else 'continued', self.runtime['control'], self.checkpoint['control'], checkpoint]
        audit = execute(command, out/(label+'-audit.log'))
        policy = out/(label+'-policy.bcmc')
        text = execute(['java', '-Xmx256m', '-cp', runtime, 'org.botsclustersmc.training.CheckpointTool',
            'export', checkpoint, policy], out/(label+'-export.log'))
        match = re.search(r'updates=(\d+), trained_samples=(\d+)', text)
        require(match is not None, 'Missing native checkpoint counters')
        result = dict(updates=int(match[1]), samples=int(match[2]), checkpoint_sha256=sha(checkpoint),
            policy_sha256=sha(policy), audit=audit.strip())
        require(result['samples'] == BASE if initial else result['samples'] > BASE, 'Incorrect source/continuation counter')
        if initial:
            require(read(policy) == read(self.policy[arm]), 'Initial checkpoint export differs from evaluated policy')
        save(out/(label+'-identity.json'), result)
        return result

    def evaluate(self, phase):
        self.guard(); directory = OUT/('evaluation-'+phase); directory.mkdir()
        if phase == 'baseline':
            models = self.policy
        else:
            target = TARGETS[0] if phase == 'early' else TARGETS[1]
            models = {arm: OUT/arm/str(target)/'stopped-policy.bcmc' for arm in ARMS}
        jobs = [(condition, seed, arm) for condition in CONDITIONS for seed in SEEDS for arm in ARMS]
        def one(index, job):
            condition, seed, arm = job; args = spec(seed, condition)
            memory(); self.guard(); name = f'{condition}-{seed}-{arm}'; output = directory/name
            policy, runtime, port = models[arm], self.runtime[arm], 31060+index
            command = ['python3', 'tests/holdout.py', '--policy', policy, '--runtime', runtime, '--cache', CACHE,
                '--output', output, '--tasks', *map(str, args.tasks), '--cases', args.cases,
                '--seed', seed, '--port', port, '--reset-intervention', condition]
            started = dict(command=list(map(str, command)), epoch=time.time(), policy=sha(policy), runtime=sha(runtime))
            save(directory/(name+'-started.json'), started)
            execute(command, directory/(name+'.log'), timeout=960)
            require(sha(policy) == started['policy'] and sha(runtime) == started['runtime'], 'Changed evaluated input')
            scores = verify_evaluation(output, policy, runtime, seed, condition, port)
            receipt = dict(arm=arm, seed=seed, condition=condition, scores=scores,
                trials=args.cases*len(args.tasks), new_training_samples=0, report_sha256=sha(output/'result.json'),
                metadata_sha256=sha(output/'metadata.json'), policy_sha256=sha(policy), runtime_sha256=sha(runtime))
            save(directory/(name+'-receipt.json'), receipt)
            print('EVALUATED', phase, arm, seed, condition, scores, flush=True)
            return receipt
        receipts = []
        # At most two current jobs; an operational failure does not launch the remaining pairs.
        for start in range(0, len(jobs), 2):
            with ThreadPoolExecutor(max_workers=2) as pool:
                futures = [pool.submit(one, index, jobs[index]) for index in range(start, min(start+2, len(jobs)))]
                receipts.extend(future.result() for future in futures)
            self.guard(); memory()
        vectors(receipts); save(directory/'completed.json', receipts)
        return receipts

    def train(self, arm, target):
        require(arm in ARMS and type(target) is int and target in TARGETS, 'Undeclared training arm/boundary')
        self.guard(); memory(); root = self.roots[arm]
        out = OUT/arm; out.mkdir(exist_ok=True); evidence = out/str(target); evidence.mkdir()
        academy = out/'academy'; data = academy/'server/plugins/BotsClustersMC'; checkpoint = data/'training.bcmc'
        if target == TARGETS[0]:
            data.mkdir(parents=True)
            write(academy/'.botsclustersmc-academy', b'botsclustersmc-owned-training\n')
            write(academy/'.gitignore', b'*\n'); write(checkpoint, read(self.checkpoint[arm]))
            require(sha(checkpoint) == sha(self.checkpoint[arm]), 'Initial checkpoint differs')
        else:
            require(target == TARGETS[1] and (out/str(TARGETS[0])/'completed.json').is_file(), 'Missing exact early boundary')
            require(sha(checkpoint) == sha(out/str(TARGETS[0])/'stopped-training.bcmc'), 'Resume checkpoint substituted')
        initial = self.identity(arm, checkpoint, evidence, 'start', target == TARGETS[0])
        env = dict(ENV, ACADEMY=str(academy), BOTS='512', HEAP_GB='2', REGION_THREADS='2',
            INFERENCE_THREADS='1', LEARNER_THREADS='1', SEED='7', PORT='31051' if arm == 'control' else '31052',
            BIND_ADDRESS='127.0.0.1', ONLINE_MODE='true', BCMC_SERVER_CACHE=str(CACHE))
        threshold = BASE+target; epoch = int(time.time()*1000); started = time.monotonic()
        first = last = None; last_print = 0
        with (evidence/'training.log').open('x') as log, (evidence/'status.jsonl').open('x') as history:
            process = subprocess.Popen(['./start.sh'], cwd=root, env=env, stdin=subprocess.DEVNULL,
                stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            try:
                while True:
                    require(time.monotonic()-started <= 1800, '30-minute training-segment operational cap')
                    memory(); require(process.poll() is None, 'Learner exited before sample boundary')
                    if (data/'status.json').is_file():
                        status = load(data/'status.json')
                        samples = training_samples(status, initial['samples'], epoch, time.time()*1000, last)
                        if samples is not None:
                            stamp, agents = status['epoch_millis'], status['active_agents']
                            require(samples <= threshold+OVERSHOOT, 'Live accepted sample budget exceeded')
                            if last is None or stamp != last['epoch_millis']:
                                if last is not None:
                                    require(samples >= last['trained_samples'], 'Training counter went backwards')
                                last = status; history.write(json.dumps(status, allow_nan=False)+'\n'); history.flush()
                                if first is None and agents == 512:
                                    first = status; save(evidence/'first-active.json', first)
                                if time.monotonic()-last_print >= 30:
                                    print('TRAIN', arm, target, samples-BASE, flush=True); last_print = time.monotonic()
                            if samples >= threshold:
                                break
                    time.sleep(2)
                execute(['./stop.sh'], evidence/'stop.log', root, env, 90); process.wait(timeout=120)
                require(process.returncode == 0, 'Unclean training shutdown')
            finally:
                if process.poll() is None:
                    try:
                        execute(['./stop.sh'], evidence/'failure-stop.log', root, env, 90); process.wait(timeout=120)
                    except Exception as error:
                        save(evidence/'cleanup-error.json', dict(error=str(error)))
                        if process.poll() is None:
                            os.killpg(process.pid, signal.SIGTERM); process.wait(timeout=45)
                if checkpoint.is_file():
                    write(evidence/'stopped-training.bcmc', read(checkpoint))
        stopped = self.identity(arm, evidence/'stopped-training.bcmc', evidence, 'stopped', False)
        self.guard()
        budget(stopped['samples'], target)
        require(first is not None and last is not None and task_counts(last['learned_task_samples_this_process'])[11] > 0,
            'Missing actor coverage or accepted task-11 samples')
        save(evidence/'last-observed.json', last)
        receipt = dict(arm=arm, target=target, additional_samples=stopped['samples']-BASE, start=initial,
            stopped=stopped, elapsed_seconds=time.monotonic()-started, source=self.source)
        save(evidence/'completed.json', receipt); print('TRAIN COMPLETE', arm, target, receipt['additional_samples'], flush=True)

    def run(self):
        memory(); OUT.mkdir()
        save(OUT/'declaration.json', dict(source=self.source, control_source=CONTROL_SOURCE, identities=self.identities,
            seeds=SEEDS, tasks=TASKS, ordinary_cases=64, assisted_cases=32, conditions=CONDITIONS,
            budgets=TARGETS, overshoot=OVERSHOOT, epoch=time.time(), runner_sha256=sha(Path(__file__)),
            question='Separate click-conditioned projection with copied weights and moments; not task-ID protection versus no protection.',
            input_qualification='relative retention, not absolute mastery', deployment=False))
        try:
            for arm in ARMS:
                self.identity(arm, self.checkpoint[arm], OUT, arm+'-input', True)
            baseline = self.evaluate('baseline'); phases = [('baseline', baseline)]
            decision = gate(baseline, baseline); save(OUT/'baseline-gate.json', decision)
            if decision['retained']:
                for phase, target in zip(('early', 'final'), TARGETS):
                    for arm in ARMS:
                        self.train(arm, target)
                    rows = self.evaluate(phase); phases.append((phase, rows))
                    decision = gate(baseline, rows); save(OUT/(phase+'-gate.json'), decision)
                    if not decision['retained']:
                        break
            phase = phases[-1][0]
            save(OUT/'outcome.json', dict(stage=phase, full_budget_completed=phase == 'final',
                retained=decision['retained'], acquisition=decision['acquisition'], assisted_gain=decision['assisted_gain'],
                useful_pilot=phase == 'final' and decision['retained'] and decision['acquisition'],
                reports=sum(len(rows) for _, rows in phases), unique_trials=sum(row['trials'] for _, rows in phases for row in rows),
                deployment=False, source=self.source, finished_epoch=time.time()))
            print('STUDY OUTCOME', load(OUT/'outcome.json'), flush=True)
        except Exception as error:
            save(OUT/'failure.json', dict(error=str(error), traceback=traceback.format_exc(), epoch=time.time()))
            raise


def qualify():
    """Run offline boundaries before constructing any study or launching Minecraft."""
    import unittest
    suite = unittest.defaultTestLoader.discover(str(ROOT/'tests/studies'), pattern='test_click_slots.py')
    require(suite.countTestCases() >= 24, 'Incomplete offline qualification suite')
    require(unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful(), 'Offline qualification failed')


def main(argv=None):
    import argparse
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--run', action='store_true', help='Run the fixed, isolated, finite research protocol after offline qualification.')
    args = parser.parse_args(argv)
    if not args.run:
        parser.error('Explicit --run is required. No Minecraft study was started.')
    qualify()
    Study().run()


if __name__ == '__main__':
    main()
