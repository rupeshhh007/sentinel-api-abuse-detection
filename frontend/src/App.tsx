import { useCallback, useEffect, useRef, useState } from "react";
import {
  Activity,
  Search,
  FlaskConical,
  Shield,
  Network,
  Play,
  Download,
  Folder,
  Database,
  KeyRound,
  RefreshCw,
} from "lucide-react";
import { api, download, setToken } from "./api";
import type { Decision, Overview } from "./types";
import { DecisionInspector, OperatorAccess } from "./ConsoleDialogs";
import TrafficChart from "./TrafficChart";
import DecisionTable, { Evidence } from "./DecisionTable";
import AttackLab from "./AttackLab";
import Policies from "./Policies";
import Architecture, { Pipeline } from "./Architecture";
const pages = [
  { name: "Overview", icon: Activity },
  { name: "Investigations", icon: Search },
  { name: "Attack lab", icon: FlaskConical },
  { name: "Policies", icon: Shield },
  { name: "Architecture", icon: Network },
];
export default function App() {
  const [page, setPage] = useState("Overview"),
    [origin, setOrigin] = useState("REPLAY"),
    [overview, setOverview] = useState<Overview>(),
    [error, setError] = useState(""),
    [selected, setSelected] = useState<Decision>(),
    [query, setQuery] = useState(""),
    [action, setAction] = useState("ALL"),
    [connect, setConnect] = useState(false),
    [busy, setBusy] = useState(false);
  const currentOrigin = useRef(origin);
  useEffect(() => {
    currentOrigin.current = origin;
  }, [origin]);
  const refresh = useCallback(
    async (signal?: AbortSignal) => {
      try {
        const d = await api<Overview>(
          `overview?origin=${origin}`,
          "GET",
          undefined,
          signal,
        );
        if (signal?.aborted || currentOrigin.current !== origin) return;
        setOverview(d);
        setError("");
      } catch (e) {
        if (!signal?.aborted && currentOrigin.current === origin)
          setError((e as Error).message);
      }
    },
    [origin],
  );
  useEffect(() => {
    const controller = new AbortController();
    refresh(controller.signal);
    const timer = setInterval(() => refresh(controller.signal), 4000);
    return () => {
      controller.abort();
      clearInterval(timer);
    };
  }, [refresh]);
  useEffect(() => {
    if (!selected && !connect) return;
    const previous = document.activeElement as HTMLElement | null;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        setSelected(undefined);
        setConnect(false);
      }
      if (e.key === "Tab") {
        const nodes = Array.from(
          document.querySelectorAll<HTMLElement>(
            '[role="dialog"] button:not(:disabled),[role="dialog"] input,[role="dialog"] select,[role="dialog"] a[href]',
          ),
        );
        const first = nodes[0],
          last = nodes[nodes.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last?.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first?.focus();
        }
      }
    };
    document.addEventListener("keydown", onKey);
    const old = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = old;
      previous?.focus();
    };
  }, [selected, connect]);
  const stats =
    overview?.telemetry.origin === origin ? overview.telemetry : undefined;
  const events = stats?.events || [];
  const filtered = events.filter(
    (d) =>
      (action === "ALL" || d.action === action) &&
      `${d.client} ${d.source} ${d.endpoint} ${d.id}`
        .toLowerCase()
        .includes(query.toLowerCase()),
  );
  async function liveProbe() {
    setBusy(true);
    try {
      const r = await fetch("/api/protected/catalog");
      if (!r.ok && r.status !== 429 && r.status !== 503)
        throw new Error(`Probe returned ${r.status}`);
      setOrigin("LIVE");
      if (origin === "LIVE") await refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          href="#"
          className="brand"
          onClick={(e) => {
            e.preventDefault();
            setPage("Overview");
          }}
        >
          <svg width="30" height="38" viewBox="0 0 30 38" aria-hidden="true">
            <path
              d="M27 8 15 1 3 8v9l19 10-7 4-9-5-5 4 14 8 13-8v-9L9 11l6-4 8 5Z"
              fill="currentColor"
            />
          </svg>
          Sentinel
        </a>
        <nav aria-label="Main navigation">
          {pages.map(({ name, icon: Icon }) => (
            <button
              key={name}
              className={page === name ? "nav-item selected" : "nav-item"}
              onClick={() => {
                setPage(name);
                setSelected(undefined);
              }}
            >
              <Icon size={19} />
              {name}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <span>
            <Folder size={18} />
            Local workspace
          </span>
          <span>
            <Database size={18} />
            {overview?.storage === "REDIS"
              ? "Redis engine"
              : "Demo environment"}
            <i className="dot" />
          </span>
          <button className="text-button" onClick={() => setConnect(true)}>
            <KeyRound size={15} />
            Operator access
          </button>
        </div>
      </aside>
      <div className="workspace">
        <header className="topbar">
          <span>
            Workspace <span className="slash">/</span> {page}
          </span>
          <span
            className={
              error || overview?.engineHealthy === false
                ? "engine offline"
                : "engine"
            }
          >
            <i className="dot" />
            {error
              ? "Connection interrupted"
              : overview?.engineHealthy === false
                ? "Store unavailable"
                : overview
                  ? "Engine online"
                  : "Connecting…"}
          </span>
        </header>
        <main>
          {error && (
            <div className="error" role="alert">
              {error}{" "}
              <button className="text-button" onClick={() => setConnect(true)}>
                Connect
              </button>
            </div>
          )}
          {(page === "Overview" || page === "Investigations") && (
            <>
              <div className="page-title">
                <div>
                  <h1>
                    {page === "Overview"
                      ? "Traffic overview"
                      : "Investigations"}
                  </h1>
                  <p>
                    {page === "Overview"
                      ? "Every request. A decision you can explain."
                      : "Follow the signals from suspicious traffic to a defensible decision."}
                  </p>
                </div>
                <button
                  className="primary"
                  onClick={() => setPage("Attack lab")}
                >
                  <Play size={16} />
                  Run scenario
                </button>
              </div>
              <div className="traffic-controls">
                <div className="segments">
                  <button
                    className={origin === "REPLAY" ? "active" : ""}
                    onClick={() => {
                      setOrigin("REPLAY");
                      setSelected(undefined);
                    }}
                  >
                    Replay traffic
                  </button>
                  <button
                    className={origin === "LIVE" ? "active" : ""}
                    onClick={() => {
                      setOrigin("LIVE");
                      setSelected(undefined);
                    }}
                  >
                    Live traffic
                  </button>
                </div>
                <span className="muted">
                  {origin === "REPLAY"
                    ? "Isolated synthetic traffic"
                    : "Protected endpoint traffic"}{" "}
                  · policy v{overview?.policy.version || "—"} ·{" "}
                  {overview?.policy.mode.toLowerCase() || "—"}
                </span>
                <button
                  className="text-button"
                  disabled={busy}
                  onClick={liveProbe}
                >
                  <Activity size={14} />
                  Send live probe
                </button>
                <button
                  className="icon-button"
                  aria-label="Refresh traffic"
                  onClick={() => refresh()}
                >
                  <RefreshCw size={15} />
                </button>
              </div>
              {page === "Overview" ? (
                <>
                  <div className="metrics">
                    {[
                      ["Total requests", stats?.total || 0, ""],
                      ["Blocked", stats?.blocked || 0, "coral-text"],
                      ["Observed", stats?.observed || 0, ""],
                      [
                        "Decision latency",
                        (stats?.p95Ms || 0).toFixed(2),
                        "teal-text",
                      ],
                    ].map(([label, value, color]) => (
                      <div className="metric" key={label}>
                        <span>{label}</span>
                        <strong className={String(color)}>
                          {value}
                          {label === "Decision latency" && (
                            <small>
                              ms <em>p95</em>
                            </small>
                          )}
                        </strong>
                      </div>
                    ))}
                  </div>
                  {!!stats?.degraded && (
                    <div className="error">
                      {stats.degraded} degraded decisions: detection storage was
                      unavailable. Inspect live evidence.
                    </div>
                  )}
                  <TrafficChart events={events} />
                  <div className="overview-lower">
                    <DecisionTable
                      events={events}
                      onSelect={setSelected}
                      compact
                      onAll={() => setPage("Investigations")}
                    />
                    <Evidence decision={events[0]} />
                  </div>
                  <Pipeline />
                </>
              ) : (
                <>
                  <div className="filter-row">
                    <label className="search-input">
                      <Search size={17} />
                      <input
                        aria-label="Search decisions"
                        placeholder="Search client, source, route, or ID"
                        value={query}
                        onChange={(e) => setQuery(e.target.value)}
                      />
                    </label>
                    <select
                      aria-label="Filter action"
                      value={action}
                      onChange={(e) => setAction(e.target.value)}
                    >
                      {[
                        "ALL",
                        "ALLOW",
                        "BLOCK",
                        "OBSERVE",
                        "DEGRADED",
                        "UNAVAILABLE",
                      ].map((a) => (
                        <option key={a}>{a}</option>
                      ))}
                    </select>
                    <button
                      className="secondary"
                      onClick={() =>
                        download("sentinel-decisions.json", {
                          origin,
                          scope: overview?.scope,
                          events: filtered,
                        })
                      }
                    >
                      <Download size={16} />
                      Export evidence
                    </button>
                  </div>
                  <DecisionTable events={filtered} onSelect={setSelected} />
                  <p className="muted">
                    {overview?.scope}. Lifetime counters may exceed the retained
                    stream.
                  </p>
                </>
              )}
            </>
          )}
          {page === "Attack lab" && (
            <AttackLab
              onRun={() => {
                setOrigin("REPLAY");
                if (origin === "REPLAY") refresh();
              }}
            />
          )}
          {page === "Policies" && overview && (
            <Policies policy={overview.policy} onSave={() => refresh()} />
          )}
          {page === "Architecture" && <Architecture />}
          {!overview && !error && (
            <div className="empty" role="status">
              Connecting to Sentinel…
            </div>
          )}
          <footer>
            Sentinel / API security workbench{" "}
            <span>Deterministic detection. Inspectable evidence.</span>
          </footer>
        </main>
      </div>
      {selected && (
        <DecisionInspector
          selected={selected}
          events={events}
          onClose={() => setSelected(undefined)}
          onSelect={setSelected}
        />
      )}
      {connect && (
        <OperatorAccess
          onClose={() => setConnect(false)}
          onConnect={(value) => {
            setToken(value);
            setConnect(false);
            refresh();
          }}
        />
      )}
    </div>
  );
}
