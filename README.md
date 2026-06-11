# BranchDesk

Branch-automation console for credit unions, by **Meridian Branch Systems**.
Two faces over one backend:

- **Serve** — the teller-facing console.
- **Kiosk** — the customer self-service terminal.

Both talk to a single in-process `BranchOperationService` (a deterministic,
seedable mock — no real hardware, no real core banking). The application logic
lives in how the frontend orchestrates service calls, handles backend events,
and branches the workflow: authentication and supervisor override, cashier's
check screening, cash withdrawal, deposit holds, transaction queuing, device
error recovery, and cash-inventory audit.

## Stack

- Apache Wicket 9.17.0 (server-rendered, stateful, component-based)
- Java 17, Maven, WAR on embedded Jetty
- H2 in-memory, SLF4J + Logback
- WicketTester + JUnit 5

## Build & run

```bash
mvn jetty:run        # boots http://localhost:8080/  (override -Djetty.http.port=NNNN)
mvn test             # WicketTester + service suite
```

> The automated suite covers happy-path workflows, using the device
> simulators. **A full manual regression is required before any release.**

## Layout

```
branchdesk/
├── pom.xml
├── scenarios/          # deterministic replay scripts (JSON)
├── traces/             # behavioral traces (regenerated from scenarios + seed)
└── src/main/java/com/meridianbranch/branchdesk/
    ├── BranchDeskApplication.java
    ├── teller/         # Serve console pages + panels
    ├── kiosk/          # Kiosk self-service pages + panels
    ├── workflow/       # FE-side workflow/state orchestration
    ├── service/        # BranchOperationService + Mock + events/
    ├── sim/            # device + screening + core simulators (seeded)
    ├── trace/          # behavioral trace emitter
    ├── model/          # domain models
    └── config/         # branch config: thresholds, device mode
```

## Behavioral traces

`ScenarioRunner` replays each `scenarios/*.json` through the workflow layer and
writes a byte-stable `traces/<scenario>.jsonl` (record schema
`{seq, face, kind, name, data, ts}`). Traces are reproducible given the same
scenario and seed, which is what makes them usable as a regression oracle.
