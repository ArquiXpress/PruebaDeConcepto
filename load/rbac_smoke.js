import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {
  const payload = JSON.stringify({ name: 'Black Friday', startsAt: '2026-11-01T00:00:00Z', endsAt: '2026-11-30T23:59:59Z' });
  const denied = http.post(`${BASE_URL}/api/admin/promotions`, payload, {
    headers: { 'Content-Type': 'application/json', 'X-Roles': 'CLIENT' },
  });
  const allowed = http.post(`${BASE_URL}/api/admin/promotions`, payload, {
    headers: { 'Content-Type': 'application/json', 'X-Roles': 'ADMIN' },
  });
  check(denied, { 'client denied': r => r.status === 403 });
  check(allowed, { 'admin allowed': r => r.status === 200 });
}
