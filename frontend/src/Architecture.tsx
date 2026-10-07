import { ArrowRight, Download } from "lucide-react";
const steps = [
  [
    "01",
    "Intercept",
    "Servlet filter protects /api/protected/* and /ping. Console and health traffic bypass detection.",
  ],
  [
    "02",
    "Identify",
    "SHA-256 client fingerprint; independently hashed socket peer aggregates rotating identities.",
  ],
  [
    "03",
    "Collect",
    "Redis TIME + one atomic Lua invocation. Cluster hash tags co-locate rate and history keys.",
  ],
  [
    "04",
    "Explain",
    "Timing variation, endpoint concentration, client and source rate pressure. Up to 20 behavior samples.",
  ],
  [
    "05",
    "Decide",
    "Versioned policy chooses enforce or observe. Dependency failures follow an explicit availability policy.",
  ],
];
export function Pipeline() {
  return (
    <section className="panel pipeline">
      <div>
        <h2>Explainable by design</h2>
        <p>Trace how each request is analyzed and why a decision was made.</p>
      </div>
      <div className="pipeline-steps">
        {[
          "Request",
          "Fingerprint",
          "Behavioral signals",
          "Risk",
          "Decision",
        ].map((s, i) => (
          <span key={s}>
            <b>{String(i + 1).padStart(2, "0")}</b>
            {s}
            {i < 4 && <ArrowRight size={16} />}
          </span>
        ))}
      </div>
    </section>
  );
}
export default function Architecture() {
  return (
    <>
      <div className="page-title">
        <div>
          <h1>Inside the engine</h1>
          <p>Built to be understood, tested, and challenged.</p>
        </div>
        <a
          className="secondary"
          href="/v3/api-docs"
          target="_blank"
          rel="noreferrer"
        >
          <Download size={16} />
          OpenAPI
        </a>
      </div>
      <div className="architecture-flow">
        {steps.map(([number, title, text]) => (
          <section key={number}>
            <span className="step-index">{number}</span>
            <div>
              <h2>{title}</h2>
              <p>{text}</p>
            </div>
            <ArrowRight size={20} />
          </section>
        ))}
      </div>
      <div className="architecture-notes">
        <section className="panel">
          <h2>Distributed where it matters</h2>
          <p>
            Redis owns live detection state. One server-side script atomically
            updates client and source rolling windows and bounded history. UUID
            members preserve concurrent requests with identical timestamps.
          </p>
          <p>
            Rate sets saturate above the configured budget to cap per-key
            memory. Telemetry retains 1,000 decisions per origin per process;
            aggregate counters are lifetime instance totals.
          </p>
        </section>
        <section className="panel">
          <h2>Honest engineering boundaries</h2>
          <p>
            Deterministic heuristics, not a trained model. Shared IPs and
            legitimate automation can trigger rate pressure. The synthetic suite
            demonstrates specific properties; it does not establish real-world
            precision or recall.
          </p>
          <p>
            Demo storage is bounded and process-local. Console policies and
            telemetry are process-local in every profile. Socket peers are
            authoritative; deploy directly or implement a reviewed trusted-proxy
            strategy.
          </p>
        </section>
        <section className="panel">
          <h2>Operate deliberately</h2>
          <p>
            Local console access is restricted to loopback by default. Remote
            operators require the configured bearer token. Mutations require a
            custom header; there is no permissive cross-origin policy.
          </p>
          <p>
            Terminate TLS for remote use. Keep actuator and Redis private.
            Export decision evidence from Investigations and inspect Prometheus
            metrics at /actuator/prometheus.
          </p>
        </section>
        <section className="panel">
          <h2>A demo you can defend</h2>
          <p>1. Run organic browsing and a catalog scraper.</p>
          <p>
            2. Investigate the first blocked request and its signal weights.
          </p>
          <p>3. Switch to observe mode; replay the same attack.</p>
          <p>4. Run the labeled evaluation and explain the limitations.</p>
        </section>
      </div>
    </>
  );
}
