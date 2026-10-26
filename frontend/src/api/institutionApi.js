import api from './client'

export const institutionApi = {
  list: (params) => api.get('/institutions', { params }),
  get: (id) => api.get(`/institutions/${id}`),
  create: (data) => api.post('/institutions', data),
}
