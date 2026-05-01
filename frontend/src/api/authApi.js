import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/auth';

// JWT token store — in-memory only, cleared on page refresh
// for security
let _token = null;

// tokenStore provides set/get/clear methods for the JWT token.
export const tokenStore = {
  set:   (t) => { _token = t; },
  get:   ()  => _token,
  clear: ()  => { _token = null; },
};

// Axios instance with base URL and JSON headers
const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Request interceptor to add Authorization header if token exists
api.interceptors.request.use((config) => {
  const token = tokenStore.get();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle 401 Unauthorized globally
// If we get a 401, it means the token is invalid/expired, so we clear it from memory.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      tokenStore.clear();
    }
    return Promise.reject(error);
  }
);

// API functions for authentication-related endpoints

/**
 * Register a new user — FR-UA-01, FR-UA-08.
 *
 * @param {{ email, username, password, dateOfBirth }} data
 *   dateOfBirth must be an ISO date string: "YYYY-MM-DD"
 * @returns {Promise<UserDto>} the created user (no password field)
 */
export const registerUser = async ({ email, username, password, dateOfBirth }) => {
  const { data } = await api.post('/register', {
    email,
    username,
    password,
    dateOfBirth, // ISO date string "YYYY-MM-DD"
  });
  return data; // UserDto
};

/**
 * Login
 * Stores JWT in memory via tokenStore on success.
 *
 * @param {{ email, password }} credentials
 * @returns {Promise<{ token: string, user: UserDto }>}
 */
export const loginUser = async ({ email, password }) => {
  const { data } = await api.post('/login', { email, password });
  tokenStore.set(data.token);
  return data; // { token, user }
};

/**
 * Logout
 * Calls the backend (sets user OFFLINE), then wipes the in-memory token.
 *
 * @returns {Promise<void>}
 */
export const logoutUser = async () => {
  try {
    await api.post('/logout');
  } finally {
    // Always clear the token, even if the request fails
    tokenStore.clear();
  }
};

/**
 * Fetch the current authenticated user — used to restore React state on mount.
 * Returns null if there is no token in memory.
 *
 * @returns {Promise<UserDto|null>}
 */
export const getMe = async () => {
  if (!tokenStore.get()) return null;
  const { data } = await api.get('/me');
  return data; // UserDto
};

export default api;