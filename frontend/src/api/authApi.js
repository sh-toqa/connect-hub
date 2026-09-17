import axios from 'axios';
import { API_BASE_URL } from '../config/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT to every request automatically
api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Only redirect to login on 401 if it's NOT the login request itself
api.interceptors.response.use(
  (res) => res,
  (err) => {
    const isLoginRequest = err.config?.url?.includes('/auth/login');
    if (err.response?.status === 401 && !isLoginRequest) {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('user');
      // Use location.replace so browser history is clean
      window.location.replace('/login');
    }
    return Promise.reject(err);
  }
);

export default api;

export const registerUser = (data) => api.post('/auth/register', data);
export const loginUser    = (data) => api.post('/auth/login', data);
export const logoutUser   = ()     => api.post('/auth/logout');