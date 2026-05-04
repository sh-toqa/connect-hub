// src/components/profile/FriendCard.jsx
// FR-PM-07, FR-UA-11: friend with username, photo, and coloured status dot

export default function FriendCard({ friend }) {
  const isOnline = friend.status === 'ONLINE';
  const initials = friend.username?.[0]?.toUpperCase() ?? '?';

  return (
    <div className="friend-card">
      <div className="friend-card__avatar-wrap">
        {friend.profilePhotoPath ? (
          <img
            src={friend.profilePhotoPath}
            alt={`${friend.username}'s avatar`}
            className="friend-card__avatar"
            loading="lazy"
          />
        ) : (
          <div className="friend-card__avatar friend-card__avatar--placeholder">
            {initials}
          </div>
        )}
        <span
          className={`status-dot status-dot--${isOnline ? 'online' : 'offline'}`}
          aria-label={isOnline ? 'Online' : 'Offline'}
          role="img"
        />
      </div>
      <div className="friend-card__info">
        <span className="friend-card__name">@{friend.username}</span>
        <span className={`friend-card__status ${isOnline ? 'online' : 'offline'}`}>
          {isOnline ? 'Online' : 'Offline'}
        </span>
      </div>
    </div>
  );
}