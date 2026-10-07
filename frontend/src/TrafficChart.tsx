import type { Decision } from "./types";
import { useState, useEffect, useRef } from "react";
export function buckets(events: Decision[], seconds: number) {
  const end = events.length
    ? Math.max(...events.map((e) => e.timestamp))
    : Date.now();
  const step = (seconds * 1000) / 30;
  const rows = Array.from({ length: 30 }, (_, i) => ({
    time: end - (29 - i) * step,
    allow: 0,
    block: 0,
  }));
  for (const event of events) {
    const i = Math.floor((event.timestamp - (end - seconds * 1000)) / step);
    if (i >= 0 && i <= 30) {
      const row = rows[Math.min(i, 29)];
      if (event.action === "BLOCK") row.block++;
      else row.allow++;
    }
  }
  return rows;
}
export default function TrafficChart({ events }: { events: Decision[] }) {
  const [seconds, setSeconds] = useState(60);
  const svg = useRef<SVGSVGElement>(null);
  const [width, setWidth] = useState(1050);
  useEffect(() => {
    if (!svg.current) return;
    const observer = new ResizeObserver(([entry]) =>
      setWidth(Math.max(300, entry.contentRect.width)),
    );
    observer.observe(svg.current);
    return () => observer.disconnect();
  }, []);
  const plot = width - 80;
  const rows = buckets(events, seconds),
    max = Math.max(5, ...rows.flatMap((r) => [r.allow, r.block]));
  const path = (key: "allow" | "block") =>
    rows
      .map(
        (r, i) =>
          `${i ? "L" : "M"} ${50 + (i * plot) / 29} ${175 - (r[key] / max) * 140}`,
      )
      .join(" ");
  return (
    <section className="panel chart-panel">
      <div className="panel-heading">
        <h2>Traffic activity</h2>
        <div className="chart-tools">
          <div className="segments">
            {[60, 300, 900].map((s) => (
              <button
                key={s}
                className={seconds === s ? "active" : ""}
                onClick={() => setSeconds(s)}
              >
                {s / 60}m
              </button>
            ))}
          </div>
          <span className="legend">
            <i />
            Allowed / observed <i className="coral" />
            Blocked
          </span>
        </div>
      </div>
      <svg
        className="traffic-chart"
        ref={svg}
        viewBox={`0 0 ${width} 215`}
        preserveAspectRatio="none"
        role="img"
        aria-label={`Request counts per ${seconds / 30} second bucket from retained decisions`}
      >
        {[0, 1, 2, 3, 4].map((i) => (
          <g key={i}>
            <line
              x1="50"
              x2={width - 30}
              y1={175 - i * 35}
              y2={175 - i * 35}
              className="gridline"
            />
            <text x="38" y={179 - i * 35} textAnchor="end">
              {Math.round((max * i) / 4)}
            </text>
          </g>
        ))}
        {[0, 6, 12, 18, 24, 29].map((i) => (
          <g
            key={i}
            className={[6, 18, 24].includes(i) ? "minor-tick" : undefined}
          >
            <line
              x1={50 + (i * plot) / 29}
              x2={50 + (i * plot) / 29}
              y1="35"
              y2="175"
              className="gridline"
            />
            <text
              x={50 + (i * plot) / 29}
              y="202"
              textAnchor={i === 0 ? "start" : i === 29 ? "end" : "middle"}
            >
              {new Date(rows[i].time).toLocaleTimeString([], { hour12: false })}
            </text>
          </g>
        ))}
        <path
          d={`${path("allow")} L ${width - 30} 175 L 50 175 Z`}
          fill="var(--teal)"
          opacity=".05"
        />
        <path
          d={path("allow")}
          fill="none"
          stroke="var(--teal)"
          strokeWidth="2"
        />
        <path
          d={path("block")}
          fill="none"
          stroke="var(--coral)"
          strokeWidth="2"
        />
        {!events.length && (
          <text
            x={width / 2}
            y="95"
            textAnchor="middle"
            className="chart-empty"
          >
            Waiting for traffic
          </text>
        )}
      </svg>
      <div className="chart-caption">
        Requests per {seconds / 30}s bucket · last retained event anchors the
        window · up to 1,000 decisions
      </div>
    </section>
  );
}
