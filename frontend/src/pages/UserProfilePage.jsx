import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getUserProfile }  from '../api/profileApi';
import { getUserPosts }    from '../api/profileApi';
import { getUserStories }  from '../api/profileApi';
import PostCard            from '../components/profile/PostCard';
import StoryStrip           from '../components/content/StoryStrip';
import ContentModal        from '../components/content/ContentModal';
import '../components/profile/profile.css';
import '../components/content/content.css';

export default function UserProfilePage() {
  const { userId }   = useParams();
  const navigate     = useNavigate();

  const [profile,  setProfile]  = useState(null);
  const [posts,    setPosts]    = useState([]);
  const [stories,  setStories]  = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);
  const [selectedContent, setSelectedContent] = useState(null);

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const [profileRes, postsRes, storiesRes] = await Promise.all([
          getUserProfile(userId),
          getUserPosts(userId).catch(() => ({ data: { content: [] } })),
          getUserStories(userId).catch(() => ({ data: { content: [] } }))
        ]);

        setProfile(profileRes.data);
        setPosts(postsRes.data.content ?? []);
        setStories(storiesRes.data.content ?? []);
      } catch (err) {
        setError(err.response?.data?.message || 'User not found');
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [userId]);

  if (loading) {
    return (
      <div className="profile-loading" role="status">
        <div className="spinner" />
        <p>Loading profile…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="profile-error" role="alert">
        <p>⚠ {error}</p>
        <button className="btn-secondary" onClick={() => navigate(-1)}>Go back</button>
      </div>
    );
  }

  const avatarSrc = profile?.profilePhotoPath
    ? `http://localhost:8080${profile.profilePhotoPath}`
    : null;
  const coverSrc  = profile?.coverPhotoPath
    ? `http://localhost:8080${profile.coverPhotoPath}`
    : null;
  const initials  = profile?.username?.slice(0, 2).toUpperCase() || '??';
  const isOnline  = profile?.status === 'ONLINE';

  return (
    <main className="profile-page">

      {/* Cover photo */}
      <div className="cover-photo-container">
        {coverSrc
          ? <img src={coverSrc} alt="Cover" className="cover-photo-img" />
          : <div className="cover-photo-placeholder" />}
      </div>

      {/* Profile header */}
      <section className="profile-header">

        <div className="profile-avatar">
          <div className="avatar-circle">
            {avatarSrc
              ? <img src={avatarSrc} alt={profile?.username} className="avatar-img" />
              : <span className="avatar-initials">{initials}</span>}
          </div>

          <span
            className={`status-dot ${isOnline ? 'online' : 'offline'}`}
            title={isOnline ? 'Online' : 'Offline'}
          />
        </div>

        <div className="profile-info">
          <h1 className="profile-username">{profile?.username}</h1>

          <p className="profile-email">
            {profile?.email}
          </p>

          {profile?.bio
            ? <p className="profile-bio">{profile.bio}</p>
            : <p className="profile-bio empty">No bio yet.</p>}
        </div>

        <button
          className="btn-primary edit-profile-btn"
          onClick={() => navigate(-1)}
        >
          ← Back
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

      {/* Posts + Friends layout */}
      <div className="profile-body">

        <section
          className="profile-posts"
          aria-label={`${profile?.username}'s posts`}
        >
          <h2>Posts</h2>

          {posts.length === 0 ? (
            <p className="empty-state">No posts yet.</p>
          ) : (
            posts.map(post => (
              <PostCard
                key={post.contentId}
                post={post}
                onClick={setSelectedContent}
              />
            ))
          )}
        </section>

      </div>

      {setSelectedContent && (
        <ContentModal
          content={selectedContent}
          onClose={() => setSelectedContent(null)}
        />
      )}

    </main>
  );
}