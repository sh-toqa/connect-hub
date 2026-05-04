import { useState, useEffect, useCallback } from 'react';
import {
  getMyProfile, getMyPosts, getMyFriends,
  updateProfile, uploadProfilePhoto, uploadCoverPhoto, updatePassword,
} from '../api/profileApi';
import { useAuth } from '../context/AuthContext';

/**
 * Encapsulates all profile data fetching and mutation.
 * Components consume this hook instead of calling the API directly.
 */
export function useProfile() {
  const { refreshUser } = useAuth();

  const [profile,  setProfile]  = useState(null);
  const [posts,    setPosts]     = useState([]);
  const [friends,  setFriends]   = useState([]);
  const [loading,  setLoading]   = useState(true);
  const [error,    setError]     = useState(null);

  // Pagination state for posts
  const [page,     setPage]      = useState(0);
  const [hasMore,  setHasMore]   = useState(true);

  // ── Initial data load ─────────────────────────────────────────────────────
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
      setPosts(postsRes.data.content);
      setHasMore(!postsRes.data.last);
      setFriends(friendsRes.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchProfile(); }, [fetchProfile]);

  // ── Load more posts (pagination) ─────────────────────────────────────────
  const loadMorePosts = useCallback(async () => {
    if (!hasMore) return;
    const nextPage = page + 1;
    const res = await getMyPosts(nextPage, 10);
    setPosts(prev => [...prev, ...res.data.content]);
    setHasMore(!res.data.last);
    setPage(nextPage);
  }, [page, hasMore]);

  // ── Update bio ────────────────────────────────────────────────────────────
  const saveBio = useCallback(async (bio) => {
    const { data } = await updateProfile({ bio });
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  // ── Upload profile photo ──────────────────────────────────────────────────
  const saveProfilePhoto = useCallback(async (file) => {
    const { data } = await uploadProfilePhoto(file);
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  // ── Upload cover photo ────────────────────────────────────────────────────
  const saveCoverPhoto = useCallback(async (file) => {
    const { data } = await uploadCoverPhoto(file);
    setProfile(data);
    refreshUser(data);
    return data;
  }, [refreshUser]);

  // ── Change password ───────────────────────────────────────────────────────
  const changePassword = useCallback(async (currentPassword, newPassword) => {
    await updatePassword({ currentPassword, newPassword });
  }, []);

  return {
    profile, posts, friends,
    loading, error,
    hasMore, loadMorePosts,
    saveBio, saveProfilePhoto, saveCoverPhoto, changePassword,
    reload: fetchProfile,
  };
}