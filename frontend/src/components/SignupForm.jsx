import { useState } from 'react';

function validate(form) {
  const errors = {};

  if (!form.email) {
    errors.email = 'Email is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!form.username) {
    errors.username = 'Username is required.';
  } else if (form.username.length < 3 || form.username.length > 50) {
    errors.username = 'Username must be 3–50 characters.';
  } else if (!/^[a-zA-Z0-9_]+$/.test(form.username)) {
    errors.username = 'Letters, numbers, and underscores only.';
  }

  if (!form.password) {
    errors.password = 'Password is required.';
  } else if (form.password.length < 8) {
    errors.password = 'Password must be at least 8 characters.';
  }

  if (!form.confirmPassword) {
    errors.confirmPassword = 'Please confirm your password.';
  } else if (form.password !== form.confirmPassword) {
    errors.confirmPassword = 'Passwords do not match.';
  }

  if (!form.dateOfBirth) {
    errors.dateOfBirth = 'Date of birth is required.';
  } else if (new Date(form.dateOfBirth) >= new Date()) {
    errors.dateOfBirth = 'Date of birth must be in the past.';
  }

  return errors;
}

export default function SignupForm({ onSubmit, serverError }) {
  const [form, setForm] = useState({
    email:           '',
    username:        '',
    password:        '',
    confirmPassword: '',
    dateOfBirth:     '',
  });
  const [errors,  setErrors]  = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Clear the field-level error as the user types
    setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validationErrors = validate(form);

    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setLoading(true);
    try {
      await onSubmit({
        email:       form.email,
        username:    form.username,
        password:    form.password,
        dateOfBirth: form.dateOfBirth, // "YYYY-MM-DD" ISO string
      });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate data-testid="signup-form">

      {/* Server-level error (e.g. duplicate email from API) */}
      {serverError && (
        <div className="alert alert--error" role="alert" aria-live="polite">
          {serverError}
        </div>
      )}

      {/* Email */}
      <div className={`form-field ${errors.email ? 'form-field--error' : ''}`}>
        <label htmlFor="email" className="form-field__label">Email address</label>
        <input
          id="email"
          name="email"
          type="email"
          value={form.email}
          onChange={handleChange}
          placeholder="you@example.com"
          autoComplete="email"
          aria-invalid={!!errors.email}
          aria-describedby={errors.email ? 'email-error' : undefined}
          className="form-field__input"
        />
        {errors.email && (
          <span id="email-error" className="form-field__error" role="alert">
            {errors.email}
          </span>
        )}
      </div>

      {/* Username */}
      <div className={`form-field ${errors.username ? 'form-field--error' : ''}`}>
        <label htmlFor="username" className="form-field__label">Username</label>
        <input
          id="username"
          name="username"
          type="text"
          value={form.username}
          onChange={handleChange}
          placeholder="e.g. alice_42"
          autoComplete="username"
          aria-invalid={!!errors.username}
          aria-describedby={errors.username ? 'username-error' : undefined}
          className="form-field__input"
        />
        {errors.username && (
          <span id="username-error" className="form-field__error" role="alert">
            {errors.username}
          </span>
        )}
      </div>

      {/* Password */}
      <div className={`form-field ${errors.password ? 'form-field--error' : ''}`}>
        <label htmlFor="password" className="form-field__label">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
          placeholder="At least 8 characters"
          autoComplete="new-password"
          aria-invalid={!!errors.password}
          aria-describedby={errors.password ? 'password-error' : undefined}
          className="form-field__input"
        />
        {errors.password && (
          <span id="password-error" className="form-field__error" role="alert">
            {errors.password}
          </span>
        )}
      </div>

      {/* Confirm Password */}
      <div className={`form-field ${errors.confirmPassword ? 'form-field--error' : ''}`}>
        <label htmlFor="confirmPassword" className="form-field__label">Confirm password</label>
        <input
          id="confirmPassword"
          name="confirmPassword"
          type="password"
          value={form.confirmPassword}
          onChange={handleChange}
          placeholder="Repeat your password"
          autoComplete="new-password"
          aria-invalid={!!errors.confirmPassword}
          aria-describedby={errors.confirmPassword ? 'confirm-error' : undefined}
          className="form-field__input"
        />
        {errors.confirmPassword && (
          <span id="confirm-error" className="form-field__error" role="alert">
            {errors.confirmPassword}
          </span>
        )}
      </div>

      {/* Date of Birth */}
      <div className={`form-field ${errors.dateOfBirth ? 'form-field--error' : ''}`}>
        <label htmlFor="dateOfBirth" className="form-field__label">Date of birth</label>
        <input
          id="dateOfBirth"
          name="dateOfBirth"
          type="date"
          value={form.dateOfBirth}
          onChange={handleChange}
          max={new Date().toISOString().split('T')[0]}
          aria-invalid={!!errors.dateOfBirth}
          aria-describedby={errors.dateOfBirth ? 'dob-error' : undefined}
          className="form-field__input"
          placeholder="YYYY-MM-DD"
        />
        {errors.dateOfBirth && (
          <span id="dob-error" className="form-field__error" role="alert">
            {errors.dateOfBirth}
          </span>
        )}
      </div>

      <button
        type="submit"
        className="btn btn--primary btn--full"
        disabled={loading}
        aria-busy={loading}
      >
        {loading ? <span className="spinner" aria-label="Creating account…" /> : 'Create account'}
      </button>
    </form>
  );
}