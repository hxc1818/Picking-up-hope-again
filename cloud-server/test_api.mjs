import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { startServer } from './server.js';

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'picking-hope-cloud-test-'));

async function req(baseUrl, method, pathname, body = null, token = null) {
  const headers = {};
  if (body !== null) headers['Content-Type'] = 'application/json';
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${baseUrl}${pathname}`, {
    method,
    headers,
    body: body !== null ? JSON.stringify(body) : undefined
  });
  const json = await res.json();
  return { status: res.status, json };
}

let serverObj;
try {
  serverObj = await startServer({ host: '127.0.0.1', port: 0, dataDir: tmpDir, quiet: true });
  const baseUrl = `http://127.0.0.1:${serverObj.address.port}`;

  // 1. Health check
  const health = await req(baseUrl, 'GET', '/api/health');
  assert.equal(health.status, 200);
  assert.equal(health.json.ok, true);
  assert.equal(health.json.service, 'picking-hope-cloud');

  // 2. Register
  const reg = await req(baseUrl, 'POST', '/api/auth/register', {
    username: 'LinYe_01',
    password: 'password123',
    displayName: '林野测试号'
  });
  assert.equal(reg.status, 201);
  assert.equal(reg.json.ok, true);
  assert.equal(reg.json.account.id, 'linye_01');
  assert.equal(reg.json.account.displayName, '林野测试号');
  assert.equal(reg.json.hasSave, false);
  assert.ok(reg.json.token.length >= 32);

  // 3. Duplicate register -> 409
  const dup = await req(baseUrl, 'POST', '/api/auth/register', {
    username: 'linye_01',
    password: 'password123'
  });
  assert.equal(dup.status, 409);
  assert.equal(dup.json.ok, false);

  // 4. Wrong password -> 401
  const badLogin = await req(baseUrl, 'POST', '/api/auth/login', {
    username: 'LinYe_01',
    password: 'wrong'
  });
  assert.equal(badLogin.status, 401);
  assert.equal(badLogin.json.ok, false);

  // 5. Login success
  const login = await req(baseUrl, 'POST', '/api/auth/login', {
    username: 'linye_01',
    password: 'password123'
  });
  assert.equal(login.status, 200);
  assert.equal(login.json.ok, true);
  const token = login.json.token;

  // 6. Verify /api/auth/me
  const me = await req(baseUrl, 'GET', '/api/auth/me', null, token);
  assert.equal(me.status, 200);
  assert.equal(me.json.account.username, 'LinYe_01');
  assert.equal(me.json.hasSave, false);

  // 7. Empty save pull
  const pullEmpty = await req(baseUrl, 'GET', '/api/save/pull', null, token);
  assert.equal(pullEmpty.status, 200);
  assert.equal(pullEmpty.json.hasSave, false);
  assert.equal(pullEmpty.json.save, null);

  // 8. Push save with Chinese characters and equipment payload
  const sampleSummary = {
    playerLevel: 12,
    worldLevel: 1,
    questStage: 15,
    questTitle: '【冰原坠毁】',
    activePartyNames: ['检修员·林野', '巡检员·陈风', '医疗员·苏晴'],
    starTicketsCount: 20
  };
  const samplePayload = {
    teamNbtBase64: 'H4sIAAAAAAAA/2NgGAWjYBSMglEwCkYBBgAAAP//AwA=',
    inventoryNbtBase64: 'H4sIAAAAAAAA/2NgGAWjYBSMglEwCkYBBgAAAP//AwA=',
    questNbtBase64: 'H4sIAAAAAAAA/2NgGAWjYBSMglEwCkYBBgAAAP//AwA=',
    playerTags: ['picking_hope_v2_clean_init_v4', 'hope_crate_7_61_10'],
    posX: 114.5,
    posY: 61.0,
    posZ: 28.5,
    yaw: 270.0,
    pitch: 0.0,
    health: 20.0
  };

  const push1 = await req(baseUrl, 'POST', '/api/save/push', {
    schemaVersion: 1,
    reason: 'quest',
    summary: sampleSummary,
    payload: samplePayload
  }, token);
  assert.equal(push1.status, 200);
  assert.equal(push1.json.ok, true);
  assert.equal(push1.json.revision, 1);

  // 9. Pull save and verify field equality
  const pull1 = await req(baseUrl, 'GET', '/api/save/pull', null, token);
  assert.equal(pull1.status, 200);
  assert.equal(pull1.json.hasSave, true);
  assert.equal(pull1.json.save.revision, 1);
  assert.deepEqual(pull1.json.save.summary, sampleSummary);
  assert.deepEqual(pull1.json.save.payload, samplePayload);

  // 10. Push second revision and verify revision == 2
  const push2 = await req(baseUrl, 'POST', '/api/save/push', {
    schemaVersion: 1,
    reason: 'logout',
    summary: { ...sampleSummary, questStage: 18 },
    payload: { ...samplePayload, posX: 131.5 }
  }, token);
  assert.equal(push2.status, 200);
  assert.equal(push2.json.revision, 2);

  // 11. Logout invalidates token
  const logout = await req(baseUrl, 'POST', '/api/auth/logout', {}, token);
  assert.equal(logout.status, 200);
  assert.equal(logout.json.ok, true);

  const meAfterLogout = await req(baseUrl, 'GET', '/api/auth/me', null, token);
  assert.equal(meAfterLogout.status, 401);

  console.log('ALL CLOUD SERVER API TESTS PASSED (11/11)');
} finally {
  if (serverObj?.server) {
    await new Promise((r) => serverObj.server.close(r));
  }
  fs.rmSync(tmpDir, { recursive: true, force: true });
}
