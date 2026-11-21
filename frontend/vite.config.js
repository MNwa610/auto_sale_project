import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const { BACKEND_URL } = loadEnv(mode, process.cwd(), '');
  const proxy = { '/api': { target: BACKEND_URL || 'http://localhost:8080', changeOrigin: true } };
  return { esbuild: { jsx: 'automatic' }, server: { proxy }, preview: { proxy } };
});
