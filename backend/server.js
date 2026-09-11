import Fastify from 'fastify';
import cors from '@fastify/cors';

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

const OPENROUTER_URL = 'https://openrouter.ai/api/v1/chat/completions';
const OPENROUTER_FREE_MODEL = 'openrouter/free';
const OPENROUTER_KEYS_PAGE = 'https://openrouter.ai/settings/keys';
const MAX_KEYS = 100;
const KEY_COOLDOWN_MS = 60_000;
const REQUEST_TIMEOUT_MS = 45_000;

function readOpenRouterKeys() {
  const numbered = Array.from({ length: MAX_KEYS }, (_, i) => process.env[`OPENROUTER_API_KEY_${i + 1}`]?.trim()).filter(Boolean);
  const packed = (process.env.OPENROUTER_API_KEYS ?? '')
    .split(/[\n,]+/)
    .map((key) => key.trim())
    .filter(Boolean);
  return [...new Set([...numbered, ...packed])].slice(0, MAX_KEYS);
}

const keys = readOpenRouterKeys();
const cooldownUntil = new Map();
let nextKeyIndex = 0;

function hasUsableKey() {
  return keys.length > 0;
}

function pickNextKey() {
  if (!hasUsableKey()) return null;
  const now = Date.now();
  for (let offset = 0; offset < keys.length; offset += 1) {
    const index = (nextKeyIndex + offset) % keys.length;
    if ((cooldownUntil.get(index) ?? 0) <= now) {
      nextKeyIndex = (index + 1) % keys.length;
      return { index, key: keys[index] };
    }
  }
  return null;
}

function rotateOnStatus(status) {
  return status === 401 || status === 402 || status === 403 || status === 408 || status === 409 || status === 425 || status === 429 || status >= 500;
}

function markKeyCoolingDown(index) {
  cooldownUntil.set(index, Date.now() + KEY_COOLDOWN_MS);
}

async function callOpenRouter(messages) {
  if (!hasUsableKey()) {
    return {
      ok: false,
      status: 503,
      body: {
        code: 'NO_OPENROUTER_KEY',
        error: 'No OpenRouter API key is configured.',
        setupUrl: OPENROUTER_KEYS_PAGE,
      },
    };
  }

  let attempts = 0;
  const maxAttempts = keys.length;
  let lastError = null;

  while (attempts < maxAttempts) {
    const picked = pickNextKey();
    if (!picked) break;

    attempts += 1;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

    try {
      const response = await fetch(OPENROUTER_URL, {
        method: 'POST',
        signal: controller.signal,
        headers: {
          'content-type': 'application/json',
          authorization: `Bearer ${picked.key}`,
          'HTTP-Referer': process.env.OPENROUTER_SITE_URL ?? 'https://github.com/softarmoryofficial/repository',
          'X-OpenRouter-Title': process.env.OPENROUTER_APP_NAME ?? 'JARVIS Android',
        },
        body: JSON.stringify({
          model: OPENROUTER_FREE_MODEL,
          messages,
          temperature: 0.3,
        }),
      });

      const data = await response.json().catch(() => ({}));
      if (response.ok) {
        return {
          ok: true,
          status: 200,
          body: {
            message: data?.choices?.[0]?.message?.content ?? '',
            model: data?.model ?? OPENROUTER_FREE_MODEL,
          },
        };
      }

      lastError = data?.error?.message ?? `OpenRouter error (${response.status})`;
      if (rotateOnStatus(response.status)) {
        markKeyCoolingDown(picked.index);
        continue;
      }

      return {
        ok: false,
        status: response.status,
        body: { code: 'OPENROUTER_ERROR', error: lastError },
      };
    } catch (error) {
      lastError = error?.name === 'AbortError' ? 'OpenRouter request timed out' : (error?.message ?? 'OpenRouter request failed');
      markKeyCoolingDown(picked.index);
    } finally {
      clearTimeout(timer);
    }
  }

  return {
    ok: false,
    status: 503,
    body: {
      code: 'ALL_OPENROUTER_KEYS_FAILED',
      error: lastError ?? 'All configured OpenRouter keys are temporarily unavailable.',
      setupUrl: OPENROUTER_KEYS_PAGE,
    },
  };
}

app.get('/health', async () => ({
  ok: true,
  service: 'jarvis-backend',
  provider: 'openrouter',
  model: OPENROUTER_FREE_MODEL,
  keyPoolSize: keys.length,
  maxKeyPoolSize: MAX_KEYS,
}));

app.get('/config', async () => ({
  provider: 'openrouter',
  model: OPENROUTER_FREE_MODEL,
  hasApiKey: hasUsableKey(),
  keyPoolSize: keys.length,
  setupUrl: OPENROUTER_KEYS_PAGE,
}));

app.post('/chat', async (request, reply) => {
  const { messages = [] } = request.body ?? {};
  if (!Array.isArray(messages) || messages.length === 0) {
    return reply.code(400).send({ code: 'INVALID_MESSAGES', error: 'messages must be a non-empty array' });
  }

  const result = await callOpenRouter(messages);
  return reply.code(result.status).send(result.body);
});

const port = Number(process.env.PORT ?? 3000);
await app.listen({ host: '0.0.0.0', port });
