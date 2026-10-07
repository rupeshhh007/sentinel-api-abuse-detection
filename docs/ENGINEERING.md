# Engineering decisions

## Keep the useful core

Spring Boot and Redis stay in place. The upgrade adds a `WindowStore` contract, a shared pure `BehaviorAnalyzer`, explicit policy/decision records, and a separate control plane. The original `/ping` response and 429 contract remain compatible. A real sample protected API demonstrates filter integration.

## Close concrete evasion paths

The original filter trusted arbitrary `X-Forwarded-For`, so identity could be changed by the caller. The new filter uses the socket peer and independently groups all client fingerprints under its hashed source. A regression test rotates User-Agent and forwarded-IP values from a fixed peer, and verifies the source budget blocks request 121.

Seconds-based timestamps discarded timing detail. The analyzer now calculates coefficient of variation from millisecond gaps. Timing regularity is an indicator, not proof that a client is malicious. Periodic monitoring under budget is a negative control.

The old additive heuristic could allow a randomized rate flood if its behavior looked varied. Rate budgets now impose the action threshold independently of timing and endpoint scores.

## Atomicity and resource cost

One Redis Lua script uses Redis server time, expires old members, records UUID members, counts requests, and maintains a 20-sample history. Keys for one source share a hash slot. A real Redis concurrency test submits 100 requests on 12 threads: exactly the first 30 client decisions remain within budget, the rate set saturates at 31, and the history stays bounded.

Denied attempts do not keep growing the sorted set. Saturated counts represent lower bounds. The source check runs first and prevents allocating new client keys after the source budget is exhausted; a regression test checks that rotating fingerprints do not create such keys. Global memory is still proportional to active sources, so Compose configures a Redis memory limit with `noeviction` and dependency failures remain explicit.

Server time also anchors history filtering; client JVM clock skew does not change the Lua window cutoff. Redis scripts are atomic, but still consume network and server execution time. No zero-latency claim is made.

## Data provenance and observability

Live telemetry comes only from protected network requests. Replay has isolated storage, seeded inputs, and virtual time; it calls the same analyzer and policy action function but does not validate Redis transport or HTTP enforcement. Both origins are visibly labeled and retained separately.

Latency in the console is a p95 over retained decisions, not response latency or a production SLA. Replay compute time is labeled separately. Prometheus records live pipeline latency with a Micrometer timer. Grafana displays mean decision latency from timer sum/count, not a fabricated percentile.

The console retains bounded process-local evidence. It is deliberately not represented as a durable forensic audit store. No unbounded per-client metric labels are created.

## Control plane and deployment

Default local operation allows only loopback peers with localhost/loopback host names, preventing arbitrary host names from using the tokenless local exception. A configured token applies to all console callers. Constant-time byte comparison checks bearer tokens. A mandatory custom mutation header prevents simple cross-origin form writes; CORS is not opened. Tokens are held in frontend memory only. Docker-published connections arrive through a bridge and require a token.

Each retained decision includes its immutable policy snapshot, so exports remain explainable after policy edits. Hard-rate threshold floors are explicitly shown in the evidence inspector. Policy updates reject stale versions rather than silently overwriting an operator's changes. Mode and numeric bounds are checked server-side. Fail-open emits a degraded decision; fail-closed emits 503, distinguishable from a 429 abuse response. Backend readiness checks Redis independently of console connectivity.

Docker builds React assets into the JAR and runs the application as a non-root user. Redis and monitoring stay private or loopback-published. Monitoring requires an explicitly configured Grafana password. For remote production usage, TLS termination, proper role-based operator identity, centralized policy distribution, durable evidence, and a reviewed trusted-proxy boundary remain necessary.

## Calibration boundaries

Five deterministic scenarios are a reproducible smoke evaluation, not a statistical dataset. The evaluation reports scenario-level true/false positives and negatives under the actual current policy, and does not pollute replay telemetry. Shared IPs, high-volume legitimate clients, jittered automation, and application-specific route semantics require representative ground truth before claiming accuracy.
