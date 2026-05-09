import { useState, useRef } from 'react';
import { useAuth } from '../../context/AuthContext';

/**
 * Compose area on the newsfeed.
 * Allows switching between Post and Story mode.
 * Calls onSubmitPost or onSubmitStory from the parent.
 */
export default function CreatePostForm({ onSubmitPost, onSubmitStory }) {
  const { user } = useAuth();

  const [mode,      setMode]      = useState('post');   // 'post' | 'story'
  const [text,      setText]      = useState('');
  const [image,     setImage]     = useState(null);
  const [preview,   setPreview]   = useState(null);
  const [loading,   setLoading]   = useState(false);
  const [error,     setError]     = useState('');
  const fileRef = useRef(null);

  const avatarSrc = user?.profilePhotoPath
    ? `http://localhost:8080${user.profilePhotoPath}`
    : null;
  const initials = user?.username?.slice(0, 2).toUpperCase() || '??';

  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    if (file.size > 5 * 1024 * 1024) { setError('Image must be under 5 MB'); return; }
    if (!file.type.startsWith('image/')) { setError('Only image files allowed'); return; }
    setError('');
    setImage(file);
    setPreview(URL.createObjectURL(file));
  };

  const removeImage = () => {
    setImage(null);
    setPreview(null);
    if (fileRef.current) fileRef.current.value = '';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!text.trim() && !image) { setError('Add some text or an image'); return; }
    setError('');
    try {
      setLoading(true);
      if (mode === 'post') await onSubmitPost(text.trim(), image);
      else                  await onSubmitStory(text.trim(), image);
      // Reset form
      setText('');
      removeImage();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to post. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="create-post-card">
      {/* Mode toggle */}
      <div className="create-post-tabs" role="tablist">
        {['post', 'story'].map(m => (
          <button
            key={m}
            role="tab"
            aria-selected={mode === m}
            className={`create-post-tab ${mode === m ? 'active' : ''}`}
            onClick={() => setMode(m)}
          >
            {m === 'post' ? '📝 Post' : '⚡ Story'}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="create-post-form">
        <div className="create-post-body">
          {/* Avatar */}
          <div className="create-post-avatar">
            {avatarSrc
              ? <img src={avatarSrc} alt={user?.username} />
              : <span>{initials}</span>}
          </div>

          {/* Text input */}
          <textarea
            className="create-post-input"
            placeholder={mode === 'post'
              ? "What's on your mind?"
              : 'Share a moment — stories disappear after 24 hours'}
            value={text}
            onChange={e => setText(e.target.value)}
            rows={3}
            maxLength={2000}
            aria-label={mode === 'post' ? 'Post content' : 'Story content'}
          />
        </div>

        {/* Image preview */}
        {preview && (
          <div className="create-post-preview">
            <img src={preview} alt="Preview" />
            <button
              type="button"
              className="remove-image-btn"
              onClick={removeImage}
              aria-label="Remove image"
            >
              ✕
            </button>
          </div>
        )}

        {error && <p className="form-error" role="alert">{error}</p>}

        {/* Actions */}
        <div className="create-post-actions">
          <button
            type="button"
            className="attach-btn"
            onClick={() => fileRef.current.click()}
            aria-label="Attach image"
          >
            📷 Photo
          </button>
          <input
            ref={fileRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleImageChange}
          />
          <button
            type="submit"
            className="btn-primary submit-post-btn"
            disabled={loading}
          >
            {loading ? 'Posting…' : mode === 'post' ? 'Post' : 'Share Story'}
          </button>
        </div>
      </form>
    </div>
  );
}