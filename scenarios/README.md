# Scenarios

Deterministic replay scripts driven by `ScenarioRunner`. Each scenario is a
config block plus an ordered list of `{face, action, ...}` steps. Running a
scenario produces a byte-stable `traces/<scenario>.jsonl` given the fixed seed.

- `01-cash-withdrawal-happy.json` — cash withdrawal with optimistic balance, reconciled on post.
- `02-cashiers-check-ofac-review.json` — cashier's-check payee screening at the review threshold.
- `03-deposit-hold.json` — a hold during a withdrawal in flight.
- `04-queued-batch.json` — batch queue with back-navigation.
- `05-after-hours-audit.json` — cash-inventory audit in after-hours mode.
