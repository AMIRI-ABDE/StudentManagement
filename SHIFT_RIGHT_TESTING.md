# Shift-Right Testing: Canary Deployment, Feature Flags & Automated Rollback

## 1. Executive Summary & Architecture

This document details the implementation and verification of the **Shift-Right Testing** pipeline for the **StudentManagement** application (Spring Boot 4.1.1 + Angular 19 + Nginx + Docker Compose).

The architecture introduces:
1. **Canary Deployment (Weighted Traffic Splitting)**: Reverse-proxy Nginx splits production traffic **90% to Stable Backend (V1)** and **10% to Canary Backend (V2)**.
2. **Feature Flags with Unleash**: Dynamic runtime toggling of feature behaviors and failure injection (`simulate-failure` returning HTTP 500 and `simulate-latency` adding 3s delays on V2).
3. **Observability & Health Checks**: Spring Boot Actuator exposes health metrics for V1 and V2 independently (`/v1/actuator/health` and `/v2/actuator/health`).
4. **Load Testing with k6**: Automated verification of traffic distribution ratios and latency thresholds.
5. **Automated Rollback Controller**: A lightweight watchdog script that continuously monitors the canary backend error rate and hot-reloads Nginx to 100% V1 traffic if anomalies exceed thresholds.

```
                             Internet / Clients
                                     │
                                     ▼
                          ┌─────────────────────┐
                          │  student-nginx:80   │
                          │  (Canary Router)    │
                          └──────┬──────┬───────┘
                                 │      │
                  90% Traffic    │      │    10% Traffic
             ┌───────────────────┘      └──────────────────┐
             ▼                                             ▼
  ┌──────────────────────┐                     ┌──────────────────────┐
  │  student-backend-v1  │                     │  student-backend-v2  │
  │    (Version: v1)     │                     │   (Canary: v2)       │
  └──────────┬───────────┘                     └──────────┬───────────┘
             │                                            │
             └───────────────────┬────────────────────────┘
                                 ▼
                      ┌──────────────────────┐
                      │   student-unleash    │ ◄── Feature Flags UI (:4242)
                      │    (Feature Flags)   │
                      └──────────┬───────────┘
                                 │
                                 ▼
                      ┌──────────────────────┐
                      │  student-unleash-db  │
                      │    (PostgreSQL 15)   │
                      └──────────────────────┘
```

---

## 2. Phase-by-Phase Implementation & Verification

### Phase 1: Backend Modifications & Unit Testing

#### Implemented Changes:
1. **`backend/pom.xml`**:
   - Added `spring-boot-starter-actuator` for health check probes.
   - Added `io.getunleash:unleash-client-java:9.2.6` for feature toggle integration.
2. **`backend/src/main/resources/application.properties`**:
   - Configured Actuator endpoints exposure: `health,info` with detailed health view.
   - Configured `app.version=${APP_VERSION:v1}`.
   - Configured Unleash client settings (`unleash.api-url`, `unleash.api-token`, `unleash.app-name`, `unleash.environment`).
3. **`VersionHeaderFilter.java`**:
   - Registered `OncePerRequestFilter` with `HIGHEST_PRECEDENCE` that stamps every HTTP response with `X-Backend-Version: <version>` (`v1` or `v2`).
4. **`UnleashConfig.java`**:
   - Initialized `DefaultUnleash` bean connected to the Unleash server.
5. **`FailureInjectionFilter.java`**:
   - Filter active only on `v2` canary instances and `/api/**` paths.
   - Injects HTTP 500 when `simulate-failure` flag is active.
   - Injects 3-second delay when `simulate-latency` flag is active.
6. **`SecurityConfig.java`**:
   - Added `.requestMatchers("/actuator/**").permitAll()` to allow container health checks.
7. **`AuthController.java`**:
   - Added `@GetMapping("/health")` endpoint returning `{"status":"UP"}` for direct API verification.
8. **Unit Tests**:
   - Created `VersionHeaderFilterTest.java` and `FailureInjectionFilterTest.java`.

#### Verification Results:
- **Build & Tests**: Executed `mvn clean test`.
  - **Result**: `Tests run: 57, Failures: 0, Errors: 0, Skipped: 0`
  - **Status**: `BUILD SUCCESS` (100% tests passing).

---

### Phase 2: Canary Routing & Docker Compose Infrastructure

#### Implemented Changes:
1. **`nginx/nginx.conf`**:
   - Configured weighted upstream routing:
     ```nginx
     upstream backend {
         server backend-v1:8080 weight=9;
         server backend-v2:8080 weight=1;
     }
     ```
   - Direct Actuator bypass routes: `/v1/actuator/` -> `backend-v1:8080/actuator/`, `/v2/actuator/` -> `backend-v2:8080/actuator/`.
   - Forwarding of `X-Backend-Version` header through `proxy_pass_header X-Backend-Version;`.
2. **`nginx/nginx-rollback.conf`**:
   - Emergency rollback configuration routing 100% of traffic to `backend-v1:8080`.
