# Progressive aiming practice

Aiming practice varies both initial orientation and required settling length.
Reset-only yaw and pitch perturbations grow with the square of difficulty.
Practice requires `4 + ceil(16 * difficulty^2)` settled ticks. Full-difficulty
practice, every full probe, and every frozen exam still require 20 settled ticks.
Probe/exam orientation generation and random draws are unchanged. No in-episode
controller aims at the target or replaces the neural policy's action.

Aiming also receives a bounded cost for actual angular error and motion, in
addition to the terminal outcome and potential difference. The per-four-tick
cost is `0.04 * min(1, abs(yaw_error)/180) + 0.04 * min(1, abs(pitch_error)/90)
+ 0.002 * min(1, angular_speed/8)`, scaled by actual elapsed ticks. This is an
explicit task objective, not policy-invariant potential shaping.

The measured initial reset-only change did not resolve the exploration failure.
Adding an angular-error cost alone also failed to produce mastery in its observed
window. Full-condition success improved after short-hold practice was introduced.
This sequential experiment is not a randomized ablation attributing the gain to
one isolated factor. The previous unsuccessful observations remain on the device.

The held-out test uses one frozen policy and stochastic actions in new real rooms.
It does not update weights or curriculum state. Software tests separately verify
that probes/exams retain their original initial angles, random state and duration.
These motor-control results do not establish natural-world survival or cooperation.
