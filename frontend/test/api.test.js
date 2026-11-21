import assert from 'node:assert/strict';
import test from 'node:test';
import { createApi, query } from '../src/api.js';

const pair = id => ({ accessToken: 'access-' + id, refreshToken: 'refresh-' + id, user: { id } });
const okay = payload => Response.json(payload);
const denied = code => Response.json({ code, detail: 'Отказано' }, { status: 401 });

test('concurrent 401 responses share one refresh and retry with the rotated access token', async () => {
  let refreshes = 0;
  let expire;
  const gate = new Promise(resolve => { expire = resolve; });
  const api = createApi(() => {}, async (path, request) => {
    if (path.endsWith('/login')) return okay(pair(1));
    if (path.endsWith('/refresh')) {
      refreshes++;
      assert.deepEqual(JSON.parse(request.body), { refreshToken: 'refresh-1' });
      await gate;
      return okay(pair(2));
    }
    return request.headers.Authorization === 'Bearer access-2' ? okay(path) : denied('INVALID_TOKEN');
  });
  await api.login({});
  const pending = Promise.all([api.request('/cars'), api.request('/analytics')]);
  expire();
  assert.deepEqual(await pending, ['/api/v1/cars', '/api/v1/analytics']);
  assert.equal(refreshes, 1);
});

test('wrong current password preserves the session and does not rotate tokens', async () => {
  const calls = [];
  const api = createApi(() => {}, async path => {
    calls.push(path);
    return path.endsWith('/login') ? okay(pair(1)) : denied('INVALID_CREDENTIALS');
  });
  await api.login({});
  await assert.rejects(api.changePassword({}), { code: 'INVALID_CREDENTIALS' });
  assert.deepEqual(calls, ['/api/v1/auth/login', '/api/v1/auth/password']);
});

test('query encodes special characters and preserves false and zero', () => {
  assert.equal(query({ q: 'a&b', page: 0, archived: false, tag: '' }), '?q=a%26b&page=0&archived=false');
});
