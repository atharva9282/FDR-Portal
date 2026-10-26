import api from './client'

export const workflowApi = {
  createRenewal: (fdrId, request) => api.post(`/fdrs/${fdrId}/renewals`, request),
  createRelease: (fdrId, request) => api.post(`/fdrs/${fdrId}/release-requests`, request),
  listRenewals: (fdrId) => api.get(`/fdrs/${fdrId}/renewals`),
  listReleases: (fdrId) => api.get(`/fdrs/${fdrId}/release-requests`),
  review: (kind, requestId, request) => api.patch(`/${kind}/${requestId}/review`, request),
}
