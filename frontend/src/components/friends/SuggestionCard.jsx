import { useState } from 'react';
import { Link } from 'react-router-dom';
import { API_BASE_URL } from '../../config/api';

export default function SuggestionCard({ user, onSend }) {
  const [sent, setSent] = useState(false);
  const avatarSrc = user?.profilePhotoPath
    ? `${API_BASE_URL}${user.profilePhotoPath}`
    : null;
  const initials = user?.username?.slice(0, 2).toUpperCase() || '??';

  const handleSend = async () => {
    await onSend(user.userId);
    setSent(true);
  };

  return (
    <div className="friend-card">
      <Link to={`/user/${user?.userId}`} className="friend-card-info">
        <div className="friend-card-avatar">
          {avatarSrc
            ? <img src={avatarSrc} alt={user?.username} />
            : <span>{initials}</span>}
        </div>
        <div>
          <p className="friend-card-name">{user?.username}</p>
          <p className="friend-card-sub">{user?.bio || 'ConnectHub member'}</p>
        </div>
      </Link>
      <button
        className={sent ? 'btn-secondary friend-btn' : 'btn-primary friend-btn'}
        onClick={handleSend}
        disabled={sent}
      >
        {sent ? '✓ Sent' : '+ Add Friend'}
      </button>
    </div>
  );
}
