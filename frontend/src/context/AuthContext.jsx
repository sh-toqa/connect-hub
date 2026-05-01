import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loginUser, logoutUser, getMe } from '../api/authApi';

// AuthContext provides authentication state and functions to the app.
const AuthContext = createContext(null);

// Auth state is managed here and provided to the rest of the app via AuthProvider.
export function AuthProvider({ children }) {
  // null  = not authenticated
  // {...} = authenticated user object (UserDto shape)
  // saves the logged-in user's info, or null if not logged in
  const [user, setUser] = useState(null);

  // check if user info is being loaded
  const [loading, setLoading] = useState(true);

  // On mount, try to restore the session.
  // This succeeds only if a token was already set in tokenStore
  useEffect(() => {
    getMe()
      .then((me) => setUser(me))
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  /**
   * login() — calls the API, stores the token, updates React state.
   * Components call this; they never touch tokenStore directly.
   *
   * @param {{ email, password }} credentials
   * @returns {Promise<UserDto>} the logged-in user
   */
  const login = useCallback(async (credentials) => {
    const { user: loggedInUser } = await loginUser(credentials);
    setUser(loggedInUser);
    return loggedInUser;
  }, []);

  /**
   * logout() — calls the API (sets OFFLINE), wipes the token, clears state.
   */
  const logout = useCallback(async () => {
    await logoutUser();
    setUser(null);
  }, []);

  const value = { user, loading, login, logout, setUser };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}

/**
 * useAuth() — consume the auth context from any child component.
 * Throws a clear error if used outside <AuthProvider>.
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth() must be used inside <AuthProvider>');
  }
  return ctx;
}