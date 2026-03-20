# Tracker Risk Audit (2026-03-20)

## Scope
- Internal tracker correctness, performance, and stability.
- No command/API behavior changes for users.

## Feature-Path Matrix (Ownership Check)
| Source/Event | Parser/Owner | Ledger Entry Path | Notes |
|---|---|---|---|
| Sack gains | `SackTracker` | `ProfitManager.addSackDrop` -> `ProfitState.addDrop` | Sack-only crop policy preserved. |
| Pest/rare/pet/overflow/shard chat drops | `ChatMessageParser` | `ProfitManager.addDropFromSource` -> `ProfitState.addDrop` | Non-sack crops blocked globally. |
| Bazaar purchase (cost) | `ChatMessageParser` | `ProfitManager.addVisitorCost`/`addSprayCost` -> `ProfitState` | Canonical visitor/spray cost keys normalized. |
| Visitor reward lines | `ChatMessageParser` | `ProfitManager.addVisitorGain` -> `ProfitState.addVisitorGain` | Visitor prefix handling preserved. |
| Purse deltas | `ProfitManager.update` | `ProfitState.addDrop("Purse", delta)` | Baseline fallback added to avoid pending lock. |
| Pet XP tab tracking | `PetXpTracker` | `ProfitManager.addPetXp` -> `ProfitState.addDrop` | Single scheduler remains in client tick. |

## Findings + Actions

### P0 (Fixed in this pass)
1. Visitor key mismatch causing HUD/accounting drift.
   - Evidence: mixed usage of `[Visitor] Visitor`, `[Visitor] Visitor Cost`, and count variants.
   - Action: introduced canonical keys in `ItemConstants`; updated `ProfitState`, `ProfitManager`, `ProfitHudRenderer`; added legacy key migration in `ProfitStorage`.

2. Config action-button injection could crash game.
   - Evidence: `HudEditScreen.addRenderableWidget` threw runtime exception on injection failure.
   - Action: replaced hard failure with one-time logged error; screen no longer hard-crashes from button add failure.

### P1 (Fixed in this pass)
1. HUD hot path repeated expensive recomputation.
   - Evidence: `render()` + `panelH()` both recalculated/sorted drops.
   - Action: render path now computes drops once per mode per frame and reuses for height/render.

2. `getActiveDrops` comparator repeatedly recalculated line profit during sort.
   - Evidence: comparator called `getItemPrice` on both sides for each compare.
   - Action: added line-profit cache before sort.

3. Bazaar fetch overlap/race risk.
   - Evidence: each fetch created a new thread; repeated calls could overlap and mutate shared maps.
   - Action: added single-flight fetch guard + queued rerun mechanism; switched price maps to concurrent maps.

4. Async persistence ordering risk.
   - Evidence: separate `CompletableFuture.runAsync` writes could interleave.
   - Action: moved to single-threaded storage executor for ordered disk writes.

### P2 (Open backlog)
1. Bazaar HTTP requests still run sequentially and can take long during network degradation.
   - Suggested next step: add per-request timeout and bounded retry policy.

2. Hover/drag hit-tests still call `panelH(mode)` which may rebuild drops outside main render.
   - Suggested next step: short-lived per-tick cached panel metrics.

3. Chat parser’s color reconstruction for pet rarity is brittle to unknown RGB variants.
   - Suggested next step: add safer fallback rarity parsing with explicit debug trace when unknown.

## Validation Run (This Pass)
- `./gradlew compileJava 2>&1` -> **BUILD SUCCESSFUL**.

## Backlog (Implementation-Ready)
- **P0 complete**
- **P1 complete**
- **Next recommended slice (P2):**
  1. Add HTTP timeouts/retries with jitter for Cofl requests.
  2. Add frame/tick cache for HUD panel metrics.
  3. Harden pet rarity parsing fallback and diagnostics.
