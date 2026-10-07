# Sentinel upgrade plan

Preserve Spring Boot 3.4 / Java 17 and Redis; introduce a React/Vite console.

1. Shared deterministic detector: millisecond timing coefficient of variation, endpoint concentration, client and source rate pressure. Atomic bounded Redis history; dependency-free local demo adapter.
2. Decision pipeline: explicit enforce/observe modes, evidence, retry hints, safe failure handling, peer IP identity and separate IP limit resilient to header rotation.
3. Operator API: bounded instance-local telemetry, incident timelines, versioned policy edits, isolated reproducible replay and labeled evaluation. Restrict console to loopback or token authentication.
4. Complete frontend: overview, investigation, attack lab, policies, architecture; real API state, no prefilled fake metrics.
5. Regression tests, frontend build/type checks, browser desktop/mobile workflows, packaging and truthful documentation.

## Scope and limits
Not a reverse proxy or an ML classifier. Heuristics are calibrated with synthetic fixtures, not production ground truth. Replay uses the production scoring function with isolated local state and virtual time. Redis detection state is distributed; console telemetry and policy are per process and bounded/in-memory. Remote operation requires token + TLS at deployment. No third-party AI service needed.

## Design system
Navy #0b1018 background, #111925 panels, #243043 border; teal #5de4c7 primary, coral #ff7b87 blocked, blue-gray muted text. System sans with monospace data. 210px rail, generous 32px gutters, open metrics, bordered chart/table, evidence inspector; flat surfaces, restrained 10px corners, no gradients. Mobile horizontal navigation and vertically stacked panels. Concepts use illustrative values only; real UI starts empty. Additional lab/policy/investigation surfaces extend these component families for necessary workflows.
