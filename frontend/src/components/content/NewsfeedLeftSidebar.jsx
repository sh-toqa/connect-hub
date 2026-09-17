import { Link }    from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { API_BASE_URL } from '../../config/api';

export default function NewsfeedLeftSidebar() {
  const { user } = useAuth();

  const avatarSrc = user?.profilePhotoPath
    ? `${API_BASE_URL}${user.profilePhotoPath}`
    : null;
  const initials = user?.username?.slice(0, 2).toUpperCase() || '??';

  return (
    <aside className="feed-sidebar feed-sidebar--left" aria-label="User info">

      {/* Profile card */}
      <Link to="/profile" className="sidebar-profile-card">
        <div className="sidebar-avatar">
          {avatarSrc
            ? <img src={avatarSrc} alt={user?.username} />
            : <span>{initials}</span>}
        </div>
        <div className="sidebar-profile-info">
          <p className="sidebar-username">{user?.username}</p>
          <p className="sidebar-bio">{user?.bio || 'View your profile'}</p>
        </div>
      </Link>

      <div className="sidebar-divider" />


    </aside>
  );
}
