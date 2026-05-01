import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import api, {
  registerUser,
  loginUser,
  logoutUser,
  getMe,
  tokenStore,
} from './authApi';

let mock;
// Create a new MockAdapter instance before each test, and restore it after each test.
beforeEach(() => {
  mock = new MockAdapter(api);
  tokenStore.clear(); // always start with a clean token state
});
// Restore the original adapter (remove the mock) after each test to prevent interference between tests.
afterEach(() => {
  mock.restore();
});

// describe blocks for each API function, with individual it() tests for different scenarios
// registerUser tests
// 1. Happy path: valid registration data → POST /auth/register with correct payload, returns UserDto
// 2. Error path: duplicate email → server returns 409 with message, function throws with that message
describe('registerUser', () => {
  // Happy path: valid registration data → POST /auth/register with correct payload, returns UserDto
  it('POSTs to /auth/register with the correct payload', async () => {
    const payload = {
      email:       'alice@example.com',
      username:    'alice',
      password:    'secret123',
      dateOfBirth: '2000-06-15',
    };
    const fakeUser = { userId: 'uuid-1', email: 'alice@example.com', username: 'alice' };
    mock.onPost('/register', payload).reply(201, fakeUser);

    const result = await registerUser(payload);
    expect(result).toEqual(fakeUser);
  });

  // Error path: duplicate email → server returns 409 with message, function throws with that message
  it('throws when the server returns 409 Conflict (duplicate email)', async () => {
    mock.onPost('/register').reply(409, {
      message: "An account with email 'alice@example.com' already exists.",
    });
    await expect(
      registerUser({ email: 'alice@example.com', username: 'alice', password: 'secret123', dateOfBirth: '2000-06-15' })
    ).rejects.toThrow();
  });
});

// loginUser tests
// 1. Happy path: valid credentials → POST /auth/login with correct payload, returns { token, user }, stores token
// 2. Error path: invalid credentials → server returns 401, function throws, token is NOT stored
describe('loginUser', () => {

  it('POSTs to /auth/login with email and password', async () => {
    const payload  = { email: 'alice@example.com', password: 'secret123' };
    const response = {
      token: 'jwt-abc123',
      user:  { userId: 'uuid-1', email: 'alice@example.com', username: 'alice', status: 'ONLINE' },
    };
    mock.onPost('/login', payload).reply(200, response);

    const result = await loginUser(payload);
    expect(result).toEqual(response);
  });

  it('stores the JWT in tokenStore on successful login', async () => {
    mock.onPost('/login').reply(200, {
      token: 'jwt-abc123',
      user:  { userId: 'uuid-1', email: 'alice@example.com', username: 'alice', status: 'ONLINE' },
    });

    await loginUser({ email: 'alice@example.com', password: 'secret123' });
    expect(tokenStore.get()).toBe('jwt-abc123');
  });

  it('does NOT store a token when credentials are wrong (401)', async () => {
    mock.onPost('/login').reply(401, { message: 'Invalid email or password.' });

    await expect(
      loginUser({ email: 'alice@example.com', password: 'wrong' })
    ).rejects.toThrow();

    expect(tokenStore.get()).toBeNull();
  });
});

// logoutUser tests
// 1. Happy path: token exists → POST /auth/logout, token is cleared from tokenStore
// 2. Even if the logout request fails (network error), the token should still be cleared from tokenStore
describe('logoutUser', () => {

  it('POSTs to /auth/logout', async () => {
    tokenStore.set('jwt-abc123');
    mock.onPost('/logout').reply(200, { message: 'Logged out successfully.' });

    await logoutUser();
    expect(mock.history.post.some((r) => r.url === '/logout')).toBe(true);
  });

  it('clears the token from tokenStore on logout', async () => {
    tokenStore.set('jwt-abc123');
    mock.onPost('/logout').reply(200, { message: 'Logged out successfully.' });

    await logoutUser();
    expect(tokenStore.get()).toBeNull();
  });

  it('clears the token even if the logout request fails', async () => {
    tokenStore.set('jwt-abc123');
    mock.onPost('/logout').networkError();

    await expect(logoutUser()).rejects.toThrow();
    // Token must be cleared regardless — frontend should always discard it
    expect(tokenStore.get()).toBeNull();
  });
});

// getMe tests
// 1. No token in memory → function should return null immediately without making an HTTP call
// 2. Token exists → GET /auth/me, returns UserDto
// 3. Token exists but is expired/invalid → GET /auth/me returns 401, function throws, token is cleared from tokenStore
describe('getMe', () => {

  it('returns null immediately when there is no token in memory', async () => {
    // No token set, so the function should short-circuit without an HTTP call
    const result = await getMe();
    expect(result).toBeNull();
    expect(mock.history.get.length).toBe(0); // no request made
  });

  it('GETs /auth/me and returns UserDto when a token exists', async () => {
    tokenStore.set('jwt-abc123');
    const fakeUser = {
      userId: 'uuid-1', email: 'alice@example.com',
      username: 'alice', status: 'ONLINE',
    };
    mock.onGet('/me').reply(200, fakeUser);

    const result = await getMe();
    expect(result).toEqual(fakeUser);
  });

  it('clears the token when /auth/me returns 401', async () => {
    tokenStore.set('expired-token');
    mock.onGet('/me').reply(401, { message: 'Unauthorized' });

    await expect(getMe()).rejects.toThrow();
    expect(tokenStore.get()).toBeNull(); // cleared by the response interceptor
  });
});