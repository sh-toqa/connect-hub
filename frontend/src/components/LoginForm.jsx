import { useState } from 'react';

// Validation logic for the login form. Returns an object with error messages for each field.
function validate(form) {
  const errors = {};

  if (!form.email) {
    errors.email = 'Email is required.';
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = 'Enter a valid email address.';
  }

  if (!form.password) {
    errors.password = 'Password is required.';
  }

  return errors;
}

export default function LoginForm({ onSubmit, serverError }) {
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors,  setErrors]  = useState({});
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: '' }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); // prevent page reload
    const validationErrors = validate(form);
    // if there are validation errors, set them in state and do NOT call onSubmit
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }
    setLoading(true); // turn on loading state while waiting for API response
    try {
      await onSubmit({ email: form.email, password: form.password });
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} noValidate data-testid="login-form">

      {/* Server-level error — generic wording to avoid user enumeration */}
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

      {/* Password */}
      <div className={`form-field ${errors.password ? 'form-field--error' : ''}`}>
        <label htmlFor="password" className="form-field__label">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          value={form.password}
          onChange={handleChange}
          placeholder="Your password"
          autoComplete="current-password"
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

      <button
        type="submit"
        className="btn btn--primary btn--full"
        disabled={loading}
        aria-busy={loading}
      >
        {loading ? <span className="spinner" aria-label="Logging in…" /> : 'Log in'}
      </button>
    </form>
  );
}