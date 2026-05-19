import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PRODUCT_ID = __ENV.PRODUCT_ID || '11111111-1111-1111-1111-111111111111';
const USER_ID = __ENV.USER_ID || '00000000-0000-0000-0000-000000000001';
const SHIPPING_ADDRESS = __ENV.SHIPPING_ADDRESS || 'Calle 123 #45-67';
const SHIPPING_CITY = __ENV.SHIPPING_CITY || 'Bogota';

http.setResponseCallback(http.expectedStatuses(200, 409));

export const options = {
  scenarios: {
    concurrent_checkout: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 20),
      duration: __ENV.DURATION || '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
  },
};

export default function () {
  const key = `k6-${__VU}-${__ITER}-${Date.now()}`;
  const payload = JSON.stringify({
    items: [{ productId: PRODUCT_ID, quantity: 1 }],
    shippingAddress: SHIPPING_ADDRESS,
    shippingCity: SHIPPING_CITY,
  });
  const res = http.post(`${BASE_URL}/api/checkout`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'Idempotency-Key': key,
      'X-User-Id': USER_ID,
      'X-Roles': 'CLIENT',
    },
  });
  check(res, {
    'checkout accepted or conflict': r => [200, 409].includes(r.status),
    'no duplicate key error': r => !r.body.includes('duplicate key'),
  });
}
