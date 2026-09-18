import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoginForm from '../components/LoginForm';

export default function LoginPage() {
  const { login } = useAuth();
  const [serverError, setServerError] = useState('');

  const handleSubmit = async (credentials) => {
    setServerError('');
    try {
      await login(credentials);
      // No navigate() here — PublicRoutes (in App.jsx) detects user is now
      // set and redirects to /feed automatically
    } catch {
      setServerError('Invalid email or password. Please try again.');
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">

        <div className="auth-card__header">
          <div className="auth-logo">
            <ConnectHubLogo />
            <span className="auth-logo__text">ConnectHub</span>
          </div>
          <h1 className="auth-card__title">Welcome back</h1>
          <p className="auth-card__subtitle">Log in to ConnectHub</p>
        </div>

        <div className="auth-card__body">
          <LoginForm onSubmit={handleSubmit} serverError={serverError} />
          <p className="auth-footer">
            Don't have an account?{' '}
            <Link to="/signup" className="link">Sign up</Link>
          </p>
        </div>

      </div>
    </div>
  );
}

function ConnectHubLogo() {
  return (
    <svg width="36" height="36" viewBox="0 0 36 36" fill="none" aria-hidden="true">
      <circle cx="18" cy="18" r="18" fill="#4F46E5" />
      <circle cx="12" cy="14" r="3" fill="white" />
      <circle cx="24" cy="14" r="3" fill="white" />
      <circle cx="18" cy="24" r="3" fill="white" />
      <line x1="12" y1="14" x2="24" y2="14" stroke="white" strokeWidth="2" />
      <line x1="12" y1="14" x2="18"  y2="24" stroke="white" strokeWidth="2" />
      <line x1="24" y1="14" x2="18"  y2="24" stroke="white" strokeWidth="2" />
    </svg>
  );
}