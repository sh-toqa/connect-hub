/**
 * Displays the user's friends list with online/offline status dots.
 * Used on the profile page right sidebar.
 */
export default function FriendsList({ friends }) {
  if (!friends || friends.length === 0) {
    return <p className="empty-state">No friends yet. Start connecting!</p>;
  }

  return (
    <ul className="friends-list" aria-label="Friends list">
      {friends.map(friend => {
        const avatarSrc = friend.profilePhotoPath
          ? `http://localhost:8080${friend.profilePhotoPath}`
          : null;
        const initials = friend.username?.slice(0, 2).toUpperCase() || '??';
        const isOnline  = friend.status === 'ONLINE';

        return (
          <li key={friend.userId} className="friend-item">
            <div className="friend-avatar-wrap">
              {avatarSrc
                ? <img src={avatarSrc} alt={friend.username} className="friend-avatar" />
                : <span className="friend-avatar-initials">{initials}</span>}
              <span
                className={`status-dot ${isOnline ? 'online' : 'offline'}`}
                title={isOnline ? 'Online' : 'Offline'}
                aria-label={isOnline ? 'Online' : 'Offline'}
              />
            </div>
            <span className="friend-name">{friend.username}</span>
          </li>
        );
      })}
    </ul>
  );
}
