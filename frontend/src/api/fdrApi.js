import api from './client'

export const fdrApi = {
  list: (params) => api.get('/fdrs', { params }),
  get: (id) => api.get(`/fdrs/${id}`),
  create: (data) => api.post('/fdrs', data),
  documents: (id) => api.get(`/fdrs/${id}/documents`),
  reviews: (id) => api.get(`/fdrs/${id}/reviews`),
  renewals: (id) => api.get(`/fdrs/${id}/renewals`),
  releases: (id) => api.get(`/fdrs/${id}/release-requests`),
}
