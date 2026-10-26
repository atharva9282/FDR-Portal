import api from './client'

export const documentApi = {
  upload: (fdrId, form) => api.post(`/fdrs/${fdrId}/documents`, form, { headers: { 'Content-Type': 'multipart/form-data' } }),
  download: (fdrId, documentId) => api.get(`/fdrs/${fdrId}/documents/${documentId}/download`, { responseType: 'blob' }),
}
