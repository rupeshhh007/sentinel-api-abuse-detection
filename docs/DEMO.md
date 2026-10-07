# A five-minute, evidence-driven demonstration

Start `./scripts/demo.sh` and open http://127.0.0.1:8080. No seeded dashboard metrics appear before you run traffic.

1. **Establish a benign control.** Attack lab → Organic browsing → Run scenario. Explain variable cadence and route diversity. Run Scheduled monitor too: repetitive polling alone remains below the default action threshold.
2. **Show a real detection boundary.** Run Catalog scraper. At the default policy, request 31 is the first detection; 50 of 80 decisions are suspicious and blocked. Overview → Replay traffic shows the resulting burst.
3. **Explain the decision.** Investigations → filter BLOCK → inspect a decision. Read client rate, source rate, timing CV, route concentration, score, sample count, and policy version. Walk through the source timeline and export the evidence.
4. **Demonstrate evasion resistance.** Run Randomized flood, then Fingerprint rotation. Random timing does not bypass a hard client budget. Sixteen identities still share a source budget; default source detection begins at request 121.
5. **Make an operational tradeoff.** Policies → Observe → Apply. Replay the scraper. Suspicious traffic is now observed, with zero blocks. Return to Enforce and reload/apply the current policy as needed.
6. **Measure, don't claim.** Run evaluation. Default-policy outcomes are 3 true-positive attack scenarios and 2 true-negative benign scenarios. Explain that this is a five-fixture synthetic test, not a production accuracy study.
7. **Connect to the actual filter.** Overview → Send live probe. This makes a network request to `/api/protected/catalog`, through the servlet filter. Contrast this with isolated replay, which never sends attack network traffic.
8. **Discuss the engineering.** Architecture covers atomic Redis updates, cluster co-location, server time, bounded history, hard source aggregation, version conflicts, and failure behavior. Describe instance-local telemetry/policy honestly.

## Resume wording grounded in the implementation

> Built an explainable API abuse detection workbench with Spring Boot, Redis Lua, and a React/TypeScript operator console; implemented atomic client/source rolling windows, millisecond behavioral analysis, versioned enforcement controls, and isolated reproducible attack evaluation.

> Verified concurrency and control-plane boundaries with Docker-backed Redis integration tests; added decision evidence exports, source investigations, explicit fail-open/fail-closed handling, and provisioned Prometheus/Grafana monitoring.

Do not add production throughput, false-positive rates, or latency targets until a reproducible deployment benchmark or representative labeled dataset supports them.
