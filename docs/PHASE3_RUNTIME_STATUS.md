# Phase 3 Runtime Status

Candidate tested: `0.4.0-alpha.0`

## Target-machine result

Structured telemetry started and captured 117 steps, but the background writer entered an error state before the user could complete trajectory validation. The exact writer exception was not available in the screenshot, so the root cause is not claimed.

The following hardening is bundled into Phase 4 rather than spending another standalone Phase 3 hotfix:

- bounded UTF-8 fields truncate safely on code-point boundaries;
- one malformed trajectory step is rejected without immediately terminating the session;
- four consecutive encoding failures are required before the writer stops;
- encoding-rejection count is exposed separately from queue drops;
- a closed failed session detaches from the service instead of wedging it permanently;
- K can start a fresh session after the failure latch is cleared;
- per-session counters reset on restart;
- the compact HUD shows a shortened failure reason;
- the System inspector retains the complete failure text and partial path;
- error status is red, recording/finalizing status is magenta, and ordinary overlay-toggle notices remain cyan.

## Gate

Phase 3 telemetry format, fixture, CRC, recovery, validator, and replay checks pass offline. Target-machine end-to-end telemetry acceptance remains open until a Phase 4 build creates a clean `.sbt` file and the latest-telemetry validator passes.
