export function getApiErrorMessage(error) {
  const status = error?.response?.status
  return error?.response?.data?.message || error?.response?.data?.detail || (
    status === 401 ? 'Your session has expired. Please sign in again.' :
    status === 403 ? 'Your account does not have permission for this action.' :
    status === 404 ? 'The requested record could not be found.' :
    status === 409 ? 'This record conflicts with existing data. Check the details and try again.' :
    error?.message === 'Network Error' ? 'Unable to reach the backend. Check that the Spring Boot server is running at localhost:8080.' :
    'The request could not be completed. Please try again.'
  )
}
