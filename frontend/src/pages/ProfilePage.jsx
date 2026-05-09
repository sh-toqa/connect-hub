import { useState } from 'react';
import { useProfile }       from '../hooks/useProfile';
import { useAuth }          from '../context/AuthContext';
import CoverPhoto           from '../components/profile/CoverPhoto';
import ProfileAvatar        from '../components/profile/ProfileAvatar';
import EditProfileModal     from '../components/profile/EditProfileModal';
import PostCard             from '../components/profile/PostCard';
import FriendsList          from '../components/profile/FriendsList';
import StoryStrip           from '../components/content/StoryStrip';
import ContentModal         from '../components/content/ContentModal';
import '../components/profile/profile.css';
import '../components/content/content.css';


export default function ProfilePage() {
  const { user } = useAuth();
  const {
    profile, posts, stories, friends,
    loading, error, hasMore,
    loadMorePosts, saveBio,
    saveProfilePhoto, saveCoverPhoto, changePassword,
    removePost
  } = useProfile();
  const [showModal,       setShowModal]       = useState(false);
  const [selectedContent, setSelectedContent] = useState(null);
  console.log("stories:", stories);

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

      {/* Cover photo */}
      <CoverPhoto src={profile?.coverPhotoPath} onUpload={saveCoverPhoto} />

      {/* Profile header */}
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

      {/* Stories strip — only shown when user has active stories */}
      {stories.length > 0 && (
        <div style={{ padding: '0 16px', marginTop: 12 }}>
          <h3 style={{ fontSize: 15, fontWeight: 700, marginBottom: 8 }}>Your Stories</h3>
          <StoryStrip
            stories={stories}
            onStoryClick={setSelectedContent}
          />
        </div>
      )}

      {/* Body: Posts + Friends */}
      <div className="profile-body">

        <section className="profile-posts" aria-label="Your posts">
          <h2>Posts</h2>
          {posts.length === 0 && (
            <p className="empty-state">You haven't posted anything yet.</p>
          )}
          {posts.map(post => (
            <PostCard
              key={post.contentId}
              post={post}
              onClick={setSelectedContent}
              onDelete={removePost}
            />
          ))}
          {hasMore && (
            <button className="btn-secondary load-more-btn" onClick={loadMorePosts}>
              Load more
            </button>
          )}
        </section>

        <aside className="profile-friends" aria-label="Friends">
          <h2>Friends ({friends.length})</h2>
          <FriendsList friends={friends} />
        </aside>
      </div>

      {/* Edit profile modal */}
      {showModal && (
        <EditProfileModal
          profile={profile}
          onSaveBio={saveBio}
          onChangePassword={changePassword}
          onClose={() => setShowModal(false)}
        />
      )}

      {/* Content viewer modal */}
      {selectedContent && (
        <ContentModal
          content={selectedContent}
          onClose={() => setSelectedContent(null)}
        />
      )}

    </main>
  );
}