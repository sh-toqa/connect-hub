import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, vi } from 'vitest';
import SignupForm from './SignupForm';

// ── Helpers ───────────────────────────────────────────────────────────────────
function fillValidForm(user) {
  return async () => {
    await user.type(screen.getByLabelText(/email address/i),   'alice@example.com');
    await user.type(screen.getByLabelText(/^username/i),        'alice_42');
    await user.type(screen.getByLabelText(/^password$/i),       'secret123');
    await user.type(screen.getByLabelText(/confirm password/i), 'secret123');
    fireEvent.change(screen.getByLabelText(/date of birth/i), {
      target: { value: '2000-06-15' },
    });
  };
}

describe('SignupForm', () => {

  it('renders all form fields', () => {
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/date of birth/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
  });

  // Validation errors for each field
  it('shows a validation error when email is empty', async () => {
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/email is required/i)).toBeInTheDocument();
  });

  it('shows a validation error for an invalid email format', async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    await user.type(screen.getByLabelText(/email address/i), 'not-an-email');
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/valid email address/i)).toBeInTheDocument();
  });

  it('shows a validation error when username is too short', async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    await user.type(screen.getByLabelText(/^username/i), 'ab');
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/3.50 characters/i)).toBeInTheDocument();
  });

  it('shows a validation error when password is too short', async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    await user.type(screen.getByLabelText(/^password$/i), 'short');
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/at least 8 characters/i)).toBeInTheDocument();
  });

  it('shows a validation error when passwords do not match', async () => {
    const user = userEvent.setup();
    render(<SignupForm onSubmit={vi.fn()} serverError="" />);
    await user.type(screen.getByLabelText(/^password$/i),       'secret123');
    await user.type(screen.getByLabelText(/confirm password/i), 'different!');
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    expect(await screen.findByText(/passwords do not match/i)).toBeInTheDocument();
  });

  // Error path: invalid form does NOT call onSubmit
  it('does NOT call onSubmit when there are validation errors', async () => {
    const onSubmit = vi.fn();
    render(<SignupForm onSubmit={onSubmit} serverError="" />);
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));
    await screen.findByText(/email is required/i);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  // Happy path: valid form → calls onSubmit with correct data
  it('calls onSubmit with correct data when the form is valid', async () => {
    const user     = userEvent.setup();
    const onSubmit = vi.fn().mockResolvedValue(undefined);
    render(<SignupForm onSubmit={onSubmit} serverError="" />);

    await fillValidForm(user)();
    fireEvent.click(screen.getByRole('button', { name: /create account/i }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        email:       'alice@example.com',
        username:    'alice_42',
        password:    'secret123',
        dateOfBirth: '2000-06-15',
      });
    });
  });

  // Server-level error message when the prop is set
  it('displays the serverError prop when provided', () => {
    render(
      <SignupForm
        onSubmit={vi.fn()}
        serverError="An account with that email already exists."
      />
    );
    expect(
      screen.getByText(/an account with that email already exists/i)
    ).toBeInTheDocument();
  });
});