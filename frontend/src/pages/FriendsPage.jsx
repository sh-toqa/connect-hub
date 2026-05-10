import { useFriends }        from '../hooks/useFriends';
import FriendRequestCard     from '../components/friends/FriendRequestCard';
import FriendCard            from '../components/friends/FriendCard';
import SuggestionCard        from '../components/friends/SuggestionCard';
import '../components/friends/friends.css';

export default function FriendsPage() {
  const {
    requests, friends, suggestions,
    loading, error,
    accept, decline, remove, send, block,
    reload,
  } = useFriends();

  if (loading) {
    return (
      <div className="friends-loading" role="status">
        <div className="spinner" />
        <p>Loading…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="friends-loading" role="alert">
        <p>⚠ {error}</p>
        <button className="btn-secondary" onClick={reload}>Retry</button>
      </div>
    );
  }

  return (
    <div className="friends-page">

      {/* Friend Requests */}
      <div className="friends-section">
        <div className="friends-section-header">
          <h2>👋 Friend Requests</h2>
          {requests.length > 0 && (
            <span className="friends-section-count">{requests.length}</span>
          )}
        </div>
        {requests.length === 0
          ? <p className="friends-empty">No pending friend requests.</p>
          : requests.map(req => (
              <FriendRequestCard
                key={req.friendshipId}
                request={req}
                onAccept={accept}
                onDecline={decline}
              />
            ))
        }
      </div>

      {/* Friends List */}
      <div className="friends-section">
        <div className="friends-section-header">
          <h2>👥 Friends</h2>
          <span className="friends-section-count">{friends.length}</span>
        </div>
        {friends.length === 0
          ? <p className="friends-empty">No friends yet — check the suggestions below!</p>
          : friends.map(friendship => (
              <FriendCard
                key={friendship.friendshipId}
                friendship={friendship}
                onRemove={remove}
                onBlock={block}
              />
            ))
        }
      </div>

      {/* Suggestions */}
      <div className="friends-section">
        <div className="friends-section-header">
          <h2>💡 People You May Know</h2>
        </div>
        {suggestions.length === 0
          ? <p className="friends-empty">No suggestions right now.</p>
          : suggestions.map(user => (
              <SuggestionCard
                key={user.userId}
                user={user}
                onSend={send}
              />
            ))
        }
      </div>

    </div>
  );
}
