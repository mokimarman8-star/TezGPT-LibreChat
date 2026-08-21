const express = require('express');
const request = require('supertest');

jest.mock('../middleware', () => ({
  requireJwtAuth: (req, _res, next) => {
    req.user = { id: 'test-user' };
    next();
  },
}));

const liveRouter = require('./live');

function makeApp() {
  const app = express();
  app.use(express.json());
  app.use('/api/live', liveRouter);
  return app;
}

describe('TezGPT Live route', () => {
  const originalApiKey = process.env.GEMINI_API_KEY;
  const originalModel = process.env.TEZGPT_LIVE_MODEL;

  afterEach(() => {
    if (originalApiKey === undefined) delete process.env.GEMINI_API_KEY;
    else process.env.GEMINI_API_KEY = originalApiKey;
    if (originalModel === undefined) delete process.env.TEZGPT_LIVE_MODEL;
    else process.env.TEZGPT_LIVE_MODEL = originalModel;
  });

  it('reports disabled without revealing provider credentials', async () => {
    delete process.env.GEMINI_API_KEY;
    const response = await request(makeApp()).get('/api/live/config').expect(200);

    expect(response.body).toEqual(
      expect.objectContaining({
        ok: true,
        enabled: false,
        deviceActions: { enabled: false, approvalRequired: true },
      }),
    );
    expect(JSON.stringify(response.body)).not.toContain('GEMINI_API_KEY');
  });

  it('rejects token creation when the server is not configured', async () => {
    delete process.env.GEMINI_API_KEY;
    const response = await request(makeApp()).post('/api/live/session').expect(503);

    expect(response.body).toEqual({
      ok: false,
      code: 'LIVE_NOT_CONFIGURED',
      error: 'TezGPT Live is not configured on the server.',
    });
  });

  it('locks the token to one audio-response live session', () => {
    const now = Date.parse('2026-08-21T00:00:00.000Z');
    const expiry = liveRouter.buildExpiryTimes(
      { sessionMinutes: 30, newSessionSeconds: 60 },
      now,
    );
    const config = liveRouter.buildTokenConfig({
      model: 'gemini-3.1-flash-live-preview',
      ...expiry,
    });

    expect(config).toEqual(
      expect.objectContaining({
        uses: 1,
        liveConnectConstraints: {
          model: 'gemini-3.1-flash-live-preview',
          config: {
            sessionResumption: {},
            responseModalities: ['AUDIO'],
          },
        },
      }),
    );
    expect(config.expireTime).toBe('2026-08-21T00:30:00.000Z');
    expect(config.newSessionExpireTime).toBe('2026-08-21T00:01:00.000Z');
  });
});
