import { useRef, useState } from 'react';

/**
 * Displays the cover photo with an overlay upload button.
 * On file select: shows a local preview immediately, then calls onUpload.
 */
export default function CoverPhoto({ src, onUpload, editable = true }) {
  const inputRef = useRef(null);
  const [preview, setPreview] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    // Validate on the client before sending
    if (file.size > 5 * 1024 * 1024) {
      setError('File must be under 5 MB');
      return;
    }
    if (!file.type.startsWith('image/')) {
      setError('Only image files are allowed');
      return;
    }

    setError('');
    setPreview(URL.createObjectURL(file));   // instant local preview

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

  const displaySrc = preview || (src ? `http://localhost:8080${src}` : null);

  return (
    <div className="cover-photo-container">
      {displaySrc
        ? <img src={displaySrc} alt="Cover" className="cover-photo-img" />
        : <div className="cover-photo-placeholder" />}

      {editable && (
        <>
          <button
            className="cover-upload-btn"
            onClick={() => inputRef.current.click()}
            disabled={uploading}
            aria-label="Change cover photo"
          >
            {uploading ? 'Uploading…' : '📷 Change Cover'}
          </button>
          <input
            ref={inputRef}
            type="file"
            accept="image/*"
            style={{ display: 'none' }}
            onChange={handleFileChange}
          />
        </>
      )}

      {error && <p className="upload-error">{error}</p>}
    </div>
  );
}
