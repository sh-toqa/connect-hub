import api from './authApi';

export const createPost = (contentText, image = null) => {
  const form = new FormData();

  if (contentText) {
    form.append('contentText', contentText);
  }

  if (image) {
    form.append('image', image);
  }

  return api.post('/content/posts', form, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const createStory = (contentText, image = null) => {
  const form = new FormData();
  if (contentText) {
    form.append('contentText', contentText);
  }
  if (image) {
    form.append('image', image);
  }
  return api.post('/content/stories', form, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const deleteContent  = (contentId)           => api.delete(`/content/${contentId}`);
export const getFeedPosts   = (page = 0, size = 10) => api.get('/content/feed/posts', { params: { page, size } });
export const getFeedStories = ()                    => api.get('/content/feed/stories');