import { describe, it, expect } from "vitest";
import { buckets } from "./TrafficChart";
import type { Decision } from "./types";
const event = (timestamp: number, action: string) =>
  ({ timestamp, action }) as Decision;
describe("traffic aggregation", () => {
  it("retains the newest timestamp and separates blocked traffic", () => {
    const rows = buckets(
      [event(100000, "ALLOW"), event(100000, "BLOCK"), event(99000, "OBSERVE")],
      60,
    );
    expect(rows.reduce((n, r) => n + r.allow, 0)).toBe(2);
    expect(rows.reduce((n, r) => n + r.block, 0)).toBe(1);
  });
  it("excludes older events and handles empty traffic", () => {
    expect(buckets([], 60)).toHaveLength(30);
    const rows = buckets([event(1, "ALLOW"), event(100000, "BLOCK")], 60);
    expect(rows.reduce((n, r) => n + r.allow, 0)).toBe(0);
  });
});
