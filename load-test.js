import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const jobE2ELatency = new Trend('job_e2e_latency_ms');

export const options = {
    vus: 20,
    duration: '30s',
};

export default function () {
    const BASE_URL = 'http://host.docker.internal:3000';

    const payload = JSON.stringify({
        job_name: 'k6-load-test',
        job_type: 'send_email',
        max_retries: 3,
        jobs_args: { from: 'test@k6.io', to: 'target@k6.io' },
    });

    const headers = { 'Content-Type': 'application/json' };

    // 1. Submit job
    const postRes = http.post(`${BASE_URL}/addJob`, payload, { headers });
    check(postRes, { 'job posted': (r) => r.status === 200 });

    const jobId = postRes.json('id');
    if (!jobId) return;

    // 2. Poll until completed or dead
    const startTime = Date.now();
    let completed = false;

    for (let i = 0; i < 35; i++) {
        sleep(1);
        const pollRes = http.get(`${BASE_URL}/getJobById?id=${jobId}`);
        const status = pollRes.json('status');

        if (status === 'completed' || status === 'dead') {
            completed = true;
            break;
        }
    }

    // 3. Record end-to-end latency
    jobE2ELatency.add(Date.now() - startTime);

    check(null, { 'job completed': () => completed });
}
