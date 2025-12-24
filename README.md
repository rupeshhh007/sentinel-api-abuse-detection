🛡️ Sentinel — Intelligent API Abuse & Bot Detection Engine

Sentinel is a backend middleware system built with Spring Boot that detects API abuse, bots, and suspicious behavior patterns using algorithmic analysis, not CAPTCHAs or machine learning.

It operates before business logic, observes request behavior, builds privacy-safe identities, and generates explainable risk scores — similar in spirit to how real-world trust systems (Cloudflare, Stripe, Google) are designed.What Sentinel Does (High Level)

For every incoming HTTP request, Sentinel:

Intercepts the request at infrastructure level

Extracts behavioral signals (IP, User-Agent, endpoint, timing)

Generates a stable fingerprint (no auth, no cookies)

Tracks request frequency using a sliding window algorithm

Analyzes behavioral entropy (time gaps + endpoint repetition)

Computes a multi-signal risk score

Produces human-readable, explainable logs

Allows safe rollout via LOG_ONLY mode

🧩 System Architecture
Client Request
      ↓
OncePerRequestFilter  (Sentinel)
      ↓
Signal Extraction
      ↓
Fingerprinting Engine
      ↓
Rate Detection (Sliding Window)
      ↓
Entropy Analysis (Behavior Patterns)
      ↓
Risk Scoring Engine
      ↓
Decision (LOG_ONLY / BLOCK)
      ↓
Controller / Business Logic


Sentinel runs before controllers, meaning abuse is detected before it reaches core APIs.

🔐 Core Concepts Implemented
1️⃣ Infrastructure-Level Interception

Uses OncePerRequestFilter

Filters only real client requests (DispatcherType.REQUEST)

No duplication across controllers

2️⃣ Privacy-Safe Fingerprinting

No authentication required

No cookies or PII stored

Fingerprint = SHA-256 hash of:

normalized IP + User-Agent + endpoint


Stable across requests

Resistant to IP rotation

3️⃣ Sliding Window Rate Detection (DSA-based)

Tracks request timestamps per fingerprint

Uses:

Map<Fingerprint, Deque<Timestamps>>


Detects bursts reliably (no fixed-window bypass)

Thread-safe design

4️⃣ Behavioral Entropy Analysis (Advanced)

Sentinel detects how predictable a client is:

Time-gap entropy → bots send requests at fixed intervals

Endpoint-sequence entropy → bots repeat same endpoint

Entropy levels:

LOW / MEDIUM / HIGH / UNKNOWN


Bots tend to converge to LOW entropy even when rate-limited.

5️⃣ Multi-Signal Risk Scoring (Explainable)

Instead of hard rules, Sentinel uses weighted scoring:

Signal	Score
Rate abuse detected	+40
LOW entropy	+50
MEDIUM entropy	+20

Example output:

[SENTINEL][RISK] score=90
reasons=[High request rate detected, Highly repetitive behavior detected]


This makes Sentinel:

Transparent

Debuggable

Interview-friendly

🧪 Example Logs
IP=127.0.0.1 | UA=curl/8.7.1 | URI=/ping
FP=4a033ca9e2b7 | IP=127.0.0.1 | URI=/ping
[SENTINEL][ENTROPY] Low entropy detected
[SENTINEL][RATE] Suspicious activity detected
[SENTINEL][RISK] score=90
reasons=[High request rate detected, Highly repetitive behavior detected]

🧠 Design Principles

Defense in depth (multiple weak signals → strong decision)

Explainability over black boxes

Safe rollout first (LOG_ONLY)

Algorithmic > heuristic

Production-inspired architecture

⚙️ Tech Stack

Java 17

Spring Boot

Servlet Filters

Maven

Concurrent data structures

No ML, no external dependencies

🧪 Testing Sentinel (curl)

Normal request:

curl http://localhost:8080/ping


Bot-like behavior:

for i in {1..50}; do
  curl -H "User-Agent: curl/8.7.1" http://localhost:8080/ping
done

🚧 Current Mode
LOG_ONLY


No requests are blocked yet.
This mirrors how real production security systems are deployed initially.

🔮 Possible Extensions

Switch to BLOCK mode

Redis-backed distributed rate limiting

Endpoint normalization

Admin dashboard

Integration with Spring Security

Per-endpoint risk policies

💼 What This Project Demonstrates

Backend systems thinking

Algorithmic problem solving

Concurrency awareness

Security-first design

Real-world engineering tradeoffs
