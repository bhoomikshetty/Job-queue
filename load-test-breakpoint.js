import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const jobE2ELatency = new Trend('job_e2e_latency_ms');
const completionRate = new Rate('job_completion_rate');

// Ramp up VUs gradually until the system breaks
export const options = {
    stages: [
        { duration: '30s', target: 20  },  // warm up
        { duration: '30s', target: 50  },  // increase
        { duration: '30s', target: 100 },  // push harder
        { duration: '30s', target: 200 },  // stress
        { duration: '30s', target: 300 },  // break point?
        { duration: '30s', target: 0   },  // cool down
    ],
    thresholds: {
        // alert if HTTP p95 exceeds 1s — system is struggling
        'http_req_duration{expected_response:true}': ['p(95)<1000'],
        // alert if fewer than 70% of jobs complete
        'job_completion_rate': ['rate>0.7'],
    },
};

export default function () {
    const BASE_URL = 'http://host.docker.internal:3000';

    const payload = JSON.stringify({
        job_name: 'breakpoint-test',
        job_type: 'send_email',
        max_retries: 1,
        jobs_args: { from: 'test@k6.io', to: 'target@k6.io' },
    });

    const headers = { 'Content-Type': 'application/json' };

    const postRes = http.post(`${BASE_URL}/addJob`, payload, { headers });
    check(postRes, { 'job posted': (r) => r.status === 200 });

    const jobId = postRes.json('id');
    if (!jobId) return;

    const startTime = Date.now();
    let completed = false;

    for (let i = 0; i < 15; i++) {
        sleep(1);
        const pollRes = http.get(`${BASE_URL}/getJobById?id=${jobId}`);
        const status = pollRes.json('status');

        if (status === 'completed' || status === 'dead') {
            completed = true;
            break;
        }
    }

    jobE2ELatency.add(Date.now() - startTime);
    completionRate.add(completed);
}
