import http from 'node:http';
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const MAX_BODY_BYTES = 2 * 1024 * 1024;
const TOKEN_TTL_MS = 30 * 24 * 60 * 60 * 1000;
const USERNAME_REGEX = /^[a-zA-Z0-9_]{3,24}$/;

function createDatabase(dataDir) {
  fs.mkdirSync(dataDir, { recursive: true });
  const dbPath = path.join(dataDir, 'db.json');

  let state = { accounts: [], tokens: {} };
  if (fs.existsSync(dbPath)) {
    try {
      const raw = fs.readFileSync(dbPath, 'utf8');
      const parsed = JSON.parse(raw);
      if (parsed && Array.isArray(parsed.accounts) && typeof parsed.tokens === 'object') {
        state = parsed;
      }
    } catch (err) {
      console.error('[CloudDB] Failed to load existing db.json, starting empty:', err.message);
    }
  }

  function pruneExpiredTokens() {
    const now = Date.now();
    for (const [tok, info] of Object.entries(state.tokens)) {
      if (!info || typeof info.expiresAt !== 'number' || info.expiresAt <= now) {
        delete state.tokens[tok];
      }
    }
  }

  function persist() {
    pruneExpiredTokens();
    const tmpPath = dbPath + '.tmp';
    fs.writeFileSync(tmpPath, JSON.stringify(state, null, 2), 'utf8');
    fs.renameSync(tmpPath, dbPath);
  }

  pruneExpiredTokens();
  return { state, persist };
}

function hashPassword(password, saltHex) {
  return crypto.scryptSync(password, saltHex, 64).toString('hex');
}

function verifyPassword(password, saltHex, expectedHashHex) {
  const actualBuf = Buffer.from(hashPassword(password, saltHex), 'hex');
  const expectedBuf = Buffer.from(expectedHashHex, 'hex');
  if (actualBuf.length !== expectedBuf.length) return false;
  return crypto.timingSafeEqual(actualBuf, expectedBuf);
}

function sanitizeAccountPublic(acc) {
  return {
    id: acc.id,
    username: acc.username,
    displayName: acc.displayName
  };
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let totalBytes = 0;
    const chunks = [];
    req.on('data', (chunk) => {
      totalBytes += chunk.length;
      if (totalBytes > MAX_BODY_BYTES) {
        const err = new Error('请求体超出 2MB 限制');
        err.statusCode = 413;
        reject(err);
        req.destroy();
        return;
      }
      chunks.push(chunk);
    });
    req.on('end', () => {
      if (chunks.length === 0) {
        resolve({});
        return;
      }
      try {
        const raw = Buffer.concat(chunks).toString('utf8');
        const parsed = JSON.parse(raw);
        if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
          const err = new Error('JSON 请求体必须是对象');
          err.statusCode = 400;
          reject(err);
          return;
        }
        resolve(parsed);
      } catch {
        const err = new Error('JSON 格式无效');
        err.statusCode = 400;
        reject(err);
      }
    });
    req.on('error', (err) => reject(err));
  });
}

function sendJson(res, statusCode, data) {
  const body = JSON.stringify(data);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
  });
  res.end(body);
}

function extractBearerToken(req) {
  const auth = req.headers['authorization'];
  if (!auth || typeof auth !== 'string') return null;
  if (!auth.startsWith('Bearer ')) return null;
  const token = auth.slice(7).trim();
  return token.length > 0 ? token : null;
}

