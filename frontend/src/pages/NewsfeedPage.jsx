import { useState } from 'react';
import { useContent }    from '../hooks/useContent';
import CreatePostForm    from '../components/content/CreatePostForm';
import StoryStrip        from '../components/content/StoryStrip';
import ContentModal      from '../components/content/ContentModal';
import PostCard          from '../components/profile/PostCard';
import '../components/content/content.css';

export default function NewsfeedPage() {
  const {
    posts, stories, loading, error, hasMore,
    loadMorePosts, submitPost, submitStory, removeContent, reload,
  } = useContent();

  const [selectedContent, setSelectedContent] = useState(null);

  return (
    <div className="newsfeed-page">

      <CreatePostForm
        onSubmitPost={submitPost}
        onSubmitStory={submitStory}
      />

      <StoryStrip
        stories={stories}
        onStoryClick={setSelectedContent}
      />

      <div className="feed-header">
        <h2>Feed</h2>
        <button className="refresh-btn" onClick={reload} aria-label="Refresh feed">
          🔄 Refresh
        </button>
      </div>

      {loading && (
        <div className="feed-loading" role="status">
          <div className="spinner" />
          <p>Loading feed…</p>
        </div>
      )}

      {error && !loading && (
        <div className="feed-error" role="alert">
          <p>⚠ {error}</p>
          <button className="btn-secondary" onClick={reload}>Retry</button>
        </div>
      )}

      {!loading && !error && (
        <>
          {posts.length === 0
            ? <div className="feed-empty"><p>Nothing here yet — create a post to get started!</p></div>
            : posts.map(post => (
                <PostCard
                  key={post.contentId}
                  post={post}
                  onDelete={removeContent}
                  onClick={setSelectedContent}
                />
              ))
          }
          {hasMore && (
            <button className="btn-secondary load-more-feed-btn" onClick={loadMorePosts}>
              Load more
            </button>
          )}
        </>
      )}

      {/* Modal viewer for posts and stories */}
      {selectedContent && (
        <ContentModal
          content={selectedContent}
          onClose={() => setSelectedContent(null)}
          onDelete={removeContent}
        />
      )}

    </div>
  );
}