// Central place for the backend base URL.
// Set VITE_API_BASE_URL in your .env (see .env.example) to point at a
// non-local backend (staging/production). Falls back to localhost for dev.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';
