import http from 'k6/http';
import { check } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 100);
const DURATION = __ENV.DURATION || '45s';
const categories = ['tecnologia', 'gaming', 'belleza', 'auto', 'hogar', 'deportes', 'moda', 'juguetes'];

export const options = {
  scenarios: {
    catalog_reads: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000'],
  },
};

export default function () {
  const page = (__VU * 17 + __ITER * 13) % 80;
  const size = 2 + ((__VU + __ITER) % 4);
  const category = categories[(__VU + __ITER) % categories.length];
  const useCategory = (__ITER % 3) !== 0;
  const url = useCategory
    ? `${BASE_URL}/api/products?category=${category}&page=${page}&size=${size}`
    : `${BASE_URL}/api/products?page=${page}&size=${size}`;

  const res = http.get(url);
  check(res, {
    'catalog status 200': r => r.status === 200,
    'catalog returns pageable body': r => Boolean(r.body && r.body.includes('totalElements')),
  });
}
