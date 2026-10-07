import { useState } from "react";
import { Save, ShieldCheck } from "lucide-react";
import { api } from "./api";
import type { Policy } from "./types";
export default function Policies({
  policy,
  onSave,
}: {
  policy: Policy;
  onSave: () => void;
}) {
  const [draft, setDraft] = useState(policy),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState(""),
    [error, setError] = useState("");
  async function save() {
    setBusy(true);
    setError("");
    setMessage("");
    try {
      setDraft(await api<Policy>("policy", "PUT", draft));
      setMessage("Policy applied. New requests use this version.");
      onSave();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  const fields: [keyof Policy, string, string, number, number][] = [
    [
      "maxRequests",
      "Client request budget",
      "Per fingerprint, per rolling window",
      5,
      10000,
    ],
    [
      "sourceMaxRequests",
      "Source request budget",
      "Across all fingerprints from one socket peer",
      5,
      100000,
    ],
    ["windowSeconds", "Window length", "Rolling window in seconds", 10, 300],
    [
      "threshold",
      "Risk threshold",
      "Behavioral score required to act; rate budgets always act",
      40,
      100,
    ],
  ];
  return (
    <>
      <div className="page-title">
        <div>
          <h1>Enforcement policies</h1>
          <p>Make the tradeoff explicit. Ship the policy with evidence.</p>
        </div>
        <span className="version">Version {draft.version}</span>
      </div>
      <div className="policy-layout">
        <form
          className="panel policy-form"
          onSubmit={(e) => {
            e.preventDefault();
            save();
          }}
        >
          <div className="panel-heading">
            <h2>Decision policy</h2>
            <ShieldCheck size={20} />
          </div>
          <label>
            Enforcement mode
            <select
              value={draft.mode}
              onChange={(e) => setDraft({ ...draft, mode: e.target.value })}
            >
              <option value="ENFORCE">
                Enforce — block suspicious traffic
              </option>
              <option value="OBSERVE">Observe — record without blocking</option>
            </select>
          </label>
          {fields.map(([key, label, help, min, max]) => (
            <label key={key}>
              {label}
              <small>{help}</small>
              <input
                required
                type="number"
                min={min}
                max={max}
                value={draft[key]}
                onChange={(e) =>
                  setDraft({ ...draft, [key]: Number(e.target.value) })
                }
              />
            </label>
          ))}
          <label>
            Dependency failure mode
            <select
              value={draft.failureMode}
              onChange={(e) =>
                setDraft({ ...draft, failureMode: e.target.value })
              }
            >
              <option value="OPEN">Fail open — allow, mark as degraded</option>
              <option value="CLOSED">Fail closed — return 503</option>
            </select>
          </label>
          {error && (
            <div className="error" role="alert">
              {error}
            </div>
          )}
          {message && (
            <div className="success" role="status">
              {message}
            </div>
          )}
          <div className="panel-footer">
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setDraft(policy);
                setError("");
                setMessage("");
              }}
            >
              Reload current policy
            </button>
            <button className="primary" disabled={busy}>
              <Save size={16} />
              {busy ? "Applying…" : "Apply policy"}
            </button>
          </div>
        </form>
        <aside className="policy-guide">
          <h2>Know what changes</h2>
          <p>
            Changes take effect for subsequent live requests and new replays.
            Existing decisions retain their policy version.
          </p>
          <h3>Observe before enforcing</h3>
          <p>
            Use observe mode to inspect suspicious decisions without returning
            429. Run both attack and benign fixtures before changing your
            threshold.
          </p>
          <h3>Rate budgets are hard limits</h3>
          <p>
            Crossing either rate budget triggers a decision even if the request
            cadence is irregular. Fingerprint rotation still shares the source
            budget.
          </p>
          <h3>Instance-local configuration</h3>
          <p>
            Edits are version-checked to prevent lost updates. They reset on
            restart. Configure baseline limits in application properties;
            multi-replica policy distribution is an explicit future boundary.
          </p>
          <h3>Failure is visible</h3>
          <p>
            Redis failure produces a degraded decision or a 503, according to
            your policy. It never silently switches to local storage.
          </p>
        </aside>
      </div>
    </>
  );
}
