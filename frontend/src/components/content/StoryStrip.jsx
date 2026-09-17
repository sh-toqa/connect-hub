import { API_BASE_URL } from '../../config/api';
export default function StoryStrip({ stories, onStoryClick }) {
  if (!stories || stories.length === 0) return null;

  const byAuthor = Object.values(
    stories.reduce((acc, story) => {
      const uid = story.author.userId;
      if (!acc[uid]) acc[uid] = story;
      return acc;
    }, {})
  );

  return (
    <div className="story-strip" aria-label="Stories">
      <div className="story-strip-scroll">
        {byAuthor.map(story => {
          const avatarSrc = story.author.profilePhotoPath
            ? `${API_BASE_URL}${story.author.profilePhotoPath}`
            : null;
          const initials = story.author.username?.slice(0, 2).toUpperCase() || '??';

          return (
            <button
              key={story.author.userId}
              className="story-bubble"
              onClick={() => onStoryClick(story)}
              aria-label={`View ${story.author.username}'s story`}
            >
              <div className="story-avatar-ring">
                {avatarSrc
                  ? <img src={avatarSrc} alt={story.author.username} />
                  : <span>{initials}</span>}
              </div>
              <span className="story-author">{story.author.username}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}