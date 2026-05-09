import api from './authApi';

export const getMyProfile      = ()              => api.get('/profile');
export const getUserProfile    = (userId)        => api.get(`/profile/${userId}`);
export const updateProfile     = (data)          => api.patch('/profile', data);
export const updatePassword    = (data)          => api.patch('/profile/password', data);
export const getMyPosts        = (page=0, size=10) => api.get('/profile/posts', { params: { page, size } });
export const getMyFriends      = ()              => api.get('/profile/friends');
export const getMyStories      = ()              => api.get('/profile/stories');

export const uploadProfilePhoto = (file) => {
  const form = new FormData();
  form.append('file', file);
  return api.post('/profile/photo', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const uploadCoverPhoto = (file) => {
  const form = new FormData();
  form.append('file', file);
  return api.post('/profile/cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
};

export const getUserPosts = (userId, page = 0, size = 20) =>
  api.get(`/profile/${userId}/posts`, { params: { page, size } });

export const getUserStories = (userId, page = 0, size = 20) =>
  api.get(`/profile/${userId}/stories`, { params: { page, size } });