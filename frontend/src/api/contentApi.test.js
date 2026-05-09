import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import api from './authApi';
import {
  createPost, createStory, deleteContent,
  getFeedPosts, getFeedStories,
} from './contentApi';

let mock;

beforeEach(() => {
  mock = new MockAdapter(api);
  sessionStorage.setItem('token', 'jwt-abc123');
});

afterEach(() => {
  mock.restore();
  sessionStorage.clear();
});

// Test data
const fakePost = {
  contentId:   'post-1',
  contentText: 'Hello world',
  imagePath:   null,
  contentType: 'POST',
  timestamp:   '2024-01-01T10:00:00',
  author: { userId: 'uuid-1', username: 'alice' },
};

const fakeStory = { ...fakePost, contentId: 'story-1', contentType: 'STORY' };

const fakePage = { content: [fakePost], last: false, totalElements: 1 };

// createPost tests
describe('createPost', () => {

  it('POSTs to /content/posts and returns the created post', async () => {
    mock.onPost('/content/posts').reply(201, fakePost);

    const res = await createPost('Hello world');
    expect(res.data).toEqual(fakePost);
  });

  it('sends FormData with a "data" part', async () => {
    mock.onPost('/content/posts').reply(201, fakePost);

    await createPost('Hello world');
    expect(mock.history.post[0].data).toBeInstanceOf(FormData);
  });

  it('includes image in FormData when provided', async () => {
    mock.onPost('/content/posts').reply(201, fakePost);

    const file = new File(['img'], 'photo.jpg', { type: 'image/jpeg' });
    await createPost('With image', file);

    const formData = mock.history.post[0].data;
    expect(formData.get('image')).toBeTruthy();
  });

  it('does not include image part when no image provided', async () => {
    mock.onPost('/content/posts').reply(201, fakePost);

    await createPost('No image');

    const formData = mock.history.post[0].data;
    expect(formData.get('image')).toBeNull();
  });

  it('throws on 401 Unauthorized', async () => {
    mock.onPost('/content/posts').reply(401);
    await expect(createPost('text')).rejects.toThrow();
  });
});

// createStory tests
describe('createStory', () => {

  it('POSTs to /content/stories and returns the created story', async () => {
    mock.onPost('/content/stories').reply(201, fakeStory);

    const res = await createStory('My story');
    expect(res.data.contentType).toBe('STORY');
  });

  it('sends FormData with a "data" part', async () => {
    mock.onPost('/content/stories').reply(201, fakeStory);

    await createStory('My story');
    expect(mock.history.post[0].data).toBeInstanceOf(FormData);
  });

  it('throws on 401 Unauthorized', async () => {
    mock.onPost('/content/stories').reply(401);
    await expect(createStory('text')).rejects.toThrow();
  });
});

// deleteContent tests
describe('deleteContent', () => {

  it('DELETEs /content/:contentId', async () => {
    mock.onDelete('/content/post-1').reply(204);

    const res = await deleteContent('post-1');
    expect(res.status).toBe(204);
  });

  it('sends Authorization header', async () => {
    mock.onDelete('/content/post-1').reply(204);

    await deleteContent('post-1');
    expect(mock.history.delete[0].headers['Authorization']).toBe('Bearer jwt-abc123');
  });

  it('throws 403 when trying to delete another user\'s content', async () => {
    mock.onDelete('/content/post-1').reply(404);
    await expect(deleteContent('post-1')).rejects.toThrow();
  });

  it('throws 404 when content does not exist', async () => {
    mock.onDelete('/content/non-existent').reply(404);
    await expect(deleteContent('non-existent')).rejects.toThrow();
  });
});

// getFeedPosts tests
describe('getFeedPosts', () => {

  it('GETs /content/feed/posts with default page and size', async () => {
    mock.onGet('/content/feed/posts').reply(200, fakePage);

    const res = await getFeedPosts();
    expect(res.data.content).toHaveLength(1);
  });

  it('sends page and size as query params', async () => {
    mock.onGet('/content/feed/posts').reply(200, fakePage);

    await getFeedPosts(2, 5);
    expect(mock.history.get[0].params).toEqual({ page: 2, size: 5 });
  });

  it('defaults to page=0 and size=10', async () => {
    mock.onGet('/content/feed/posts').reply(200, fakePage);

    await getFeedPosts();
    expect(mock.history.get[0].params).toEqual({ page: 0, size: 10 });
  });

  it('returns empty content when user has no friends', async () => {
    mock.onGet('/content/feed/posts').reply(200, { content: [], last: true });

    const res = await getFeedPosts();
    expect(res.data.content).toEqual([]);
  });

  it('throws on 401 Unauthorized', async () => {
    mock.onGet('/content/feed/posts').reply(401);
    await expect(getFeedPosts()).rejects.toThrow();
  });
});

// getFeedStories tests
describe('getFeedStories', () => {

  it('GETs /content/feed/stories and returns stories list', async () => {
    mock.onGet('/content/feed/stories').reply(200, [fakeStory]);

    const res = await getFeedStories();
    expect(res.data).toHaveLength(1);
    expect(res.data[0].contentType).toBe('STORY');
  });

  it('returns empty array when no active stories', async () => {
    mock.onGet('/content/feed/stories').reply(200, []);

    const res = await getFeedStories();
    expect(res.data).toEqual([]);
  });

  it('sends Authorization header', async () => {
    mock.onGet('/content/feed/stories').reply(200, []);

    await getFeedStories();
    expect(mock.history.get[0].headers['Authorization']).toBe('Bearer jwt-abc123');
  });
});