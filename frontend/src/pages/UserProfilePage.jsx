import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';

import { getUserProfile, getUserPosts } from '../api/profileApi';
import { getFriendStatus, sendRequest } from '../api/friendApi';

import CoverPhoto from '../components/profile/CoverPhoto';
import ProfileAvatar from '../components/profile/ProfileAvatar';
import PostCard from '../components/profile/PostCard';
import ContentModal from '../components/content/ContentModal';

import '../components/profile/profile.css';
import '../components/content/content.css';

export default function UserProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();

  const [profile, setProfile] = useState(null);
  const [posts, setPosts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [selectedContent, setSelectedContent] = useState(null);
  const [friendStatus, setFriendStatus] = useState('NONE');

  useEffect(() => {
    const load = async () => {
      try {
        setLoading(true);
        setError(null);

        const [profileRes, postsRes] = await Promise.all([
          getUserProfile(userId),
          getUserPosts(userId).catch(() => ({
            data: { content: [] }
          }))
        ]);

        setProfile(profileRes.data);
        setPosts(postsRes.data.content ?? []);

        const statusRes = await getFriendStatus(userId).catch(() => ({
          data: { status: 'NONE' }
        }));

        setFriendStatus(statusRes.data.status);

      } catch (err) {
        setError(
          err.response?.data?.message || 'User not found'
        );
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

        <button
          className="btn-secondary"
          onClick={() => navigate(-1)}
        >
          Go Back
        </button>
      </div>
    );
  }

  const isOnline = profile?.status === 'ONLINE';

  return (
    <main className="profile-page">

      {/* Cover photo - read-only, this is not the logged-in user's profile */}
      <CoverPhoto
        src={profile?.coverPhotoPath}
        editable={false}
      />

      {/* Profile header */}
      <section className="profile-header">

        <ProfileAvatar
          src={profile?.profilePhotoPath}
          username={profile?.username}
          editable={false}
        />

        <div className="profile-info">

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 8
            }}
          >
            <h1 className="profile-username">
              {profile?.username}
            </h1>

            <span
              className={`status-dot ${
                isOnline ? 'online' : 'offline'
              }`}
              title={isOnline ? 'Online' : 'Offline'}
              style={{
                position: 'static',
                width: 10,
                height: 10,
                flexShrink: 0
              }}
            />
          </div>

          <p className="profile-email">
            {profile?.email}
          </p>

          {profile?.bio ? (
            <p className="profile-bio">
              {profile.bio}
            </p>
          ) : (
            <p className="profile-bio empty">
              No bio yet.
            </p>
          )}
        </div>

        <div
          style={{
            display: 'flex',
            gap: 8,
            marginLeft: 'auto'
          }}
        >

          {friendStatus === 'NONE' && (
            <button
              className="btn-primary"
              onClick={async () => {
                await sendRequest(userId);
                setFriendStatus('PENDING');
              }}
            >
              + Add Friend
            </button>
          )}

          {friendStatus === 'PENDING' && (
            <button
              className="btn-secondary"
              disabled
            >
              ⏳ Request Sent
            </button>
          )}

          {friendStatus === 'ACCEPTED' && (
            <button
              className="btn-secondary"
              disabled
            >
              ✓ Friends
            </button>
          )}

          {friendStatus === 'BLOCKED' && (
            <button
              className="btn-secondary"
              disabled
            >
              🚫 Blocked
            </button>
          )}

          <button
            className="btn-secondary"
            onClick={() => navigate(-1)}
          >
            ← Back
          </button>

        </div>
      </section>

      {/* Posts */}
      <div className="profile-body">

        <section
          className="profile-posts"
          aria-label={`${profile?.username}'s posts`}
        >
          <h2>Posts</h2>

          {posts.length === 0 ? (
            <p className="empty-state">
              No posts yet.
            </p>
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

      {/* Content modal */}
      {selectedContent && (
        <ContentModal
          content={selectedContent}
          onClose={() => setSelectedContent(null)}
        />
      )}

    </main>
  );
}