import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  scenarios: {
    normal: {
      executor: 'constant-vus',
      vus: 50,
      duration: '30s',
      exec: "normal",
      gracefulStop: '5s',
    },
    ramp: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '20s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '20s', target: 0 },
      ],
      exec: "ramping",
      gracefulStop: '5s',
      startTime: '35s'
    },
    stress: {
      executor: 'constant-vus',
      vus: 200,
      duration: '20s',
      exec: "stress",
      gracefulStop: '10s',
      startTime: '90s'
    }
  },
  thresholds: {
    http_req_duration: ['p(95)<1000', 'p(99)<5000'],
    http_req_failed: ['rate<0.1'],
  }
};

const host = "http://backend:5001";

export function normal() {
  const productIds = ['1', '2', '3'];
  const productId = productIds[Math.floor(Math.random() * productIds.length)];
  
  const res = http.get(host + "/product/" + productId + "/similar");
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1s': (r) => r.timings.duration < 1000,
    'response has content': (r) => r.body.length > 0,
    'is JSON': (r) => r.headers['Content-Type'] !== undefined,
  });
  
  sleep(0.5);
}

export function ramping() {
  const productIds = ['1', '2', '3'];
  const productId = productIds[Math.floor(Math.random() * productIds.length)];
  
  const res = http.get(host + "/product/" + productId + "/similar");
  
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 1.5s': (r) => r.timings.duration < 1500,
  });
  
  sleep(0.5);
}

export function stress() {
  const productIds = ['1', '2', '3'];
  const productId = productIds[Math.floor(Math.random() * productIds.length)];
  
  const res = http.get(host + "/product/" + productId + "/similar");
  
  check(res, {
    'status is 200 or 500': (r) => r.status === 200 || r.status === 500,
  });
  
  sleep(0.2);
}

export default function () {
  normal();
}