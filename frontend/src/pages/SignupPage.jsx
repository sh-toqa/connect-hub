import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { registerUser } from '../api/authApi';
import SignupForm from '../components/SignupForm';

export default function SignupPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState('');
  const [success,     setSuccess]     = useState(false);

  const handleSubmit = async (formData) => {
    setServerError('');
    try {
      await registerUser(formData);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      // Show the message from the API (e.g. "email already exists")
      // or a generic fallback if the network is down
      const msg =
        err.response?.data?.message ||
        'Registration failed. Please try again.';
      setServerError(msg);
    }
  };

  if (success) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-card__header">
            <div className="auth-logo">
              <ConnectHubLogo />
              <span className="auth-logo__text">ConnectHub</span>
            </div>
          </div>
          <div className="auth-card__body">
            <div className="success-banner" role="status">
              <span className="success-icon" aria-hidden="true">✓</span>
              <p>Account created! Redirecting to login…</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">

        <div className="auth-card__header">
          <div className="auth-logo">
            <ConnectHubLogo />
            <span className="auth-logo__text">ConnectHub</span>
          </div>
          <h1 className="auth-card__title">Create your account</h1>
          <p className="auth-card__subtitle">Join ConnectHub today</p>
        </div>

        <div className="auth-card__body">
          <SignupForm onSubmit={handleSubmit} serverError={serverError} />

          <p className="auth-footer">
            Already have an account?{' '}
            <Link to="/login" className="link">Log in</Link>
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