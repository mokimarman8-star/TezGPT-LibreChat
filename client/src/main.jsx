import './polyfills/regeneratorRuntime';
import { createRoot } from 'react-dom/client';
import { initializeI18n } from './locales/i18n';
import '@librechat/client/style.css';
import './style.css';
import './mobile.css';
import 'katex/dist/katex.min.css';
import 'katex/dist/contrib/copy-tex.js';

window.addEventListener('vite:preloadError', (event) => {
  if (window.__lcRecoverStaleAssets?.()) {
    event.preventDefault();
  }
});

const container = document.getElementById('root');
const root = createRoot(container);

async function bootstrap() {
  await initializeI18n();

  // Keep the lightweight HTML boot shell visible while GitHub Pages downloads
  // optional editor, diagram, and markdown application chunks.
  const [{ default: App }, { ApiErrorBoundaryProvider }] = await Promise.all([
    import('./App'),
    import('./hooks/ApiErrorBoundaryContext'),
  ]);

  const capacitor = window.Capacitor;
  const nativePermissions = capacitor?.Plugins?.NativePermissions;
  if (capacitor?.isNativePlatform?.() && nativePermissions?.request) {
    await nativePermissions.request();
  }

  root.render(
    <ApiErrorBoundaryProvider>
      <App />
    </ApiErrorBoundaryProvider>,
  );
}

bootstrap().catch((error) => {
  console.error('[i18n] Failed to initialize before render', error);
  import('./App').then(({ default: App }) =>
    import('./hooks/ApiErrorBoundaryContext').then(({ ApiErrorBoundaryProvider }) =>
      root.render(
        <ApiErrorBoundaryProvider>
          <App />
        </ApiErrorBoundaryProvider>,
      ),
    ),
  );
});
