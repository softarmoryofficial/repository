import Fastify from 'fastify';
import cors from '@fastify/cors';

const app = Fastify({ logger: true });
await app.register(cors, { origin: true });

app.get('/health', async () => ({ ok: true, service: 'jarvis-backend' }));

app.post('/chat', async (request, reply) => {
  const { messages = [] } = request.body ?? {};
  if (!Array.isArray(messages) || messages.length === 0) {
    return reply.code(400).send({ error: 'messages must be a non-empty array' });
  }

  const apiKey = process.env.OPENAI_API_KEY;
  const model = process.env.OPENAI_MODEL;
  const baseUrl = process.env.OPENAI_BASE_URL ?? 'https://api.openai.com/v1';
  if (!apiKey || !model) {
    return reply.code(503).send({ error: 'AI provider is not configured on the server' });
  }

  const response = await fetch(`${baseUrl}/chat/completions`, {
    method: 'POST',
    headers: { 'content-type': 'application/json', authorization: `Bearer ${apiKey}` },
    body: JSON.stringify({ model, messages, temperature: 0.3 })
  });

  const data = await response.json();
  if (!response.ok) return reply.code(response.status).send({ error: data?.error?.message ?? 'Provider error' });

  return { message: data.choices?.[0]?.message?.content ?? '' };
});

const port = Number(process.env.PORT ?? 3000);
await app.listen({ host: '0.0.0.0', port });
