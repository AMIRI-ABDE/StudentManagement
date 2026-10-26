import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const v1Count = new Rate('v1_responses');
const v2Count = new Rate('v2_responses');

// Configuration: adjust BASE_URL to your EC2 public IP
const BASE_URL = __ENV.BASE_URL || 'http://localhost';

export const options = {
    scenarios: {
        // Scenario 1: Normal traffic
        normal_traffic: {
            executor: 'constant-vus',
            vus: 5,
            duration: '30s',
            exec: 'normalTraffic',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'],  // 95% of requests under 2s
        errors: ['rate<0.1'],               // less than 10% errors
    },
};

// Normal traffic: hit the API endpoints
export function normalTraffic() {
    // Test auth endpoint (public)
    const loginRes = http.post(`${BASE_URL}/api/auth/login`, JSON.stringify({
        email: 'test@test.com',
        password: 'password123'
    }), {
        headers: { 'Content-Type': 'application/json' },
    });

    // Track which backend version handled the request
    const version = loginRes.headers['X-Backend-Version'];
    if (version === 'v1') {
        v1Count.add(1);
        v2Count.add(0);
    } else if (version === 'v2') {
        v1Count.add(0);
        v2Count.add(1);
    }

    // Track errors
    const isError = loginRes.status >= 500;
    errorRate.add(isError);

    check(loginRes, {
        'status is not 500': (r) => r.status !== 500,
        'has version header': (r) => r.headers['X-Backend-Version'] !== undefined,
    });

    sleep(1);
}

// Scenario 2: Ramp up load (use with --scenario ramp_up)
export function rampUpTraffic() {
    const res = http.get(`${BASE_URL}/api/courses`);

    const version = res.headers['X-Backend-Version'];
    if (version === 'v1') {
        v1Count.add(1);
    } else if (version === 'v2') {
        v2Count.add(1);
    }

    errorRate.add(res.status >= 500);

    check(res, {
        'status is 200 or 401': (r) => r.status === 200 || r.status === 401,
    });

    sleep(0.5);
}
