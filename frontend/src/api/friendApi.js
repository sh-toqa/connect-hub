import api from './authApi';

export const sendRequest      = (receiverId)    => api.post(`/friends/request/${receiverId}`);
export const acceptRequest    = (friendshipId)  => api.post(`/friends/${friendshipId}/accept`);
export const declineRequest   = (friendshipId)  => api.delete(`/friends/${friendshipId}/decline`);
export const removeFriend     = (friendshipId)  => api.delete(`/friends/${friendshipId}`);
export const blockUser        = (targetId)      => api.post(`/friends/block/${targetId}`);
export const unblockUser      = (targetId)      => api.delete(`/friends/block/${targetId}`);
export const getPendingRequests = ()            => api.get('/friends/requests');
export const getFriends       = ()              => api.get('/friends');
export const getSuggestions   = ()              => api.get('/friends/suggestions');
export const getFriendStatus  = (otherUserId)   => api.get(`/friends/status/${otherUserId}`);