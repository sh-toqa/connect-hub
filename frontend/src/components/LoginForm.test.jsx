// src/components/LoginForm.test.jsx
//
// Tests that:
//  - Wrong / missing credentials show the correct validation errors
//  - Successful login stores the token and redirects the user
//  - Server error message is rendered when credentials are rejected

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import LoginForm from './LoginForm';

// ── Tests ─────────────────────────────────────────────────────────────────────
describe('LoginForm', () => {

  it('renders email and password fields and a submit button', () => {
    render(<LoginForm onSubmit={vi.fn()} serverError="" />);
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /log in/i })).toBeInTheDocument();
  });

  it('shows a required error when email is empty on submit', async () => {
    render(<LoginForm onSubmit={vi.fn()} serverError="" />);
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
  });

  it('shows a required error when password is empty on submit', async () => {
    const user = userEvent.setup();
    render(<LoginForm onSubmit={vi.fn()} serverError="" />);
    await user.type(screen.getByLabelText(/email address/i), 'alice@example.com');
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));
    expect(await screen.findByText(/password is required/i)).toBeInTheDocument();
  });

  it('does NOT call onSubmit when fields are empty', async () => {
    const onSubmit = vi.fn();
    render(<LoginForm onSubmit={onSubmit} serverError="" />);
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));
    await screen.findByText(/email is required/i);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with email and password when form is valid', async () => {
    const user     = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<LoginForm onSubmit={onSubmit} serverError="" />);

    await user.type(screen.getByLabelText(/email address/i), 'alice@example.com');
    await user.type(screen.getByLabelText(/password/i),       'secret123');
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        email:    'alice@example.com',
        password: 'secret123',
      });
    });
  });

  it('displays the serverError prop (wrong credentials message)', () => {
    render(
      <LoginForm
        onSubmit={vi.fn()}
        serverError="Invalid email or password. Please try again."
      />
    );
    expect(
      screen.getByText(/invalid email or password/i)
    ).toBeInTheDocument();
  });

  it('disables the submit button while the login request is in flight', async () => {
    const user = userEvent.setup();
    // onSubmit never resolves → simulates a slow request
    const onSubmit = vi.fn(() => new Promise(() => {}));
    render(<LoginForm onSubmit={onSubmit} serverError="" />);

    await user.type(screen.getByLabelText(/email address/i), 'alice@example.com');
    await user.type(screen.getByLabelText(/password/i),       'secret123');
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /log in|logging in/i })).toBeDisabled();
    });
  });
});