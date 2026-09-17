import { useRef, useState } from 'react';
import { API_BASE_URL } from '../../config/api';

/**
 * Circular profile photo with hover-to-upload overlay.
 * Handles local preview, size/type validation, and upload state.
 */
export default function ProfileAvatar({ src, username, onUpload, editable = true }) {
  const inputRef = useRef(null);
  const [preview,   setPreview]   = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error,     setError]     = useState('');

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    if (file.size > 5 * 1024 * 1024) { setError('Max 5 MB'); return; }
    if (!file.type.startsWith('image/')) { setError('Images only'); return; }

    setError('');
    setPreview(URL.createObjectURL(file));

    try {
      setUploading(true);
      await onUpload(file);
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed');
      setPreview(null);
    } finally {
      setUploading(false);
    }
  };

  const displaySrc = preview || (src ? `${API_BASE_URL}${src}` : null);
  const initials   = username ? username.slice(0, 2).toUpperCase() : '??';

  return (
    <div className="avatar-wrapper">
      <div
        className="avatar-circle"
        onClick={() => editable && inputRef.current.click()}
        role={editable ? 'button' : undefined}
        aria-label={editable ? 'Change profile photo' : undefined}
        tabIndex={editable ? 0 : undefined}
        onKeyDown={(e) => e.key === 'Enter' && editable && inputRef.current.click()}
      >
        {displaySrc
          ? <img src={displaySrc} alt={`${username}'s avatar`} className="avatar-img" />
          : <span className="avatar-initials">{initials}</span>}

        {editable && (
          <div className="avatar-overlay">
            {uploading ? '…' : '📷'}
          </div>
        )}
      </div>

      {editable && (
        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          style={{ display: 'none' }}
          onChange={handleFileChange}
        />
      )}

      {error && <p className="upload-error" role="alert">{error}</p>}
    </div>
  );
}
