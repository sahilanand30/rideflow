import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 20,
  iterations: 100,
};

export default function () {
  const payload = JSON.stringify({
    riderId: 1,
    pickupLat: 12.9716,
    pickupLng: 77.5946,
    dropLat: 12.9352,
    dropLng: 77.6245,
  });

  const res = http.post('http://host.docker.internal:8080/trips', payload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, { 'status is 201': (r) => r.status === 201 });
}