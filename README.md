# 🛡️ Sentinel — Enterprise API Abuse & Bot Detection Engine

Sentinel is a high-performance, distributed backend middleware system built with **Spring Boot** and **Redis**. It detects API abuse, automated clients, and suspicious request patterns using deterministic, algorithmic techniques.

Operating at the servlet filter level, Sentinel intercepts traffic before it reaches your core business logic. It constructs privacy-safe client fingerprints, calculates behavioral entropy, and leverages **Atomic Redis Lua Scripts** to evaluate risk and block malicious requests in real-time without introducing network latency.

---

## 🚀 The Request Pipeline

1. **Intercept** → `OncePerRequestFilter` catches traffic before the Controller.
2. **Fingerprint** → Generates a deterministic device ID using passive HTTP headers.
3. **Sliding Window** → Redis Lua script counts requests atomically.
4. **Entropy Analysis** → Evaluates robotic timing and repetitive endpoints.
5. **Risk Engine** → Computes a multi-signal score.
6. **Action** → Blocks threats (`429 Too Many Requests`) or allows traffic.

---

## 🧠 Core Engineering Achievements

### 1. Atomic Rate Limiting (Redis + Lua)
Traditional rate limiters suffer from read-modify-write race conditions under heavy concurrent load. Sentinel solves this by shifting the Sliding Window algorithm into a **single Redis Lua script**. Because Redis is single-threaded, the script executes atomically, making it mathematically impossible for attackers to bypass the limit via concurrent flooding.

### 2. Behavioral Entropy Optimization (Solving N+1 Latency)
To detect bots, Sentinel tracks the time-gaps between requests. Instead of making 8 separate network round-trips to Redis to manage lists and TTLs, Sentinel consolidates the payload (`timestamp::endpoint`) and executes the push, trim, and fetch operations inside a **pipelined Lua script**, reducing network latency to a single round-trip.

### 3. Smart, Cross-Endpoint Fingerprinting
Simple bots easily spoof `User-Agent` strings. Sentinel combats this by hashing a combination of passive headers (`Accept-Language`, `Accept-Encoding`) and modern Client Hints (`Sec-CH-UA`). Furthermore, the request URI is deliberately *excluded* from the fingerprint, preventing attackers from bypassing limits by rotating their attacks across different endpoints.

### 4. Integration Testing with Testcontainers
Sentinel's Redis logic is fully covered by automated integration tests using **Testcontainers**. During `mvn test`, a real Redis Docker container is spun up dynamically to verify the Lua scripts against an actual database, guaranteeing production reliability.

---

## 🛠️ Technology Stack

* **Language:** Java 17
* **Framework:** Spring Boot 3.4
* **Data Store:** Redis (via Spring Data Redis)
* **Performance:** Lua Scripting (Atomic Operations)
* **Testing:** JUnit 5, Testcontainers
* **Deployment:** Docker & Docker Compose

---

## 🚦 Getting Started

Sentinel is fully containerized. You do not need to install Redis or Java on your local machine to run it.

### 1. Spin up the environment
```bash
docker-compose up --build
