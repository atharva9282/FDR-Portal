import api from './client'

export const monitoringApi = {
  dashboard: (withinDays) => api.get('/monitoring/dashboard', { params: { withinDays } }),
  maturities: (withinDays) => api.get('/monitoring/maturities', { params: { withinDays } }),
  notifications: (unreadOnly = false) => api.get('/monitoring/notifications', { params: { unreadOnly } }),
  markNotificationRead: (id) => api.patch(`/monitoring/notifications/${id}/read`),
}
