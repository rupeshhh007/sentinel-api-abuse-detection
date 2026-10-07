export type Signal = {
  name: string;
  value: number;
  points: number;
  evidence: string;
};
export type Decision = {
  id: string;
  timestamp: number;
  client: string;
  source: string;
  endpoint: string;
  method: string;
  score: number;
  action: string;
  origin: string;
  latencyMs: number;
  samples: number;
  policyVersion: number;
  policy: Policy;
  signals: Signal[];
};
export type Policy = {
  version: number;
  mode: string;
  maxRequests: number;
  sourceMaxRequests: number;
  windowSeconds: number;
  threshold: number;
  failureMode: string;
};
export type Overview = {
  telemetry: {
    total: number;
    blocked: number;
    observed: number;
    degraded: number;
    p95Ms: number;
    events: Decision[];
    retention: number;
    origin: string;
  };
  policy: Policy;
  storage: string;
  engineHealthy: boolean;
  scope: string;
};
export type Scenario = {
  id: string;
  name: string;
  description: string;
  requests: number;
  malicious: boolean;
};
export type Replay = {
  scenario: string;
  requests: number;
  blocked: number;
  observed: number;
  suspicious: number;
  firstDetection: number;
  decisionMs: number;
  policy: Policy;
  decisions: Decision[];
};
export type Evaluation = {
  truePositive: number;
  trueNegative: number;
  falsePositive: number;
  falseNegative: number;
  note: string;
  policyVersion: number;
  policy: Policy;
  scenarios: {
    scenario: string;
    malicious: boolean;
    detected: boolean;
    firstDetection: number;
  }[];
};
