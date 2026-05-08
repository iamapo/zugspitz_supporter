# Zugspitz Ultratrail Supporter App Design Notes

## Product direction

The app is for supporters, not primarily for the runner. It should feel like a race-day coordination cockpit: calm, glanceable, low-friction and readable in poor light. The core user need is answering one question quickly:

> When should we be at the next VP, and what context do we need when the runner arrives?

Supporters can choose an expected finish range such as 17-18h. The app derives likely arrival windows at each VP and summarizes:

- completed distance and elevation by the selected VP
- distance and elevation until the next VP
- expected arrival window
- planned stop duration
- practical support note

## Screens

1. **Setup**
   - Supporters choose a finish estimate such as 17-18h.
   - Start time remains editable.
   - The app calculates arrival windows for all VPs from this model.

2. **VP Card**
   - One page per VP.
   - Swipe left/right to move between VPs.
   - Shows arrival window, completed distance/elevation, distance/elevation to this VP, and next-section context.

3. **Check-in**
   - Supporters can enter the actual arrival time at a VP.
   - Future VP windows shift based on the difference between expected and actual arrival.
   - The check-in action should be fast and forgiving: "now", direct time edit, plus/minus stepper.

4. **VP List**
   - Separate tab with all VP windows.
   - Checked-in VPs are marked as done.
   - Future VPs show recalculated arrival ranges after a check-in.

## Visual system

- Background: light dashboard or deep graphite depending on concept.
- Primary action/status: alpine signal green.
- Warning: amber for small delays.
- Critical: red-orange for larger projected misses.
- Surface cards: restrained neutrals with thin borders.
- Typography: numeric values should use tabular figures where available.

## Interaction rules

- Main screen must work for supporters standing outside, possibly at night.
- Time entry and check-in should require only a few taps.
- Time range selection should be obvious before the race starts.
- VP rows should be dense but not cramped.
- Important race numbers should never be hidden behind decorative layout.
