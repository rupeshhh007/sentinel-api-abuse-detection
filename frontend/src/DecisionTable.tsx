import type { Decision } from "./types";
import { ArrowRight, Search } from "lucide-react";
export function Action({ value }: { value: string }) {
  return <span className={`action ${value.toLowerCase()}`}>{value}</span>;
}
export default function DecisionTable({
  events,
  onSelect,
  onAll,
  compact = false,
}: {
  events: Decision[];
  onSelect: (d: Decision) => void;
  onAll?: () => void;
  compact?: boolean;
}) {
  return (
    <section className="panel decision-panel">
      <div className="panel-heading">
        <h2>Decision stream</h2>
        {onAll && (
          <button className="text-button" onClick={onAll}>
            View all <ArrowRight size={15} />
          </button>
        )}
        <span className="muted">{events.length} retained</span>
      </div>
      <div className="table-scroll">
        <table>
          <thead>
            <tr>
              <th>Time</th>
              <th>Client</th>
              <th>Endpoint</th>
              <th>Score</th>
              <th>Action</th>
              <th aria-label="Inspect" />
            </tr>
          </thead>
          <tbody>
            {events.slice(0, compact ? 6 : 100).map((d) => (
              <tr key={d.id}>
                <td className="mono">
                  {new Date(d.timestamp).toLocaleTimeString([], {
                    hour12: false,
                  })}
                </td>
                <td className="mono client">{d.client}</td>
                <td className="mono endpoint">
                  <span className="muted">{d.method}</span> {d.endpoint}
                </td>
                <td>
                  <span className={d.score >= 60 ? "risk-high" : "mono"}>
                    {d.score}
                  </span>
                </td>
                <td>
                  <Action value={d.action} />
                </td>
                <td>
                  <button
                    className="icon-button"
                    aria-label={`Inspect decision ${d.id}`}
                    onClick={() => onSelect(d)}
                  >
                    <ArrowRight size={16} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!events.length && (
        <div className="empty">
          <Search size={25} />
          <strong>No decisions yet</strong>
          <p>
            Send a protected request or run an isolated scenario in the attack
            lab.
          </p>
        </div>
      )}
      {!compact && events.length > 100 && (
        <p className="panel-note">
          Showing newest 100 matches. Export includes all retained decisions.
        </p>
      )}
    </section>
  );
}
export function Evidence({ decision }: { decision?: Decision }) {
  return (
    <section className="panel evidence">
      <div className="panel-heading">
        <h2>Detection signals</h2>
        {decision && <span className="mono">{decision.score}/100</span>}
      </div>
      {decision ? (
        decision.signals.map((s) => (
          <div className="signal" key={s.name}>
            <div>
              <span>{s.name}</span>
              <span className="mono">+{s.points}</span>
            </div>
            <div className="track">
              <span style={{ width: `${Math.min(1, s.value) * 100}%` }} />
            </div>
            <p>{s.evidence}</p>
          </div>
        ))
      ) : (
        <div className="empty">
          <strong>Evidence, not a black box</strong>
          <p>
            Select a decision to inspect the measured signals behind its score.
          </p>
        </div>
      )}
    </section>
  );
}
