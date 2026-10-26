import api from './client'

export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials),
  me: () => api.get('/auth/me'),
  createUser: (user) => api.post('/auth/users', user),
}
