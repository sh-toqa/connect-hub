import { useEffect } from 'react';
import { Link }      from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { API_BASE_URL } from '../../config/api';

// Modal to view a post or story in detail, with delete option for owner.
export default function ContentModal({ content, onClose, onDelete }) {
  const { user } = useAuth();

  // Close on Escape key
  useEffect(() => {
    const handle = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handle);
    return () => window.removeEventListener('keydown', handle);
  }, [onClose]);

  if (!content) return null;

  const { contentId, contentText, imagePath, contentType, timestamp, author } = content;

  const avatarSrc = author?.profilePhotoPath
    ? `${API_BASE_URL}${author.profilePhotoPath}`
    : null;
  const initials    = author?.username?.slice(0, 2).toUpperCase() || '??';
  const isOwner      = user?.userId === author?.userId;
  const isStory      = contentType === 'STORY';
  const profileLink  = isOwner ? '/profile' : `/profile/${author?.userId}/posts`;
  const formattedDate = new Date(timestamp).toLocaleString();

  const handleDelete = async () => {
    await onDelete(contentId);
    onClose();
  };

  return (
    <div
      className="content-modal-overlay"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-label={isStory ? 'Story viewer' : 'Post viewer'}
    >
      <div
        className={`content-modal-box ${isStory ? 'story-modal' : 'post-modal'}`}
        onClick={e => e.stopPropagation()}
      >
        {/* Header */}
        <div className="content-modal-header">
          <div className="content-modal-author">
            <div className="content-modal-avatar">
              {avatarSrc
                ? <img src={avatarSrc} alt={author?.username} />
                : <span>{initials}</span>}
            </div>
            <div>
              <Link to={profileLink} className="content-modal-username-link" onClick={onClose}>{author?.username}</Link>
              <time className="content-modal-time">{formattedDate}</time>
              {isStory && (
                <span className="content-modal-badge">⚡ Story</span>
              )}
            </div>
          </div>

          <div className="content-modal-actions">
            {isOwner && onDelete && (
              <button
                className="modal-delete-btn"
                onClick={handleDelete}
                aria-label="Delete"
              >
                🗑 Delete
              </button>
            )}
            <button
              className="modal-close-btn"
              onClick={onClose}
              aria-label="Close"
            >
              ✕
            </button>
          </div>
        </div>

        {/* Image */}
        {imagePath && (
          <div className="content-modal-image">
            <img
              src={`${API_BASE_URL}${imagePath}`}
              alt="Content"
            />
          </div>
        )}

        {/* Text */}
        {contentText && (
          <p className="content-modal-text">{contentText}</p>
        )}
      </div>
    </div>
  );
}
