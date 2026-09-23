#!/usr/bin/env python3
"""Summarize retained real status snapshots, without claiming skill mastery."""
import argparse,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('directory',type=Path);args=p.parse_args()
result={}
for phase in ('fresh','resume'):
    file=args.directory/f'{phase}-series.json'
    if not file.exists():continue
    rows=json.loads(file.read_text());a,b=rows[0],rows[-1];seconds=(b['epoch_millis']-a['epoch_millis'])/1000
    result[phase]={'seconds':seconds,'snapshots':len(rows),'agents':a['active_agents'],
      'trained_samples':b['trained_samples']-a['trained_samples'],
      'trained_samples_per_second':(b['trained_samples']-a['trained_samples'])/seconds,
      'decisions_per_second':(b['decision_transitions']-a['decision_transitions'])/seconds,
      'mean_cpu_cores':(b['process_cpu_ns']-a['process_cpu_ns'])/1e9/seconds,
      'available_processors':b['available_processors'],
      'maximum_sampled_heap_mib':max(x['heap_used_mib'] for x in rows),
      'minimum_ticking_agents':min(x['ticking_agents'] for x in rows),
      'minimum_progressed_agents':min(x['progressed_agents_since_status'] for x in rows),
      'retired_agents':b['retired_agents'],'inference_failed':b['inference_failed'],
      'stale_samples':b['learner_stale_samples'],'rejected_samples':b['learner_rejected_samples'],
      'maximum_task':b['course_max_task'],'passed_individual_exams':b['course_passed_exams']}
file=args.directory/'inference-series.json'
if file.exists():
    rows=json.loads(file.read_text());a,b=rows[0],rows[-1];seconds=(b['epoch_millis']-a['epoch_millis'])/1000
    result['inference']={'seconds':seconds,'snapshots':len(rows),'agents':a['active_agents'],
      'decisions_per_second':(b['decision_transitions']-a['decision_transitions'])/seconds,
      'mean_cpu_cores':(b['process_cpu_ns']-a['process_cpu_ns'])/1e9/seconds,
      'available_processors':b['available_processors'],
      'maximum_sampled_heap_mib':max(x['heap_used_mib'] for x in rows),
      'minimum_ticking_agents':min(x['ticking_agents'] for x in rows),
      'minimum_progressed_agents':min(x['progressed_agents_since_status'] for x in rows),
      'retired_agents':b['retired_agents'],'inference_failed':b['inference_failed'],
      'new_trained_samples':b['trained_samples']-a['trained_samples']}
print(json.dumps(result,indent=2))
