import api from './client'

export const verificationApi = {
  submitReview: (fdrId, review) => api.post(`/fdrs/${fdrId}/reviews`, review),
}
