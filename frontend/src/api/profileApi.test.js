import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import api from './authApi';
import MockAdapter from 'axios-mock-adapter';
import {
  getMyProfile,
  getUserProfile,
  updateProfile,
  updatePassword,
  getMyPosts,
  getMyFriends,
  uploadProfilePhoto,
  uploadCoverPhoto,
} from './profileApi';

let mock;

beforeEach(() => {
  mock = new MockAdapter(api);
  sessionStorage.setItem('token', 'jwt-abc123'); // simulate logged-in user
});

afterEach(() => {
  mock.restore();
  sessionStorage.clear();
});

// Fake data for testing  
const fakeProfile = {
  userId:          'uuid-1',
  username:        'alice',
  email:           'alice@example.com',
  bio:             'Hello!',
  profilePhotoPath: '/uploads/profiles/alice.jpg',
  coverPhotoPath:  '/uploads/covers/alice-cover.jpg',
  status:          'ONLINE',
};

const fakePosts = {
  content: [
    { contentId: 'post-1', contentText: 'Hello world', timestamp: '2024-01-01T10:00:00' },
    { contentId: 'post-2', contentText: 'Second post',  timestamp: '2024-01-02T10:00:00' },
  ],
  last: false,
  totalElements: 2,
};

const fakeFriends = [
  { userId: 'uuid-2', username: 'bob',   status: 'ONLINE'  },
  { userId: 'uuid-3', username: 'carol', status: 'OFFLINE' },
];


describe('getMyProfile', () => {

  it('GETs /profile and returns the user profile', async () => {
    mock.onGet('/profile').reply(200, fakeProfile);

    const res = await getMyProfile();
    expect(res.data).toEqual(fakeProfile);
  });

  it('sends Authorization header with the token', async () => {
    mock.onGet('/profile').reply(200, fakeProfile);

    await getMyProfile();
    expect(mock.history.get[0].headers['Authorization']).toBe('Bearer jwt-abc123');
  });

  it('throws on 401 Unauthorized', async () => {
    mock.onGet('/profile').reply(401, { message: 'Unauthorized' });

    await expect(getMyProfile()).rejects.toThrow();
  });

  it('throws on 500 Internal Server Error', async () => {
    mock.onGet('/profile').reply(500, { message: 'An unexpected error occurred' });

    await expect(getMyProfile()).rejects.toThrow();
  });
});

describe('getUserProfile', () => {

  it('GETs /profile/:userId with the correct user ID', async () => {
    const userId = 'uuid-2';
    mock.onGet(`/profile/${userId}`).reply(200, { ...fakeProfile, userId });

    const res = await getUserProfile(userId);
    expect(res.data.userId).toBe(userId);
  });

  it('throws on 404 when user does not exist', async () => {
    mock.onGet('/profile/non-existent').reply(404, { message: 'User not found' });

    await expect(getUserProfile('non-existent')).rejects.toThrow();
  });
});

describe('updateProfile', () => {

  it('PATCHes /profile with the provided data', async () => {
    const update = { bio: 'New bio text' };
    const updated = { ...fakeProfile, bio: 'New bio text' };
    mock.onPatch('/profile', update).reply(200, updated);

    const res = await updateProfile(update);
    expect(res.data.bio).toBe('New bio text');
  });

  it('sends the correct payload in the request body', async () => {
    const update = { bio: 'Updated!' };
    mock.onPatch('/profile').reply(200, fakeProfile);

    await updateProfile(update);
    expect(JSON.parse(mock.history.patch[0].data)).toEqual(update);
  });

  it('throws on 400 when validation fails', async () => {
    mock.onPatch('/profile').reply(400, {
      fieldErrors: { bio: 'Bio cannot exceed 300 characters' },
    });

    await expect(updateProfile({ bio: 'x'.repeat(301) })).rejects.toThrow();
  });
});

describe('updatePassword', () => {

  it('PATCHes /profile/password with current and new password', async () => {
    const payload = { currentPassword: 'OldPass1!', newPassword: 'NewPass2!' };
    mock.onPatch('/profile/password', payload).reply(204);

    const res = await updatePassword(payload);
    expect(res.status).toBe(204);
  });

  it('throws on 401 when current password is wrong', async () => {
    mock.onPatch('/profile/password').reply(401, { message: 'Current password is incorrect' });

    await expect(
      updatePassword({ currentPassword: 'wrong', newPassword: 'NewPass2!' })
    ).rejects.toThrow();
  });

  it('sends the correct payload in the request body', async () => {
    const payload = { currentPassword: 'OldPass1!', newPassword: 'NewPass2!' };
    mock.onPatch('/profile/password').reply(204);

    await updatePassword(payload);
    expect(JSON.parse(mock.history.patch[0].data)).toEqual(payload);
  });
});

