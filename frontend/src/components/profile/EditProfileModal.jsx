// src/components/profile/EditProfileModal.jsx
// FR-PM-09: edit modal with bio, profile photo, cover photo, password change
// FR-PM-10: local image preview before upload

import { useState, useRef } from 'react';

const TABS = ['Profile', 'Cover Photo', 'Password'];

export default function EditProfileModal({ profile, onSaveBio, onSaveProfilePhoto,
  onSaveCoverPhoto, onChangePassword, onClose }) {

  const [activeTab,    setActiveTab]    = useState('Profile');
  const [bio,          setBio]          = useState(profile?.bio || '');
  const [bioError,     setBioError]     = useState('');
  const [photoPreview, setPhotoPreview] = useState(null);
  const [coverPreview, setCoverPreview] = useState(null);
  const [photoFile,    setPhotoFile]    = useState(null);
  const [coverFile,    setCoverFile]    = useState(null);
  const [pwForm,       setPwForm]       = useState({ current: '', next: '', confirm: '' });
  const [pwErrors,     setPwErrors]     = useState({});
  const [saving,       setSaving]       = useState(false);
  const [successMsg,   setSuccessMsg]   = useState('');
  const [serverError,  setServerError]  = useState('');

  const profileInputRef = useRef();
  const coverInputRef   = useRef();

  // ── Shared reset ─────────────────────────────────────────────────────────
  const clearMessages = () => { setSuccessMsg(''); setServerError(''); };

  // ── Tab: Profile ─────────────────────────────────────────────────────────
  const handlePhotoChange = (e, type) => {
    const file = e.target.files[0];
    if (!file) return;
    const preview = URL.createObjectURL(file);
    if (type === 'profile') { setPhotoPreview(preview); setPhotoFile(file); }
    else                    { setCoverPreview(preview);  setCoverFile(file); }
    clearMessages();
  };

  const handleSaveProfile = async () => {
    if (bio.length > 300) { setBioError('Bio cannot exceed 300 characters'); return; }
    setBioError('');
    setSaving(true);
    clearMessages();
    try {
      if (bio !== profile.bio) await onSaveBio(bio);
      if (photoFile)           await onSaveProfilePhoto(photoFile);
      setSuccessMsg('Profile updated!');
      setPhotoFile(null);
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Failed to save. Please try again.');
    } finally { setSaving(false); }
  };

  // ── Tab: Cover Photo ─────────────────────────────────────────────────────
  const handleSaveCover = async () => {
    if (!coverFile) return;
    setSaving(true);
    clearMessages();
    try {
      await onSaveCoverPhoto(coverFile);
      setSuccessMsg('Cover photo updated!');
      setCoverFile(null);
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Upload failed.');
    } finally { setSaving(false); }
  };

  // ── Tab: Password ─────────────────────────────────────────────────────────
  const validatePassword = () => {
    const errs = {};
    if (!pwForm.current)         errs.current = 'Current password is required';
    if (!pwForm.next)            errs.next    = 'New password is required';
    else if (pwForm.next.length < 8) errs.next = 'Must be at least 8 characters';
    if (pwForm.next !== pwForm.confirm) errs.confirm = 'Passwords do not match';
    return errs;
  };

  const handleSavePassword = async () => {
    const errs = validatePassword();
    if (Object.keys(errs).length) { setPwErrors(errs); return; }
    setPwErrors({});
    setSaving(true);
    clearMessages();
    try {
      await onChangePassword(pwForm.current, pwForm.next);
      setSuccessMsg('Password changed successfully!');
      setPwForm({ current: '', next: '', confirm: '' });
    } catch (err) {
      setServerError(err?.response?.data?.message || 'Incorrect current password.');
    } finally { setSaving(false); }
  };

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-label="Edit profile">
      <div className="modal">
        {/* Header */}
        <div className="modal__header">
          <h2 className="modal__title">Edit Profile</h2>
          <button className="modal__close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {/* Tabs */}
        <div className="modal__tabs" role="tablist">
          {TABS.map(tab => (
            <button key={tab}
              role="tab"
              aria-selected={activeTab === tab}
              className={`modal__tab ${activeTab === tab ? 'modal__tab--active' : ''}`}
              onClick={() => { setActiveTab(tab); clearMessages(); }}>
              {tab}
            </button>
          ))}
        </div>

        {/* Messages */}
        {successMsg  && <div className="alert alert--success" role="status">{successMsg}</div>}
        {serverError && <div className="alert alert--error"   role="alert">{serverError}</div>}

        {/* ── Tab: Profile ───────────────────────────────────────────────── */}
        {activeTab === 'Profile' && (
          <div className="modal__body">
            {/* Profile photo upload with preview — FR-PM-10 */}
            <div className="upload-row">
              <div className="upload-preview upload-preview--round">
                <img
                  src={photoPreview || profile?.profilePhotoPath || '/default-avatar.png'}
                  alt="Profile photo preview"
                />
                <button
                  className="upload-overlay-btn"
                  onClick={() => profileInputRef.current?.click()}
                  aria-label="Change profile photo">
                  📷
                </button>
              </div>
              <div className="upload-info">
                <strong>Profile Photo</strong>
                <p>JPEG, PNG or WebP · Max 5 MB</p>
                <button className="btn btn--ghost btn--sm"
                  onClick={() => profileInputRef.current?.click()}>
                  Choose file
                </button>
                {photoFile && <span className="upload-filename">✓ {photoFile.name}</span>}
              </div>
              <input ref={profileInputRef} type="file" accept="image/*" hidden
                onChange={e => handlePhotoChange(e, 'profile')} />
            </div>

            {/* Bio */}
            <div className={`form-field ${bioError ? 'form-field--error' : ''}`}>
              <label htmlFor="bio" className="form-field__label">
                Bio <span className="char-count">{bio.length}/300</span>
              </label>
              <textarea
                id="bio"
                className="form-field__input form-field__textarea"
                rows={4}
                value={bio}
                onChange={e => { setBio(e.target.value); setBioError(''); }}
                placeholder="Tell people about yourself…"
                maxLength={300}
                aria-invalid={!!bioError}
              />
              {bioError && <span className="form-field__error" role="alert">{bioError}</span>}
            </div>

            <button className="btn btn--primary btn--full" onClick={handleSaveProfile}
              disabled={saving} aria-busy={saving}>
              {saving ? <span className="spinner" aria-label="Saving…" /> : 'Save changes'}
            </button>
          </div>
        )}

        {/* ── Tab: Cover Photo ───────────────────────────────────────────── */}
        {activeTab === 'Cover Photo' && (
          <div className="modal__body">
            {/* Cover preview — FR-PM-10 */}
            <div className="upload-preview upload-preview--cover">
              <img
                src={coverPreview || profile?.coverPhotoPath ||
                  'https://images.unsplash.com/photo-1519681393784-d120267933ba?w=800&q=60'}
                alt="Cover photo preview"
              />
              <button
                className="upload-overlay-btn upload-overlay-btn--cover"
                onClick={() => coverInputRef.current?.click()}
                aria-label="Change cover photo">
                📷 Change cover
              </button>
            </div>
            <input ref={coverInputRef} type="file" accept="image/*" hidden
              onChange={e => handlePhotoChange(e, 'cover')} />

            <p className="upload-hint">Recommended: 1200 × 400 px · JPEG or PNG · Max 5 MB</p>
            {coverFile && <p className="upload-filename">✓ {coverFile.name} selected</p>}

            <button className="btn btn--primary btn--full" onClick={handleSaveCover}
              disabled={saving || !coverFile} aria-busy={saving}>
              {saving ? <span className="spinner" aria-label="Uploading…" /> : 'Upload cover photo'}
            </button>
          </div>
        )}

        {/* ── Tab: Password ──────────────────────────────────────────────── */}
        {activeTab === 'Password' && (
          <div className="modal__body">
            {[
              { id: 'cur-pw',  label: 'Current password', key: 'current', complete: 'current-password' },
              { id: 'new-pw',  label: 'New password',     key: 'next',    complete: 'new-password' },
              { id: 'con-pw',  label: 'Confirm new password', key: 'confirm', complete: 'new-password' },
            ].map(({ id, label, key, complete }) => (
              <div key={id} className={`form-field ${pwErrors[key] ? 'form-field--error' : ''}`}>
                <label htmlFor={id} className="form-field__label">{label}</label>
                <input id={id} type="password" className="form-field__input"
                  value={pwForm[key]}
                  autoComplete={complete}
                  onChange={e => { setPwForm(p => ({ ...p, [key]: e.target.value }));
                                   setPwErrors(p => ({ ...p, [key]: '' })); }}
                  aria-invalid={!!pwErrors[key]}
                  aria-describedby={pwErrors[key] ? `${id}-err` : undefined}
                />
                {pwErrors[key] && (
                  <span id={`${id}-err`} className="form-field__error" role="alert">
                    {pwErrors[key]}
                  </span>
                )}
              </div>
            ))}

            <button className="btn btn--primary btn--full" onClick={handleSavePassword}
              disabled={saving} aria-busy={saving}>
              {saving ? <span className="spinner" aria-label="Saving…" /> : 'Change password'}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}