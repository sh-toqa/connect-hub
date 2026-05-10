import { useState } from 'react';
import { Link }     from 'react-router-dom';
import { useAuth }  from '../../context/AuthContext';

/**
 * Receives a FriendshipDto — { friendshipId, requester, receiver, status }
 * Determines which side is "the friend" based on currentUserId.
 */
export default function FriendCard({ friendship, onRemove, onBlock }) {
  const { user: currentUser } = useAuth();
  const [showMenu, setShowMenu] = useState(false);

  // The friend is whichever side is NOT the current user
  const friend = friendship.requester?.userId === currentUser?.userId
    ? friendship.receiver
    : friendship.requester;

  const avatarSrc = friend?.profilePhotoPath
    ? `http://localhost:8080${friend.profilePhotoPath}`
    : null;
  const initials  = friend?.username?.slice(0, 2).toUpperCase() || '??';
  const isOnline  = friend?.status === 'ONLINE';

  const handleRemove = () => {
    onRemove(friendship.friendshipId);
    setShowMenu(false);
  };

  const handleBlock = () => {
    onBlock(friend?.userId, friendship.friendshipId);
    setShowMenu(false);
  };

  return (
    <div className="friend-card">
      <Link to={`/user/${friend?.userId}`} className="friend-card-info">
        <div className="friend-card-avatar-wrap" style={{ position: 'relative' }}>
          <div className="friend-card-avatar">
            {avatarSrc
              ? <img src={avatarSrc} alt={friend?.username} />
              : <span>{initials}</span>}
          </div>
          <span
            className={`status-dot ${isOnline ? 'online' : 'offline'}`}
            style={{ position: 'absolute', bottom: 1, right: 1,
                     width: 11, height: 11, border: '2px solid #fff', borderRadius: '50%' }}
          />
        </div>
        <div>
          <p className="friend-card-name">{friend?.username}</p>
          <p className="friend-card-sub">{isOnline ? '🟢 Online' : '⚫ Offline'}</p>
        </div>
      </Link>

      <div style={{ position: 'relative' }}>
        <button
          className="friend-menu-btn"
          onClick={() => setShowMenu(p => !p)}
          aria-label="Friend options"
        >
          ···
        </button>
        {showMenu && (
          <div className="friend-menu-dropdown">
            <button onClick={handleRemove}>👋 Remove Friend</button>
            <button className="danger" onClick={handleBlock}>🚫 Block</button>
          </div>
        )}
      </div>
    </div>
  );
}
