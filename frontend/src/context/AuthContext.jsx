import { createContext, useContext, useState, useCallback } from 'react';
import { loginUser as loginApi, logoutUser as logoutApi } from '../api/authApi';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = sessionStorage.getItem('user');
      return stored ? JSON.parse(stored) : null;
    } catch { return null; }
  });

  const login = useCallback(async ({ email, password }) => {
    const { data } = await loginApi({ email, password });

    // Log the raw response so we can see the exact shape
    console.log('Raw login response:', data);

    // Handle both response shapes:
    const token = data.token;
    const user  = data.user ?? {
      userId:          data.userId,
      username:        data.username,
      email:           data.email,
      bio:             data.bio,
      profilePhotoPath: data.profilePhotoPath,
      coverPhotoPath:  data.coverPhotoPath,
      status:          data.status,
    };

    console.log('Parsed user:', user);

    sessionStorage.setItem('token', token);
    sessionStorage.setItem('user', JSON.stringify(user));
    setUser(user);
    return user;
  }, []);

  const logout = useCallback(async () => {
    try { await logoutApi(); } catch { /* best-effort */ }
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    setUser(null);
  }, []);

  const refreshUser = useCallback((updatedUser) => {
    sessionStorage.setItem('user', JSON.stringify(updatedUser));
    setUser(updatedUser);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, logout, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
};
