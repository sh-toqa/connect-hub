import { Link } from 'react-router-dom';
import { API_BASE_URL } from '../../config/api';

export default function FriendRequestCard({ request, onAccept, onDecline }) {
  const { friendshipId, requester } = request;
  const avatarSrc = requester?.profilePhotoPath
    ? `${API_BASE_URL}${requester.profilePhotoPath}`
    : null;
  const initials = requester?.username?.slice(0, 2).toUpperCase() || '??';

  return (
    <div className="friend-card">
      <Link to={`/profile/${requester?.userId}/posts`} className="friend-card-info">
        <div className="friend-card-avatar">
          {avatarSrc
            ? <img src={avatarSrc} alt={requester?.username} />
            : <span>{initials}</span>}
        </div>
        <div>
          <p className="friend-card-name">{requester?.username}</p>
          <p className="friend-card-sub">{requester?.bio || requester?.email}</p>
        </div>
      </Link>
      <div className="friend-card-actions">
        <button
          className="btn-primary friend-btn"
          onClick={() => onAccept(friendshipId)}
        >
          Accept
        </button>
        <button
          className="btn-secondary friend-btn"
          onClick={() => onDecline(friendshipId)}
        >
          Decline
        </button>
      </div>
    </div>
  );
}
