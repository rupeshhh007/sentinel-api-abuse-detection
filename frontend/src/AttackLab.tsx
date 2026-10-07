import { useEffect, useState } from "react";
import { Play, Download, FlaskConical, ArrowUpRight } from "lucide-react";
import { api, download } from "./api";
import type { Scenario, Replay, Evaluation } from "./types";
export default function AttackLab({ onRun }: { onRun: () => void }) {
  const [scenarios, setScenarios] = useState<Scenario[]>([]),
    [selected, setSelected] = useState("scraper"),
    [busy, setBusy] = useState(false),
    [result, setResult] = useState<Replay>(),
    [evaluation, setEvaluation] = useState<Evaluation>(),
    [error, setError] = useState("");
  useEffect(() => {
    const c = new AbortController();
    api<Scenario[]>("scenarios", "GET", undefined, c.signal)
      .then(setScenarios)
      .catch((e) => {
        if (!c.signal.aborted) setError(e.message);
      });
    return () => c.abort();
  }, []);
  async function run(evaluate = false) {
    setBusy(true);
    setError("");
    try {
      if (evaluate) setEvaluation(await api<Evaluation>("evaluation", "POST"));
      else {
        setResult(await api<Replay>("replay", "POST", { scenario: selected }));
        onRun();
      }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <>
      <div className="page-title">
        <div>
          <h1>Attack lab</h1>
          <p>
            Reproduce the attack. Inspect the evidence. Challenge the policy.
          </p>
        </div>
        <FlaskConical className="title-icon" size={32} />
      </div>
      <div className="callout">
        <span className="dot" />
        Isolated replay · virtual time · seed 42 · production scoring function
        <div>
          Replay does not send network traffic or modify live detection state.
        </div>
      </div>
      {error && (
        <div role="alert" className="error">
          {error}
        </div>
      )}
      <div className="lab-layout">
        <section className="panel">
          <div className="panel-heading">
            <h2>Choose a scenario</h2>
            <span className="muted">{scenarios.length} fixtures</span>
          </div>
          <div className="scenario-list">
            {scenarios.map((s) => (
              <button
                key={s.id}
                className={`scenario ${selected === s.id ? "selected" : ""}`}
                onClick={() => setSelected(s.id)}
              >
                <span className="scenario-number">
                  0{scenarios.indexOf(s) + 1}
                </span>
                <span>
                  <strong>{s.name}</strong>
                  <p>{s.description}</p>
                  <small>
                    {s.requests} requests ·{" "}
                    {s.malicious ? "Attack fixture" : "Benign control"}
                  </small>
                </span>
                <ArrowUpRight size={18} />
              </button>
            ))}
          </div>
          <div className="panel-footer">
            <button
              className="primary"
              disabled={busy || !scenarios.length}
              onClick={() => run()}
            >
              <Play size={16} />
              {busy ? "Evaluating…" : "Run scenario"}
            </button>
          </div>
        </section>
        <section className="panel replay-result">
          <div className="panel-heading">
            <h2>Replay result</h2>
            {result && (
              <button
                className="icon-button"
                aria-label="Export replay"
                onClick={() => download("sentinel-replay.json", result)}
              >
                <Download size={18} />
              </button>
            )}
          </div>
          {result ? (
            <>
              <div className="result-score">
                <span>{result.suspicious}</span>
                <p>suspicious decisions / {result.requests} requests</p>
              </div>
              <div className="result-stats">
                <div>
                  <b>{result.firstDetection || "—"}</b>
                  <span>First detection · request #</span>
                </div>
                <div>
                  <b>{result.blocked}</b>
                  <span>Blocked</span>
                </div>
                <div>
                  <b>{result.observed}</b>
                  <span>Observed</span>
                </div>
                <div>
                  <b>{result.decisionMs.toFixed(2)} ms</b>
                  <span>Total replay compute time</span>
                </div>
              </div>
              <p className="panel-note">
                Policy v{result.policy.version} · {result.policy.mode} ·{" "}
                {result.policy.maxRequests} requests /{" "}
                {result.policy.windowSeconds}s. Virtual timestamps are displayed
                in replay traffic.
              </p>
            </>
          ) : (
            <div className="empty tall">
              <FlaskConical size={38} />
              <strong>A repeatable security demonstration</strong>
              <p>
                Run a scenario to see the detection point, decisions, and
                execution time.
              </p>
            </div>
          )}
        </section>
      </div>
      <section className="panel evaluation">
        <div className="panel-heading">
          <div>
            <h2>Put the detector to the test</h2>
            <p className="muted">
              Attack and benign controls evaluated against the current policy.
            </p>
          </div>
          <button
            className="secondary"
            disabled={busy}
            onClick={() => run(true)}
          >
            Run evaluation
          </button>
        </div>
        {evaluation ? (
          <>
            <div className="evaluation-stats">
              {[
                ["True positives", evaluation.truePositive],
                ["True negatives", evaluation.trueNegative],
                ["False positives", evaluation.falsePositive],
                ["False negatives", evaluation.falseNegative],
              ].map(([label, value]) => (
                <div key={label}>
                  <b>{value}</b>
                  <span>{label}</span>
                </div>
              ))}
            </div>
            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>Scenario</th>
                    <th>Ground truth</th>
                    <th>Detection</th>
                    <th>First request</th>
                  </tr>
                </thead>
                <tbody>
                  {evaluation.scenarios.map((s) => (
                    <tr key={s.scenario}>
                      <td>{s.scenario}</td>
                      <td>{s.malicious ? "Attack" : "Benign"}</td>
                      <td>{s.detected ? "Suspicious" : "Allowed"}</td>
                      <td>{s.firstDetection || "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="panel-note">
              {evaluation.note} Policy v{evaluation.policyVersion}.
            </p>
          </>
        ) : (
          <p className="panel-note">
            Five labeled synthetic scenarios. Results are scenario-level
            outcomes, not a claim of real-world accuracy.
          </p>
        )}
      </section>
    </>
  );
}
