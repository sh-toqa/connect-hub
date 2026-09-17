import { useState, useEffect, useCallback } from 'react';
import { Link }                from 'react-router-dom';
import { getFriends, getSuggestions, sendRequest } from '../../api/friendApi';
import { useAuth }             from '../../context/AuthContext';
import { API_BASE_URL } from '../../config/api';

export default function NewsfeedRightSidebar() {
  const { user: currentUser } = useAuth();
  const [friends,     setFriends]     = useState([]);
  const [suggestions, setSuggestions] = useState([]);

  const load = useCallback(async () => {
    try {
      const [fRes, sRes] = await Promise.all([getFriends(), getSuggestions()]);
      setFriends(fRes.data     ?? []);
      setSuggestions((sRes.data ?? []).slice(0, 5));
    } catch { /* silent */ }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleSend = async (userId) => {
    await sendRequest(userId);
    setSuggestions(prev => prev.filter(s => s.userId !== userId));
  };

  return (
    <aside className="feed-sidebar feed-sidebar--right" aria-label="Friends and suggestions">

      {/* Friends list */}
      <div className="sidebar-section">
        <h3 className="sidebar-section-title">
          Friends
          <Link to="/friends" className="sidebar-section-link">See all</Link>
        </h3>

        {friends.length === 0
          ? <p className="sidebar-empty">No friends yet.</p>
          : friends.map(f => {
              // FriendshipDto — extract the friend (not the current user)
              const friend   = f.requester?.userId === currentUser?.userId
                ? f.receiver
                : f.requester;
              const isOnline = friend?.status === 'ONLINE';
              const avatarSrc = friend?.profilePhotoPath
                ? `${API_BASE_URL}${friend.profilePhotoPath}`
                : null;
              const initials = friend?.username?.slice(0, 2).toUpperCase() || '??';

              return (
                <Link
                  key={f.friendshipId}
                  to={`/user/${friend?.userId}`}
                  className="sidebar-friend-item"
                >
                  <div className="sidebar-friend-avatar-wrap">
                    <div className="sidebar-friend-avatar">
                      {avatarSrc
                        ? <img src={avatarSrc} alt={friend?.username} />
                        : <span>{initials}</span>}
                    </div>
                    <span
                      className={`status-dot ${isOnline ? 'online' : 'offline'}`}
                      style={{ position: 'absolute', bottom: 1, right: 1,
                               width: 10, height: 10, border: '2px solid #fff', borderRadius: '50%' }}
                    />
                  </div>
                  <span className="sidebar-friend-name">{friend?.username}</span>
                </Link>
              );
            })
        }
      </div>

      <div className="sidebar-divider" />

      {/* Suggestions */}
      {suggestions.length > 0 && (
        <div className="sidebar-section">
          <h3 className="sidebar-section-title">
            People You May Know
            <Link to="/friends" className="sidebar-section-link">See all</Link>
          </h3>
          {suggestions.map(user => {
            const avatarSrc = user?.profilePhotoPath
              ? `${API_BASE_URL}${user.profilePhotoPath}`
              : null;
            const initials = user?.username?.slice(0, 2).toUpperCase() || '??';

            return (
              <div key={user.userId} className="sidebar-suggestion-item">
                <Link to={`/user/${user.userId}`} className="sidebar-suggestion-info">
                  <div className="sidebar-friend-avatar">
                    {avatarSrc
                      ? <img src={avatarSrc} alt={user.username} />
                      : <span>{initials}</span>}
                  </div>
                  <span className="sidebar-friend-name">{user.username}</span>
                </Link>
                <button className="sidebar-add-btn" onClick={() => handleSend(user.userId)}>
                  + Add
                </button>
              </div>
            );
          })}
        </div>
      )}

    </aside>
  );
}