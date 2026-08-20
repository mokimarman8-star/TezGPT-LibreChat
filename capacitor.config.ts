import type { CapacitorConfig } from '@capacitor/cli';

/**
 * This legacy web-packaging configuration is retained only for the web project.
 * The delivered native Java Android client does not use Capacitor or this file.
 * Set LIBRECHAT_SERVER_URL before `npx cap copy` only if web packaging is explicitly needed.
 */
const config: CapacitorConfig = {
  appId: 'com.tezgpt.app',
  appName: 'TezGPT',
  webDir: 'client/dist',
  server: {
    url: process.env.LIBRECHAT_SERVER_URL ?? 'https://tezgpt.onrender.com',
    cleartext: false,
  },
  android: {
    allowMixedContent: false,
  },
};

export default config;