describe('getMyPosts', () => {

  it('GETs /profile/posts with default page and size', async () => {
    mock.onGet('/profile/posts').reply(200, fakePosts);

    const res = await getMyPosts();
    expect(res.data.content).toHaveLength(2);
  });

  it('sends page and size as query params', async () => {
    mock.onGet('/profile/posts').reply(200, fakePosts);

    await getMyPosts(2, 5);
    expect(mock.history.get[0].params).toEqual({ page: 2, size: 5 });
  });

  it('defaults to page=0 and size=10', async () => {
    mock.onGet('/profile/posts').reply(200, fakePosts);

    await getMyPosts();
    expect(mock.history.get[0].params).toEqual({ page: 0, size: 10 });
  });

  it('returns empty content array when user has no posts', async () => {
    mock.onGet('/profile/posts').reply(200, { content: [], last: true, totalElements: 0 });

    const res = await getMyPosts();
    expect(res.data.content).toEqual([]);
    expect(res.data.last).toBe(true);
  });
});

describe('getMyFriends', () => {

  it('GETs /profile/friends and returns friends list', async () => {
    mock.onGet('/profile/friends').reply(200, fakeFriends);

    const res = await getMyFriends();
    expect(res.data).toEqual(fakeFriends);
  });

  it('returns empty array when user has no friends', async () => {
    mock.onGet('/profile/friends').reply(200, []);

    const res = await getMyFriends();
    expect(res.data).toEqual([]);
  });

  it('includes online/offline status for each friend', async () => {
    mock.onGet('/profile/friends').reply(200, fakeFriends);

    const res = await getMyFriends();
    expect(res.data[0].status).toBe('ONLINE');
    expect(res.data[1].status).toBe('OFFLINE');
  });
});

describe('uploadProfilePhoto', () => {

  it('POSTs to /profile/photo with multipart/form-data', async () => {
    const updatedProfile = { ...fakeProfile, profilePhotoPath: '/uploads/profiles/new.jpg' };
    mock.onPost('/profile/photo').reply(200, updatedProfile);

    const file = new File(['image content'], 'photo.jpg', { type: 'image/jpeg' });
    const res  = await uploadProfilePhoto(file);

    expect(res.data.profilePhotoPath).toBe('/uploads/profiles/new.jpg');
  });

  it('sends the file inside a FormData object', async () => {
    mock.onPost('/profile/photo').reply(200, fakeProfile);

    const file = new File(['img'], 'avatar.jpg', { type: 'image/jpeg' });
    await uploadProfilePhoto(file);

    // FormData is sent as the request data
    expect(mock.history.post[0].data).toBeInstanceOf(FormData);
  });

  it('throws on 400 when file type is invalid', async () => {
    mock.onPost('/profile/photo').reply(400, { message: 'Invalid file type' });

    const file = new File(['text'], 'script.js', { type: 'application/javascript' });
    await expect(uploadProfilePhoto(file)).rejects.toThrow();
  });

  it('throws on 400 when file is too large', async () => {
    mock.onPost('/profile/photo').reply(400, { message: 'File size exceeds the 5 MB limit' });

    const file = new File(['x'.repeat(6_000_000)], 'big.jpg', { type: 'image/jpeg' });
    await expect(uploadProfilePhoto(file)).rejects.toThrow();
  });
});

describe('uploadCoverPhoto', () => {

  it('POSTs to /profile/cover with multipart/form-data', async () => {
    const updatedProfile = { ...fakeProfile, coverPhotoPath: '/uploads/covers/new-cover.jpg' };
    mock.onPost('/profile/cover').reply(200, updatedProfile);

    const file = new File(['image'], 'cover.jpg', { type: 'image/jpeg' });
    const res  = await uploadCoverPhoto(file);

    expect(res.data.coverPhotoPath).toBe('/uploads/covers/new-cover.jpg');
  });

  it('sends the file inside a FormData object', async () => {
    mock.onPost('/profile/cover').reply(200, fakeProfile);

    const file = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });
    await uploadCoverPhoto(file);

    expect(mock.history.post[0].data).toBeInstanceOf(FormData);
  });

  it('throws on 500 server error during upload', async () => {
    mock.onPost('/profile/cover').reply(500, { message: 'An unexpected error occurred' });

    const file = new File(['img'], 'cover.jpg', { type: 'image/jpeg' });
    await expect(uploadCoverPhoto(file)).rejects.toThrow();
  });
});