3. **`docker-compose.yml`**:
   - Replaced service definitions with full 6-container topology:
     - `student-nginx` (port 80)
     - `student-frontend` (Angular SPA)
     - `student-backend-v1` (Stable, APP_VERSION=v1, memory limit 384MB)
     - `student-backend-v2` (Canary, APP_VERSION=v2, memory limit 384MB)
     - `student-unleash` (Feature flags server, port 4242)
     - `student-unleash-db` (Postgres 15 database)
   - Mounted `nginx/` directory with dynamic configuration copy to permit live rollback updates without Docker mount lockups.

#### Verification Results:
- Executed `docker-compose up -d`.
- Verified container health:
  ```
  student-backend-v1   Up (healthy)
  student-backend-v2   Up (healthy)
  student-frontend     Up
  student-nginx        Up (port 80)
  student-unleash      Up (healthy, port 4242)
  student-unleash-db   Up (healthy)
  ```
- Checked health endpoints:
  - `curl -s http://localhost/v1/actuator/health` -> `{"status":"UP"}`
  - `curl -s http://localhost/v2/actuator/health` -> `{"status":"UP"}`
  - `curl -sI http://localhost/` -> `HTTP/1.1 200 OK`

---

### Phase 3: Feature Flags Integration (Unleash)

#### Implemented Changes:
1. Configured Unleash server with Client and Admin tokens:
   - Admin token: `*:*.unleash-insecure-admin-api-token`
   - Client token: `default:development.unleash-insecure-api-token`
2. Created feature flags via Admin API:
   - `simulate-failure` (Type: release)
   - `simulate-latency` (Type: release)

#### Verification Results:
- Enabled `simulate-failure` flag:
  - Sent requests to canary:
    ```http
    HTTP/1.1 500 
    X-Backend-Version: v2
    {"error":"Simulated failure (canary test)","version":"v2"}
    ```
  - Stable `v1` backend continued serving `HTTP 200 OK` responses normally.
  - Confirmed isolation: canary failures never bleed into `v1`.

---

### Phase 4: Load Testing with k6

#### Implemented Changes:
1. **`scripts/k6-load-test.js`**:
   - Runs 5 virtual users for 30 seconds hitting the login endpoint.
   - Inspects `X-Backend-Version` header on every request.
   - Measures custom metrics: `v1_responses`, `v2_responses`, `errors`.
   - Asserts SLA thresholds: `p(95) < 2000ms`, `errors < 10%`.

#### Verification Results:
- Executed `k6` load test:
  ```
  █ THRESHOLDS 
    errors...............: ✓ 'rate<0.1' rate=0.00%
    http_req_duration....: ✓ 'p(95)<2000' p(95)=13.96ms

  █ TOTAL RESULTS 
    checks_succeeded.....: 100.00% 300 out of 300
    checks_failed........: 0.00%   0 out of 300

    CUSTOM METRICS
    errors...............: 0.00%   0 out of 150
    v1_responses.........: 90.00%  135 out of 150
    v2_responses.........: 10.00%  15 out of 150

    HTTP METRICS
    http_req_duration....: avg=11.26ms min=4.42ms med=8.82ms max=74.06ms p(95)=13.96ms
  ```
- **Conclusion**: Traffic was routed with exact mathematical precision (**90.00% V1 vs 10.00% V2**), meeting all SLA thresholds.

---

### Phase 5: Automated Rollback Controller & Failure Injection

#### Implemented Changes:
1. **`scripts/rollback-controller.sh`**:
   - Actively checks `student-backend-v2` health and test request error rate.
   - Evaluates consecutive failure threshold (`CONSECUTIVE_FAILURES=3`).
   - When triggered:
     1. Copies `nginx/nginx-rollback.conf` into Nginx configuration.
     2. Validates syntax with `nginx -t`.
     3. Hot-reloads Nginx with `nginx -s reload`.
     4. Logs all actions to `rollback.log`.
2. **`scripts/demo-shift-right.sh`**:
   - Automated interactive demonstration script testing health checks, canary traffic distribution, failure injection, and rollback detection.

#### Verification Results:
1. Activated `simulate-failure` flag in Unleash.
2. Executed `CHECK_INTERVAL=2 bash scripts/rollback-controller.sh`:
   ```
   [2026-10-26 14:34:17] Rollback controller started
   [2026-10-26 14:34:17] Config: threshold=5%, min_requests=20, consecutive=3
   [2026-10-26 14:34:18] WARNING: V2 error rate high: 100% (1/3)
   [2026-10-26 14:34:20] WARNING: V2 error rate high: 100% (2/3)
   [2026-10-26 14:34:22] WARNING: V2 error rate high: 100% (3/3)
   [2026-10-26 14:34:22] ROLLBACK TRIGGERED: Removing V2 from Nginx routing
   [2026-10-26 14:34:22] ROLLBACK COMPLETE: All traffic now routed to V1
   ```
