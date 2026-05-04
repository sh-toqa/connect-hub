import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * Wraps login and signup routes.
 * If the user is already authenticated, redirects to /feed.
 */
export default function GuestRoute() {
  const { user } = useAuth();
  return user ? <Navigate to="/feed" replace /> : <Outlet />;
}
