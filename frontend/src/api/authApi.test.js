import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import api, { registerUser, loginUser, logoutUser } from './authApi';

let mock;

beforeEach(() => {
  mock = new MockAdapter(api);
  sessionStorage.clear();
});

afterEach(() => {
  mock.restore();
  sessionStorage.clear();
});

// registerUser tests
describe('registerUser', () => {

  // correct registration test
  it('POSTs to /auth/register with the correct payload', async () => {
    const payload  = { email: 'alice@example.com', username: 'alice', password: 'secret123', dateOfBirth: '2000-06-15' };
    const fakeUser = { userId: 'uuid-1', email: 'alice@example.com', username: 'alice' };
    mock.onPost('/auth/register', payload).reply(201, fakeUser);

    const res = await registerUser(payload);
    expect(res.data).toEqual(fakeUser);
  });

  // duplicate email test
  it('throws when the server returns 409 Conflict (duplicate email)', async () => {
    mock.onPost('/auth/register').reply(409, {
      message: "An account with email 'alice@example.com' already exists.",
    });

    await expect(
      registerUser({ email: 'alice@example.com', username: 'alice', password: 'secret123', dateOfBirth: '2000-06-15' })
    ).rejects.toThrow();
  });
});

// loginUser tests
describe('loginUser', () => {

  // correct login test
  it('POSTs to /auth/login with email and password', async () => {
    const payload  = { email: 'alice@example.com', password: 'secret123' };
    const response = {
      token: 'jwt-abc123',
      user:  { userId: 'uuid-1', email: 'alice@example.com', username: 'alice', status: 'ONLINE' },
    };
    mock.onPost('/auth/login', payload).reply(200, response);

    const res = await loginUser(payload);
    expect(res.data).toEqual(response);
  });

  // invalid credentials test
  it('does NOT store a token when credentials are wrong (401)', async () => {
    mock.onPost('/auth/login').reply(401, { message: 'Invalid email or password.' });

    await expect(
      loginUser({ email: 'alice@example.com', password: 'wrong' })
    ).rejects.toThrow();

    expect(sessionStorage.getItem('token')).toBeNull();
  });
});

// logoutUser tests
describe('logoutUser', () => {

  // correct logout test
  it('POSTs to /auth/logout', async () => {
    sessionStorage.setItem('token', 'jwt-abc123');
    mock.onPost('/auth/logout').reply(200);

    await logoutUser();
    expect(mock.history.post.some((r) => r.url === '/auth/logout')).toBe(true);
  });

  // Authorization header test
  it('sends Authorization header when token exists', async () => {
    sessionStorage.setItem('token', 'jwt-abc123');
    mock.onPost('/auth/logout').reply(200);

    await logoutUser();
    const logoutReq = mock.history.post.find((r) => r.url === '/auth/logout');
    expect(logoutReq.headers['Authorization']).toBe('Bearer jwt-abc123');
  });

  // network error test - should throw and NOT clear token
  it('throws when the logout request fails (network error)', async () => {
    sessionStorage.setItem('token', 'jwt-abc123');
    mock.onPost('/auth/logout').networkError();

    await expect(logoutUser()).rejects.toThrow();
  });
});