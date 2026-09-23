# Research foundations and scope — 2026-09-23

This is a source-integrated, CPU-based PPO experiment, not a claim of a new
state-of-the-art algorithm. The [learning contract](LEARNING.md) specifies the
actual enabled mechanisms; [validation](VALIDATION.md) states what was measured.
Paper results on other environments do not establish this runtime's performance.

## Foundations actually used

[Proximal Policy Optimization Algorithms](https://arxiv.org/abs/1707.06347)
(Schulman et al., 2017) supplies the clipped policy-gradient objective and the
multiple-minibatch-update baseline. The runtime computes the likelihood of the
actual executed joint action, including the conditional GUI-slot branch. The
whole-batch KL check and transactional retry are additional engineering choices,
not a proof of monotonic improvement or an implementation of TRPO or RePPO.

[High-Dimensional Continuous Control Using Generalized Advantage Estimation](https://arxiv.org/abs/1506.02438)
(Schulman et al., 2015) supplies the advantage-estimation foundation. This runtime
explicitly extends its discount/trace convention to variable observed durations:
both are raised to elapsed server ticks divided by four. Potential shaping uses
the same discount. This convention is tested mathematically, not asserted to be
the empirically optimal convention for every Minecraft action.

[Syllabus: Portable Curricula for Reinforcement Learning Agents, v2](https://arxiv.org/html/2411.11318v2)
(Sullivan et al., 2 August 2025) describes modular curriculum interfaces and
learning-progress methods using fast and slow success estimates. It also reports
that existing methods do not directly transfer to some complex new environments.
The runtime therefore treats adaptation as a separate, replaceable coordinator.
Its success-window difficulty adjustment, forgetting-weighted review and mandatory
coverage are a **custom inspired scheduler**, not a port of Syllabus or an exact
reproduction of one of its benchmark algorithms.

## Deliberate integration decisions

Correct server inventory/cursor observations, reachable task environments and
consistent behavior-policy data are prerequisites for a meaningful algorithm
comparison. They were fixed and exercised before attributing better performance
to a newer optimizer. The 64-actor episode-boundary cohort removes silent stale
fragment loss, but early actors wait for stragglers. More actors do not imply a
proportional learning-speed gain.

The adaptive course uses furnished tools/resources and, in easy training resets,
partial raw-material recipe layouts or opened menus. Full probes and frozen exams
remove the latter assistance. The neural learner still receives privileged state
and goal features. Neither reward-only training nor removal of demonstrations
makes this equivalent to unassisted pixels-only survival.

All-actor retention gates are a conservative acceptance choice, not a theorem
that this schedule maximizes learning. Passing short lifecycle tests is not
passing these exams. Scripted diagnostics are separate executables and are never
used as demonstrations or a fallback controller.

World-model agents, intrinsic exploration objectives, recurrent policies and
alternative policy optimizers remain potential experiments, not secretly enabled
features. A subsequent comparison should hold world/task seeds, compute budgets,
input/reward semantics and evaluation protocol fixed, report wall-clock and sample
efficiency separately, retain failures and use several independent training seeds.
There is no completed old-versus-new Minecraft learning-speed ablation in this
release, and no claim that the current settings are optimal.
