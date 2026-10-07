"""Strict tool-use trace checks; observed input masses are not contact probabilities."""
import math

SCOPE = 'tool-use-pre-action-not-contact'


def require(ok, label):
    if not ok:
        raise AssertionError('Tool diagnostics: ' + label)


def verify(trace, observations):
    require(type(trace) is dict and trace.get('scope') == SCOPE, 'scope')
    require(type(observations) is int and 1 <= observations <= 2**31-1, 'observations')

    def count(key, maximum=observations):
        v = trace.get(key)
        require(type(v) is int and 0 <= v <= maximum, key)
        return v

    def vector(key, size, maximum=observations, integral=True):
        v = trace.get(key)
        require(type(v) is list and len(v) == size, key)
        for x in v:
            require((type(x) is int if integral else type(x) in (int, float))
                    and 0 <= x <= maximum and math.isfinite(x), key)
        return v

    def mass(key, maximum):
        v = trace.get(key)
        require(type(v) in (int, float) and 0 <= v <= maximum + 1e-7 and math.isfinite(v), key)
        return v

    require(count('transitions') == observations, 'transition denominator')
    first, last = count('initial_state', 3), count('final_state', 3)
    visits = vector('state_visits', 4)
    flow = vector('state_transitions', 16)
    require(sum(visits) == observations, 'state denominator')
    for i in range(4):
        outgoing = sum(flow[i*4:i*4+4])
        incoming = sum(flow[i::4])
        require(outgoing == visits[i], 'transition rows')
        require(outgoing - incoming == int(first == i) - int(last == i), 'flow conservation')
    reachable = {first}
    for _ in range(4):
        reachable |= {j for i in tuple(reachable) for j in range(4) if flow[i*4+j]}
    require(all(i in reachable or visits[i] == 0 for i in range(4)), 'disconnected flow')
    locations = vector('visible_pick_location_states', 5)
    visible = observations - locations[4]
    require(max(locations[:4]) <= visible <= sum(locations[:4]), 'visible tool union')
    require(locations[0] >= visits[1] + visits[3], 'selected tool must be in hotbar')
    closed, opened = sum(visits[:2]), sum(visits[2:])
    hotbar = count('closed_hotbar_pick_states', min(closed, locations[0]))
    require(visits[1] <= hotbar, 'closed selected tool')
    casc = vector('closed_cascade_selections', 4, closed)
    masses = vector('closed_cascade_probability_sums', 4, closed + 1e-7, False)
    for values in (casc, masses):
        require(all(values[i+1] <= values[i] + (0 if values is casc else 1e-7)
                    for i in range(3)), 'nested closed input sets')
        require(values[2] <= hotbar + (0 if values is casc else 1e-7), 'tool selection opportunity')
    gui = vector('open_gui_selections', 6, opened)
    gui_mass = vector('open_gui_probability_sums', 6, opened + 1e-7, False)
    require(sum(gui) == opened and abs(sum(gui_mass)-opened) <= 1e-7, 'open operation denominator')
    require(gui[4] == 0 and gui_mass[4] == 0, 'already open has no open action')
    count('open_selected_pick_close_selections', min(visits[3], gui[5]))
    close_mass = mass('open_selected_pick_close_probability_sum', visits[3])
    require(close_mass <= gui_mass[5] + 1e-7, 'close tool mass subset')
    return trace
