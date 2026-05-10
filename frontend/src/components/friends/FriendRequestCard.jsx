import { Link } from 'react-router-dom';

export default function FriendRequestCard({ request, onAccept, onDecline }) {
  const { friendshipId, requester } = request;
  const avatarSrc = requester?.profilePhotoPath
    ? `http://localhost:8080${requester.profilePhotoPath}`
    : null;
  const initials = requester?.username?.slice(0, 2).toUpperCase() || '??';

  return (
    <div className="friend-card">
      <Link to={`/user/${requester?.userId}`} className="friend-card-info">
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
