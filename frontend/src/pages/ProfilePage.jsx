import { useState } from 'react';
import { useProfile }       from '../hooks/useProfile';
import CoverPhoto           from '../components/profile/CoverPhoto';
import ProfileAvatar        from '../components/profile/ProfileAvatar';
import EditProfileModal     from '../components/profile/EditProfileModal';
import PostCard             from '../components/profile/PostCard';
import FriendsList          from '../components/profile/FriendsList';
import { useAuth }          from '../context/AuthContext';

export default function ProfilePage() {
  const { user }  = useAuth();
  const {
    profile, posts, friends,
    loading, error, hasMore,
    loadMorePosts, saveBio,
    saveProfilePhoto, saveCoverPhoto, changePassword,
  } = useProfile();

  const [showModal, setShowModal] = useState(false);

  if (loading) {
    return (
      <div className="profile-loading" role="status" aria-live="polite">
        <div className="spinner" aria-label="Loading profile" />
        <p>Loading profile…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="profile-error" role="alert">
        <p>⚠ {error}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return (
    <main className="profile-page">

      <CoverPhoto
        src={profile?.coverPhotoPath}
        onUpload={saveCoverPhoto}
      />

      <section className="profile-header">
        <ProfileAvatar
          src={profile?.profilePhotoPath}
          username={profile?.username}
          onUpload={saveProfilePhoto}
        />

        <div className="profile-info">
          <h1 className="profile-username">{profile?.username}</h1>
          <p className="profile-email">{profile?.email}</p>
          {profile?.bio
            ? <p className="profile-bio">{profile.bio}</p>
            : <p className="profile-bio empty">Add a bio to tell people about yourself.</p>}
        </div>

        <button
          className="btn-primary edit-profile-btn"
          onClick={() => setShowModal(true)}
          aria-label="Edit profile"
        >
          ✏ Edit Profile
        </button>
      </section>

      <div className="profile-body">

        {/* Posts column */}
        <section className="profile-posts" aria-label="Your posts">
          <h2>Posts</h2>
          {posts.length === 0 && (
            <p className="empty-state">You haven't posted anything yet.</p>
          )}
          {posts.map(post => <PostCard key={post.contentId} post={post} />)}

          {hasMore && (
            <button
              className="btn-secondary load-more-btn"
              onClick={loadMorePosts}
            >
              Load more
            </button>
          )}
        </section>

        <aside className="profile-friends" aria-label="Friends">
          <h2>Friends ({friends.length})</h2>
          <FriendsList friends={friends} />
        </aside>
      </div>

      {showModal && (
        <EditProfileModal
          profile={profile}
          onSaveBio={saveBio}
          onChangePassword={changePassword}
          onClose={() => setShowModal(false)}
        />
      )}
    </main>
  );
}