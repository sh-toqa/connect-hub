import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// A wrapper for <Route> that redirects to the login page if you're not authenticated.
export default function ProtectedRoute({ children }) {
  const { user, loading } = useAuth();
  const location = useLocation();

  // If checking auth status (e.g. on first load), show a loading spinner instead of redirecting
  if (loading) {
    return (
      <div className="full-page-loader" role="status" aria-label="Loading…">
        <span className="spinner spinner--lg" />
        <p>Loading…</p>
      </div>
    );
  }

  // No valid session -> redirect to login
  // Save the current location in state to come back after login
  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}