export function startServer(options = {}) {
  const host = options.host || process.env.HOST || '127.0.0.1';
  const port = options.port !== undefined ? Number(options.port) : Number(process.env.PORT || 3847);
  const dataDir = options.dataDir || path.join(__dirname, 'data');
  const quiet = Boolean(options.quiet);

  const db = createDatabase(dataDir);

  function authenticate(req) {
    const token = extractBearerToken(req);
    if (!token) return null;
    const entry = db.state.tokens[token];
    if (!entry || entry.expiresAt <= Date.now()) {
      if (entry) {
        delete db.state.tokens[token];
        db.persist();
      }
      return null;
    }
    const acc = db.state.accounts.find((a) => a.id === entry.accountId);
    if (!acc) return null;
    return { token, account: acc };
  }

  function issueToken(accountId) {
    const token = crypto.randomBytes(32).toString('hex');
    const now = Date.now();
    db.state.tokens[token] = {
      accountId,
      createdAt: now,
      expiresAt: now + TOKEN_TTL_MS
    };
    return token;
  }

  const server = http.createServer(async (req, res) => {
    if (req.method === 'OPTIONS') {
      res.writeHead(204, {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Headers': 'Content-Type, Authorization',
        'Access-Control-Allow-Methods': 'GET, POST, OPTIONS'
      });
      res.end();
      return;
    }

    const parsedUrl = new URL(req.url || '/', `http://${host}`);
    const pathname = parsedUrl.pathname;

    try {
      if (req.method === 'GET' && pathname === '/api/health') {
        sendJson(res, 200, {
          ok: true,
          service: 'picking-hope-cloud',
          version: '1.0.0',
          time: Date.now()
        });
        return;
      }

      if (req.method === 'POST' && pathname === '/api/auth/register') {
        const body = await readJsonBody(req);
        const username = typeof body.username === 'string' ? body.username.trim() : '';
        const password = typeof body.password === 'string' ? body.password : '';
        const displayRaw = typeof body.displayName === 'string' ? body.displayName.trim() : '';

        if (!USERNAME_REGEX.test(username)) {
          sendJson(res, 400, { ok: false, error: '用户名需为 3~24 位字母、数字或下划线' });
          return;
        }
        if (password.length < 4 || password.length > 128) {
          sendJson(res, 400, { ok: false, error: '密码长度需在 4~128 位之间' });
          return;
        }

        const exists = db.state.accounts.some(
          (a) => a.username.toLowerCase() === username.toLowerCase()
        );
        if (exists) {
          sendJson(res, 409, { ok: false, error: '该用户名已被注册' });
          return;
        }

        const now = Date.now();
        const salt = crypto.randomBytes(16).toString('hex');
        const account = {
          id: username.toLowerCase(),
          username,
          displayName: (displayRaw || username).slice(0, 24),
          salt,
          passwordHash: hashPassword(password, salt),
          createdAt: now,
          updatedAt: now,
          save: null,
          history: []
        };
        db.state.accounts.push(account);
        const token = issueToken(account.id);
        db.persist();

        if (!quiet) console.log(`[Auth] Registered account: ${account.username}`);
        sendJson(res, 201, {
          ok: true,
          account: sanitizeAccountPublic(account),
          token,
          hasSave: false,
          saveSummary: null
        });
        return;
      }

      if (req.method === 'POST' && pathname === '/api/auth/login') {
        const body = await readJsonBody(req);
        const username = typeof body.username === 'string' ? body.username.trim() : '';
        const password = typeof body.password === 'string' ? body.password : '';

        const acc = db.state.accounts.find(
          (a) => a.username.toLowerCase() === username.toLowerCase()
        );
        if (!acc || !verifyPassword(password, acc.salt, acc.passwordHash)) {
          sendJson(res, 401, { ok: false, error: '用户名或密码错误' });
          return;
        }

        const token = issueToken(acc.id);
        db.persist();

        if (!quiet) console.log(`[Auth] Login success: ${acc.username}`);
        sendJson(res, 200, {
          ok: true,
          account: sanitizeAccountPublic(acc),
          token,
          hasSave: Boolean(acc.save),
          saveSummary: acc.save ? acc.save.summary || null : null
        });
        return;
      }

      if (req.method === 'GET' && pathname === '/api/auth/me') {
        const session = authenticate(req);
        if (!session) {
          sendJson(res, 401, { ok: false, error: '登录已过期或凭证无效' });
          return;
        }
        const acc = session.account;
        sendJson(res, 200, {
          ok: true,
          account: sanitizeAccountPublic(acc),
          hasSave: Boolean(acc.save),
          saveSummary: acc.save ? acc.save.summary || null : null
        });
        return;
      }

      if (req.method === 'POST' && pathname === '/api/auth/logout') {
        const token = extractBearerToken(req);
        if (token && db.state.tokens[token]) {
          delete db.state.tokens[token];
          db.persist();
        }
        sendJson(res, 200, { ok: true });
        return;
      }

      if (req.method === 'GET' && pathname === '/api/save/pull') {
        const session = authenticate(req);
        if (!session) {
          sendJson(res, 401, { ok: false, error: '请先登录账号' });
          return;
        }
        const acc = session.account;
        sendJson(res, 200, {
          ok: true,
          hasSave: Boolean(acc.save),
          save: acc.save || null
        });
        return;
      }

      if (req.method === 'POST' && pathname === '/api/save/push') {
        const session = authenticate(req);
        if (!session) {
          sendJson(res, 401, { ok: false, error: '请先登录账号' });
          return;
        }
        const body = await readJsonBody(req);
        if (!body.payload || typeof body.payload !== 'object' || Array.isArray(body.payload)) {
          sendJson(res, 400, { ok: false, error: '缺少有效的存档 payload 数据' });
          return;
        }

        const acc = session.account;
        if (acc.save) {
          acc.history = Array.isArray(acc.history) ? acc.history : [];
          acc.history.unshift(acc.save);
          if (acc.history.length > 3) {
            acc.history = acc.history.slice(0, 3);
          }
        }

        const now = Date.now();
        const nextRevision = (acc.save && typeof acc.save.revision === 'number' ? acc.save.revision : 0) + 1;
        acc.save = {
          schemaVersion: typeof body.schemaVersion === 'number' ? body.schemaVersion : 1,
          updatedAt: now,
          revision: nextRevision,
          reason: typeof body.reason === 'string' ? body.reason : 'manual',
          summary: body.summary && typeof body.summary === 'object' ? body.summary : {},
          payload: body.payload
        };
        acc.updatedAt = now;
        db.persist();

        if (!quiet) {
          const stage = acc.save.summary?.questStage ?? '?';
          console.log(`[Save] Pushed save for ${acc.username} (rev=${nextRevision}, stage=${stage}, reason=${acc.save.reason})`);
        }
        sendJson(res, 200, {
          ok: true,
          updatedAt: now,
          revision: nextRevision
        });
        return;
      }

      sendJson(res, 404, { ok: false, error: '接口不存在' });
    } catch (err) {
      const code = err.statusCode || 500;
      sendJson(res, code, { ok: false, error: err.message || '服务器内部错误' });
    }
  });

  return new Promise((resolve) => {
    server.listen(port, host, () => {
      const addr = server.address();
      if (!quiet) {
        console.log(`Picking Hope Cloud Server listening on http://${addr.address}:${addr.port}`);
        console.log(`Data directory: ${dataDir}`);
      }
      resolve({ server, address: addr });
    });
  });
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  startServer();
}
