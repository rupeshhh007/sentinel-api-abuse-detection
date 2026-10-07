import { useState } from "react";
import { X, Download, ArrowRight } from "lucide-react";
import { download } from "./api";
import type { Decision } from "./types";
import { Action, Evidence } from "./DecisionTable";
export function DecisionInspector({
  selected,
  events,
  onClose,
  onSelect,
}: {
  selected: Decision;
  events: Decision[];
  onClose: () => void;
  onSelect: (decision: Decision) => void;
}) {
  return (
    <div className="modal-backdrop" onClick={() => onClose()}>
      <section
        className="inspector"
        role="dialog"
        aria-modal="true"
        aria-labelledby="decision-heading"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="panel-heading">
          <h2 id="decision-heading">Decision evidence</h2>
          <button
            className="icon-button"
            aria-label="Close evidence"
            autoFocus
            onClick={() => onClose()}
          >
            <X size={20} />
          </button>
        </div>
        <div className="inspector-summary">
          <Action value={selected.action} />
          <strong>
            {selected.score}
            <small>/100</small>
          </strong>
          <p className="mono">
            {selected.method} {selected.endpoint}
          </p>
        </div>
        <dl>
          {[
            ["Decision ID", selected.id],
            ["Client", selected.client],
            ["Source group", selected.source],
            ["Origin", selected.origin],
            ["Timestamp", new Date(selected.timestamp).toISOString()],
            ["Policy version", selected.policyVersion],
            ["Mode", selected.policy.mode],
            ["Risk threshold", selected.policy.threshold],
            ["Window", `${selected.policy.windowSeconds}s`],
            ["Samples", selected.samples],
            ["Decision time", `${selected.latencyMs.toFixed(3)} ms`],
          ].map(([k, v]) => (
            <div key={k}>
              <dt>{k}</dt>
              <dd>{v}</dd>
            </div>
          ))}
        </dl>
        <section className="panel timeline">
          <div className="panel-heading">
            <h2>Source timeline</h2>
            <span className="muted">Last 20 retained</span>
          </div>
          <div className="timeline-rows">
            {events
              .filter((d) => d.source === selected.source)
              .slice(0, 20)
              .reverse()
              .map((d) => (
                <button
                  key={d.id}
                  className={
                    d.id === selected.id
                      ? "timeline-row current"
                      : "timeline-row"
                  }
                  onClick={() => onSelect(d)}
                >
                  <span className={`timeline-dot ${d.action.toLowerCase()}`} />
                  <span className="mono">
                    {new Date(d.timestamp).toLocaleTimeString([], {
                      hour12: false,
                    })}
                  </span>
                  <span className="mono">{d.client}</span>
                  <Action value={d.action} />
                  <span className="mono">{d.score}</span>
                </button>
              ))}
          </div>
        </section>
        <Evidence decision={selected} />
        {selected.score >
          selected.signals.reduce((sum, signal) => sum + signal.points, 0) && (
          <div className="callout">
            Hard rate constraint: score raised to the policy threshold of{" "}
            {selected.policy.threshold}. Rate pressure triggers an action
            independently of behavioral regularity.
          </div>
        )}
        <div className="callout">
          {selected.action === "BLOCK"
            ? "Rate budget exceeded or score crossed the policy threshold."
            : selected.action === "OBSERVE"
              ? "Suspicious under the policy. Observe mode allowed the request."
              : selected.action === "DEGRADED" ||
                  selected.action === "UNAVAILABLE"
                ? "Detection storage was unavailable. The explicit failure policy determined this action."
                : "Below the action threshold and within rate budgets."}
        </div>
        <button
          className="secondary"
          onClick={() => download(`decision-${selected.id}.json`, selected)}
        >
          <Download size={16} />
          Export this decision
        </button>
      </section>
    </div>
  );
}
export function OperatorAccess({
  onClose,
  onConnect,
}: {
  onClose: () => void;
  onConnect: (token: string) => void;
}) {
  const [credential, setCredential] = useState("");
  return (
    <div className="modal-backdrop">
      <form
        className="connect-modal panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="connect-heading"
        onSubmit={(e) => {
          e.preventDefault();
          onConnect(credential);
        }}
      >
        <div className="panel-heading">
          <h2 id="connect-heading">Operator access</h2>
          <button
            type="button"
            className="icon-button"
            aria-label="Close operator access"
            onClick={() => onClose()}
          >
            <X size={20} />
          </button>
        </div>
        <p>
          Enter the configured console token. It is kept in memory for this
          browser session. Local loopback access needs no token unless one is
          configured.
        </p>
        <label>
          Console token
          <input
            autoFocus
            type="password"
            autoComplete="off"
            value={credential}
            onChange={(e) => setCredential(e.target.value)}
          />
        </label>
        <button className="primary">
          Connect <ArrowRight size={16} />
        </button>
      </form>
    </div>
  );
}
