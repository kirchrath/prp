/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // same paths as Traefik in production
    proxy: {
      '/api/catalog': 'http://localhost:8081',
      '/api/stock': 'http://localhost:8082',
      '/api/shopping': 'http://localhost:8083',
      '/api/device': 'http://localhost:8084',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/api/catalog/**', 'src/api/stock/**', 'src/api/shopping/**', 'src/api/device/**', 'src/test/**',
        'src/**/*.test.{ts,tsx}', 'src/vite-env.d.ts'],
      thresholds: { lines: 100, branches: 100, functions: 100, statements: 100 },
    },
  },
});
