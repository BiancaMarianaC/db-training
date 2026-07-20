/**
 * ============================================================================
 * vite.config.js — TICKET-I099
 * ============================================================================
 * WHAT:    Vite build config.
 * HOW:     Proxy /api/* during dev so the React app and Spring Boot
 *          look like one origin (no CORS pain).
 * WHY:     Faster development; CORS-free; matches the Day-10 production
 *          deployment where nginx fronts the backend.
 * ============================================================================
 */
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            // TICKET-I100: send /api/* and /actuator/* to the Spring Boot dev server.
            '/api': 'http://localhost:8080',
            '/actuator': 'http://localhost:8080'
        }
    },
    test: {
        environment: 'jsdom',
        setupFiles: ['./src/setupTests.js']
    }
});
