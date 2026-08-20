import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Android loads the existing LibreChat web app unchanged.
 * Set LIBRECHAT_SERVER_URL before `npx cap copy` for your deployment.
 */
const config: CapacitorConfig = {
  appId: 'com.librechat.app',
  appName: 'LibreChat',
  webDir: 'client/dist',
  server: {
    url: process.env.LIBRECHAT_SERVER_URL ?? 'https://chat.librechat.ai',
    cleartext: false,
  },
  android: {
    allowMixedContent: false,
  },
};

export default config;
