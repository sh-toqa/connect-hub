// src/components/profile/PostCard.jsx
// Renders one post entry — used by the posts grid on the profile page.

function timeAgo(isoString) {
  const diff = Date.now() - new Date(isoString).getTime();
  const mins  = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days  = Math.floor(diff / 86400000);
  if (mins  < 1)  return 'just now';
  if (mins  < 60) return `${mins}m ago`;
  if (hours < 24) return `${hours}h ago`;
  return `${days}d ago`;
}

export default function PostCard({ post }) {
  return (
    <article className="post-card">
      {post.imagePath && (
        <div className="post-card__image-wrap">
          <img src={post.imagePath} alt="Post attachment" className="post-card__image" loading="lazy" />
        </div>
      )}
      <div className="post-card__body">
        <p className="post-card__text">{post.contentText}</p>
        <time className="post-card__time" dateTime={post.timestamp}>
          {timeAgo(post.timestamp)}
        </time>
      </div>
    </article>
  );
}