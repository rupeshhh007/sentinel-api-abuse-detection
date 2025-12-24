# Sentinel — Intelligent API Abuse & Bot Detection Engine

Sentinel is a backend middleware system built using Spring Boot to detect API abuse, automated clients, and suspicious request patterns using deterministic, algorithmic techniques.

It operates before application business logic, observes request behavior, constructs privacy-safe client fingerprints, and computes explainable risk scores.

---

## Overview

For every incoming HTTP request, Sentinel:

- Intercepts traffic at the servlet filter level
- Extracts behavioral signals (IP address, User-Agent, endpoint, timing)
- Generates a stable fingerprint without authentication or cookies
- Tracks request frequency using a sliding window algorithm
- Analyzes behavioral entropy based on timing and endpoint repetition
- Computes a multi-signal risk score
- Produces structured, human-readable logs
- Supports safe rollout using LOG_ONLY mode

---

## Request Processing Pipeline

Client Request
↓
OncePerRequestFilter (Sentinel)
↓
Signal Extraction
↓
Fingerprinting Engine
↓
Sliding Window Rate Detection
↓
Behavioral Entropy Analysis
↓
Risk Scoring Engine
↓
Decision (LOG_ONLY / BLOCK)
↓
Controller / Business Logic



Sentinel executes before controllers, ensuring abusive behavior is detected before reaching core application code.

---

## Core Components

### 1. Infrastructure-Level Interception

- Implemented using `OncePerRequestFilter`
- Executes once per HTTP request
- Filters only `DispatcherType.REQUEST`
- Prevents duplication across controllers

---

### 2. Privacy-Safe Fingerprinting

- No authentication required
- No cookies or personally identifiable information stored
- Fingerprint generated as a SHA-256 hash of:

normalized IP + User-Agent + endpoint



- Stable across requests
- Partially resistant to basic IP rotation

---

### 3. Sliding Window Rate Detection

- Tracks request timestamps per fingerprint
- Data structure used:

Map<Fingerprint, Deque<Long>>



- Detects burst traffic reliably
- Avoids fixed-window boundary bypass
- Uses thread-safe concurrent collections

---

### 4. Behavioral Entropy Analysis

Sentinel evaluates how predictable a client’s behavior is.

Signals analyzed:
- Time-gap entropy (fixed or near-fixed request intervals)
- Endpoint sequence entropy (repeated access to the same endpoint)

Entropy levels:
- LOW
- MEDIUM
- HIGH
- UNKNOWN

Automated clients tend to converge toward LOW entropy even when rate-limited.

---

### 5. Risk Scoring Engine

Rather than hard blocking rules, Sentinel uses weighted scoring.

| Signal Detected     | Score |
|---------------------|-------|
| Rate abuse detected | +40   |
| LOW entropy         | +50   |
| MEDIUM entropy      | +20   |

Example output:

[SENTINEL][RISK] score=90
reasons=[High request rate detected, Highly repetitive behavior detected]



---

## Example Logs

IP=127.0.0.1 | UA=curl/8.7.1 | URI=/ping
FP=4a033ca9e2b7
[SENTINEL][ENTROPY] Low entropy detected
[SENTINEL][RATE] Suspicious activity detected
[SENTINEL][RISK] score=90
reasons=[High request rate detected, Highly repetitive behavior detected]



---

## Design Principles

- Defense in depth using multiple weak signals
- Explainability over opaque decision-making
- Safe rollout via observation-first deployment
- Algorithmic approaches over heuristics
- Production-inspired system boundaries

---

## Technology Stack

- Java 17
- Spring Boot
- Servlet Filters
- Maven
- Concurrent data structures
- No machine learning
- No external security dependencies

---

## Running the Application

mvn spring-boot:run



---

## Testing

Normal request:

curl http://localhost:8080/ping



Simulated automated behavior:

for i in {1..50}; do
curl -H "User-Agent: curl/8.7.1" http://localhost:8080/ping
done


---

## Current Mode

LOG_ONLY



Requests are not blocked yet. The system currently logs detections to allow safe evaluation before enforcement.

---

## Possible Extensions

- Enable BLOCK mode
- Redis-backed distributed rate limiting
- Endpoint normalization
- Administrative dashboard
- Spring Security integration
- Per-endpoint risk policies

---

## What This Project Demonstrates

- Backend systems design
- Algorithmic problem solving
- Concurrency handling
- Security-oriented thinking
- Real-world engineering tradeoffs
