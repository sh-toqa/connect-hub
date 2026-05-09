import { useState, useEffect, useCallback } from 'react';
import {
  getFeedPosts, getFeedStories,
  createPost, createStory, deleteContent,
} from '../api/contentApi';

export function useContent() {
  const [posts,    setPosts]    = useState([]);
  const [stories,  setStories]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);
  const [page,     setPage]     = useState(0);
  const [hasMore,  setHasMore]  = useState(false);

  const fetchFeed = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [postsRes, storiesRes] = await Promise.all([
        getFeedPosts(0, 10),
        getFeedStories(),
      ]);

      setPosts(postsRes.data.content ?? []);
      setHasMore(!postsRes.data.last);
      setPage(0);
      setStories(storiesRes.data ?? []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load feed');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchFeed(); }, [fetchFeed]);

  // Pagination for feed posts
  const loadMorePosts = useCallback(async () => {
    if (!hasMore) return;
    const nextPage = page + 1;
    const res = await getFeedPosts(nextPage, 10);
    setPosts(prev => [...prev, ...res.data.content]);
    setHasMore(!res.data.last);
    setPage(nextPage);
  }, [page, hasMore]);

  // Content creation
  const submitPost = useCallback(async (contentText, image) => {
    const res = await createPost(contentText, image);
    // Prepend new post to the top of the feed
    setPosts(prev => [res.data, ...prev]);
    return res.data;
  }, []);

  const submitStory = useCallback(async (contentText, image) => {
    const res = await createStory(contentText, image);
    setStories(prev => [res.data, ...prev]);
    return res.data;
  }, []);

  // Content deletion
  const removeContent = useCallback(async (contentId) => {
    await deleteContent(contentId);
    setPosts(prev  => prev.filter(p => p.contentId !== contentId));
    setStories(prev => prev.filter(s => s.contentId !== contentId));
  }, []);

  return {
    posts, stories, loading, error, hasMore,
    loadMorePosts, submitPost, submitStory, removeContent,
    reload: fetchFeed,
  };
}