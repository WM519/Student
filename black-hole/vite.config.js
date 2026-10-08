import { defineConfig } from 'vite';

export default defineConfig({
  root: '.',
  server: { open: false, host: '127.0.0.1', port: 5173 },
  build: { target: 'es2020', outDir: 'dist', emptyOutDir: true },
});
