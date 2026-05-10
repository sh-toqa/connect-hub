import { useState, useEffect, useCallback } from 'react';
import {
  getPendingRequests, getFriends, getSuggestions,
  sendRequest, acceptRequest, declineRequest,
  removeFriend, blockUser, unblockUser,
} from '../api/friendApi';

export function useFriends() {
  const [requests,    setRequests]    = useState([]);
  const [friends,     setFriends]     = useState([]);  // FriendshipDto[]
  const [suggestions, setSuggestions] = useState([]);
  const [loading,     setLoading]     = useState(true);
  const [error,       setError]       = useState(null);

  const fetchAll = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const [reqRes, friendsRes, sugRes] = await Promise.all([
        getPendingRequests(),
        getFriends(),       // returns FriendshipDto[]
        getSuggestions(),
      ]);
      setRequests(reqRes.data    ?? []);
      setFriends(friendsRes.data ?? []);
      setSuggestions(sugRes.data ?? []);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load friends');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  const accept = useCallback(async (friendshipId) => {
    await acceptRequest(friendshipId);
    setRequests(prev => prev.filter(r => r.friendshipId !== friendshipId));
    fetchAll();
  }, [fetchAll]);

  const decline = useCallback(async (friendshipId) => {
    await declineRequest(friendshipId);
    setRequests(prev => prev.filter(r => r.friendshipId !== friendshipId));
  }, []);

  // friendshipId comes directly from FriendshipDto
  const remove = useCallback(async (friendshipId) => {
    await removeFriend(friendshipId);
    setFriends(prev => prev.filter(f => f.friendshipId !== friendshipId));
  }, []);

  const send = useCallback(async (receiverId) => {
    await sendRequest(receiverId);
    setSuggestions(prev => prev.filter(s => s.userId !== receiverId));
  }, []);

  const block = useCallback(async (targetId, friendshipId) => {
    await blockUser(targetId);
    if (friendshipId) {
      setFriends(prev => prev.filter(f => f.friendshipId !== friendshipId));
    }
    setSuggestions(prev => prev.filter(s => s.userId !== targetId));
  }, []);

  const unblock = useCallback(async (targetId) => {
    await unblockUser(targetId);
    fetchAll();
  }, [fetchAll]);

  return {
    requests, friends, suggestions,
    loading, error,
    accept, decline, remove, send, block, unblock,
    reload: fetchAll,
  };
}