3. Verified traffic post-rollback (while canary failure was still active):
   - Sent 30 consecutive requests to `/api/auth/health`.
   - **Result**: `30 X-Backend-Version: v1` (100% v1, 0% v2).
   - **Error Rate**: 0% errors observed by clients.
4. Restored canary:
   - Reloaded `nginx.conf`.
   - Distribution returned to 90% v1 / 10% v2.

---

## 3. Summary of Created & Modified Files

| File | Status | Description |
|---|---|---|
| `backend/pom.xml` | Modified | Added Spring Boot Actuator and Unleash client dependencies |
| `backend/src/main/resources/application.properties` | Modified | Configured Actuator endpoints, app version, and Unleash settings |
| `backend/src/main/java/ma/ac/emi/studentmanagement/config/VersionHeaderFilter.java` | Created | Adds `X-Backend-Version` header to all HTTP responses |
| `backend/src/main/java/ma/ac/emi/studentmanagement/config/UnleashConfig.java` | Created | Configures Unleash client bean |
| `backend/src/main/java/ma/ac/emi/studentmanagement/config/FailureInjectionFilter.java` | Created | Injects HTTP 500 and latency into canary V2 based on feature flags |
| `backend/src/main/java/ma/ac/emi/studentmanagement/config/SecurityConfig.java` | Modified | Permits unauthenticated access to `/actuator/**` |
| `backend/src/main/java/ma/ac/emi/studentmanagement/controller/AuthController.java` | Modified | Added `/api/auth/health` endpoint returning `{"status":"UP"}` |
| `backend/src/test/java/ma/ac/emi/studentmanagement/config/VersionHeaderFilterTest.java` | Created | Unit test for version header filter |
| `backend/src/test/java/ma/ac/emi/studentmanagement/config/FailureInjectionFilterTest.java` | Created | Unit test verifying bypass on v1, 500 injection on v2 |
| `nginx/nginx.conf` | Created | Reverse proxy config with canary routing (weight 9:1) |
| `nginx/nginx-rollback.conf` | Created | Emergency rollback config (100% traffic to v1) |
| `docker-compose.yml` | Modified | Multi-service orchestration (nginx, frontend, backend-v1, backend-v2, unleash, unleash-db) |
| `deploy-ec2.sh` | Modified | Automated EC2 deployment script with permissions and service launch |
| `scripts/rollback-controller.sh` | Created | Automated canary watchdog and rollback script |
| `scripts/k6-load-test.js` | Created | k6 script testing canary ratios, SLA thresholds, and error rates |
| `scripts/demo-shift-right.sh` | Created | Interactive end-to-end demonstration script |

---

## 4. EC2 Deployment & Operational Guide

### 1. Initial Setup on AWS EC2
```bash
# SSH into EC2 instance
ssh -i your-key.pem ubuntu@<EC2-PUBLIC-IP>

# Clone or pull the latest changes
cd ~/StudentManagement
git pull origin main

# Run the deployment script (configures swap, Docker, and launches all containers)
bash deploy-ec2.sh
```

### 2. AWS Security Group Rules
Ensure the following inbound ports are open in the EC2 Security Group:
- **Port 80**: HTTP (0.0.0.0/0) - Frontend & Canary API routing
- **Port 4242**: Unleash Feature Flags UI (Restrict to your IP only for security)
- **Port 22**: SSH (Restrict to your IP)

### 3. Unleash Feature Flag Management
1. Open `http://<EC2-PUBLIC-IP>:4242` in your browser.
2. Log in using the default Unleash credentials:
   - **Username**: `admin`
   - **Password**: `unleash4all`
   *(Unleash will ask you to change/update your password upon initial login)*
3. **API Tokens (Configured in docker-compose)**:
   - **Admin API Token**: `*:*.unleash-insecure-admin-api-token`
   - **Client API Token**: `default:development.unleash-insecure-api-token`
4. The pre-configured feature flags `simulate-failure` and `simulate-latency` are available in project `default`.
5. Toggle them ON/OFF under the `development` environment to test resilience.

### 4. Running Load Tests
Run k6 locally or from another machine against the EC2 instance:
```bash
# Using Docker
docker run --rm -i --network="host" -v "$PWD/scripts:/scripts" grafana/k6 run \
  -e BASE_URL=http://<EC2-PUBLIC-IP> /scripts/k6-load-test.js

# Or using native k6 binary
k6 run -e BASE_URL=http://<EC2-PUBLIC-IP> scripts/k6-load-test.js
```

### 5. Running the Automated Rollback Watchdog
On EC2, start the controller in a persistent terminal (or tmux/systemd service):
```bash
cd ~/StudentManagement
bash scripts/rollback-controller.sh
```
When failure rates exceed 5% across 3 consecutive checks, the controller will automatically re-route 100% of traffic to V1.

### 6. Restoring Canary After Rollback
Once the issue in V2 is resolved:
```bash
docker cp nginx/nginx.conf student-nginx:/etc/nginx/conf.d/default.conf
docker exec student-nginx nginx -s reload
```
