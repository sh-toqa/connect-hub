import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { API_BASE_URL } from '../../config/api';

export default function Navbar() {
  const { user, logout } = useAuth();
  const navigate         = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const avatarSrc = user?.profilePhotoPath
    ? `${API_BASE_URL}${user.profilePhotoPath}`
    : null;
  const initials = user?.username?.slice(0, 2).toUpperCase() || '??';

  return (
    <nav className="navbar" role="navigation" aria-label="Main navigation">
      <NavLink to="/" className="navbar-brand">
        <span className="brand-icon" aria-hidden="true">C</span>
        ConnectHub
      </NavLink>

      <div className="navbar-links">
        <NavLink to="/feed"    className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Home
        </NavLink>
        <NavLink to="/profile" end className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Profile
        </NavLink>
        <NavLink to="/friends" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Friends
        </NavLink>
      </div>

      <div className="navbar-user">
        <div className="navbar-avatar" aria-hidden="true">
          {avatarSrc
            ? <img src={avatarSrc} alt={user?.username} />
            : <span>{initials}</span>}
        </div>
        <span className="navbar-username">{user?.username}</span>
        <button className="logout-btn" onClick={handleLogout} aria-label="Log out">
          Log out
        </button>
      </div>
    </nav>
  );
}
