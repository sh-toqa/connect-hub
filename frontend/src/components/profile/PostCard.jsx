import { useAuth } from '../../context/AuthContext';
import { API_BASE_URL } from '../../config/api';

export default function PostCard({ post, onDelete, onClick }) {
  const { user } = useAuth();
  const { author, contentText, imagePath, timestamp, contentId } = post;

  const avatarSrc    = author?.profilePhotoPath
    ? `${API_BASE_URL}${author.profilePhotoPath}`
    : null;
  const initials     = author?.username?.slice(0, 2).toUpperCase() || '??';
  const formattedDate = new Date(timestamp).toLocaleString();
  const isOwner      = user?.userId === author?.userId;

  return (
    <article
      className="post-card"
      onClick={onClick ? () => onClick(post) : undefined}
      style={{ cursor: onClick ? 'pointer' : 'default' }}
    >
      <header className="post-header">
        <div className="post-avatar">
          {avatarSrc
            ? <img src={avatarSrc} alt={author?.username} />
            : <span>{initials}</span>}
        </div>
        <div className="post-meta">
          <span className="post-username">{author?.username}</span>
          <time className="post-time" dateTime={timestamp}>{formattedDate}</time>
        </div>
        {isOwner && onDelete && (
          <button
            className="post-delete-btn"
            onClick={(e) => { e.stopPropagation(); onDelete(contentId); }}
            aria-label="Delete post"
          >
            🗑
          </button>
        )}
      </header>

      {contentText && <p className="post-text">{contentText}</p>}

      {imagePath && (
        <img
          src={`${API_BASE_URL}${imagePath}`}
          alt="Post attachment"
          className="post-image"
        />
      )}
    </article>
  );
}