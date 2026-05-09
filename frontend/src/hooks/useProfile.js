import { useState, useEffect, useCallback } from 'react';
import {
  getMyProfile, getMyPosts, getMyStories, getMyFriends,
  updateProfile, uploadProfilePhoto, uploadCoverPhoto, updatePassword,
} from '../api/profileApi';
import { useAuth } from '../context/AuthContext';
import { deleteContent } from '../api/contentApi';

export function useProfile() {
  const { refreshUser } = useAuth();

  const [profile,  setProfile]  = useState(null);
  const [posts,    setPosts]    = useState([]);
  const [stories,  setStories]  = useState([]);
  const [friends,  setFriends]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);
  const [page,     setPage]     = useState(0);
  const [hasMore,  setHasMore]  = useState(false);

  const fetchProfile = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const [profileRes, postsRes, friendsRes] = await Promise.all([
        getMyProfile(),
        getMyPosts(0, 10),
        getMyFriends(),
      ]);

      setProfile(profileRes.data);
      setPosts(postsRes.data.content ?? []);
      setHasMore(!postsRes.data.last);
      setFriends(friendsRes.data ?? []);

      try {
        const storiesRes = await getMyStories();
        setStories(storiesRes.data.content ?? []);
      } catch {
        setStories([]);
      }

    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchProfile(); }, [fetchProfile]);

  const loadMorePosts = useCallback(async () => {
    if (!hasMore) return;
    const nextPage = page + 1;
    const res = await getMyPosts(nextPage, 10);
    setPosts(prev => [...prev, ...res.data.content]);
    setHasMore(!res.data.last);
    setPage(nextPage);
  }, [page, hasMore]);

  const saveBio = useCallback(async (bio) => {
    const { data } = await updateProfile({ bio });
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  const saveProfilePhoto = useCallback(async (file) => {
    const { data } = await uploadProfilePhoto(file);
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  const saveCoverPhoto = useCallback(async (file) => {
    const { data } = await uploadCoverPhoto(file);
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  const changePassword = useCallback(async (currentPassword, newPassword) => {
    await updatePassword({ currentPassword, newPassword });
  }, []);

  const removePost = useCallback(async (contentId) => {
    await deleteContent(contentId);

    setPosts(prev =>
      prev.filter(p => p.contentId !== contentId)
    );

    setStories(prev =>
      prev.filter(s => s.contentId !== contentId)
    );
  }, []);

  return {
    profile, posts, stories, friends,
    loading, error,
    hasMore, loadMorePosts,
    saveBio, saveProfilePhoto, saveCoverPhoto, changePassword,
    removePost,
    reload: fetchProfile,
  };
}