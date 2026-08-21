const express = require('express');
const { GoogleGenAI } = require('@google/genai');
const { requireJwtAuth } = require('../middleware');

const router = express.Router();

const DEFAULT_LIVE_MODEL = 'gemini-3.1-flash-live-preview';
const DEFAULT_SESSION_MINUTES = 30;
const DEFAULT_NEW_SESSION_SECONDS = 60;
const MAX_SESSION_MINUTES = 19 * 60; // Gemini requires less than 20 hours.

function readPositiveInteger(value, fallback, maximum) {
  const parsed = Number.parseInt(String(value ?? ''), 10);
  if (!Number.isFinite(parsed) || parsed <= 0) return fallback;
  return Math.min(parsed, maximum);
}

function getLiveConfig() {
  const model = String(process.env.TEZGPT_LIVE_MODEL || DEFAULT_LIVE_MODEL).trim();
  const sessionMinutes = readPositiveInteger(
    process.env.TEZGPT_LIVE_SESSION_MINUTES,
    DEFAULT_SESSION_MINUTES,
    MAX_SESSION_MINUTES,
  );
  const newSessionSeconds = readPositiveInteger(
    process.env.TEZGPT_LIVE_NEW_SESSION_SECONDS,
    DEFAULT_NEW_SESSION_SECONDS,
    19 * 60,
  );
  return { model, sessionMinutes, newSessionSeconds };
}

function buildExpiryTimes({ sessionMinutes, newSessionSeconds }, now = Date.now()) {
  return {
    expiresAt: new Date(now + sessionMinutes * 60_000).toISOString(),
    newSessionExpiresAt: new Date(now + newSessionSeconds * 1_000).toISOString(),
  };
}

function buildTokenConfig({ model, expiresAt, newSessionExpiresAt }) {
  return {
    uses: 1,
    expireTime: expiresAt,
    newSessionExpireTime: newSessionExpiresAt,
    liveConnectConstraints: {
      model,
      config: {
        sessionResumption: {},
        responseModalities: ['AUDIO'],
      },
    },
    lockAdditionalFields: ['model', 'config.sessionResumption', 'config.responseModalities'],
  };
}

/**
 * Authenticated control-plane endpoints for TezGPT Live.
 *
 * The client receives only a short-lived Gemini token. The long-lived
 * GEMINI_API_KEY stays in the server environment and is never serialized.
 */
router.get('/config', requireJwtAuth, (_req, res) => {
  const { model, sessionMinutes, newSessionSeconds } = getLiveConfig();
  const configured = Boolean(String(process.env.GEMINI_API_KEY || '').trim());
  res.set('Cache-Control', 'no-store');
  return res.json({
    ok: true,
    enabled: configured,
    model,
    sessionMinutes,
    newSessionSeconds,
    input: {
      audio: { mimeType: 'audio/pcm;rate=16000', sampleRate: 16_000 },
      images: { mimeType: 'image/jpeg', maxFramesPerSecond: 1 },
      text: true,
    },
    output: { audio: { mimeType: 'audio/pcm;rate=24000', sampleRate: 24_000 } },
    deviceActions: { enabled: false, approvalRequired: true },
  });
});

router.post('/session', requireJwtAuth, async (req, res) => {
  const apiKey = String(process.env.GEMINI_API_KEY || '').trim();
  if (!apiKey) {
    return res.status(503).json({
      ok: false,
      code: 'LIVE_NOT_CONFIGURED',
      error: 'TezGPT Live is not configured on the server.',
    });
  }

  const config = getLiveConfig();
  const expiry = buildExpiryTimes(config);

  try {
    const client = new GoogleGenAI({ apiKey });
    const token = await client.authTokens.create({
      config: buildTokenConfig({
        model: config.model,
        expiresAt: expiry.expiresAt,
        newSessionExpiresAt: expiry.newSessionExpiresAt,
      }),
    });

    if (!token?.name || typeof token.name !== 'string') {
      return res.status(502).json({
        ok: false,
        code: 'LIVE_TOKEN_INVALID',
        error: 'The Live API did not return a usable session token.',
      });
    }

    res.set('Cache-Control', 'no-store');
    return res.json({
      ok: true,
      token: token.name,
      model: config.model,
      expiresAt: expiry.expiresAt,
      newSessionExpiresAt: expiry.newSessionExpiresAt,
      input: {
        audio: { mimeType: 'audio/pcm;rate=16000', sampleRate: 16_000 },
        images: { mimeType: 'image/jpeg', maxFramesPerSecond: 1 },
        text: true,
      },
      output: { audio: { mimeType: 'audio/pcm;rate=24000', sampleRate: 24_000 } },
      deviceActions: { enabled: false, approvalRequired: true },
    });
  } catch (error) {
    console.warn('[TezGPT Live] ephemeral token creation failed', {
      userId: req.user?.id,
      name: error?.name,
      message: error?.message,
    });
    return res.status(502).json({
      ok: false,
      code: 'LIVE_TOKEN_FAILED',
      error: 'Unable to start a TezGPT Live session right now.',
    });
  }
});

module.exports = router;
module.exports.buildExpiryTimes = buildExpiryTimes;
module.exports.buildTokenConfig = buildTokenConfig;
module.exports.getLiveConfig = getLiveConfig;
