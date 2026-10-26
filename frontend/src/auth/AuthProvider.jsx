import { useContext, useEffect, useState } from 'react'
import AuthContext from './AuthContext'
import { authApi } from '../api/authApi'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [checking, setChecking] = useState(true)

  useEffect(() => {
    const expired = () => setUser(null)
    window.addEventListener('fdr-session-expired', expired)
    if (!localStorage.getItem('fdr_token')) {
      setChecking(false)
    } else {
      authApi.me()
        .then(({ data }) => setUser(data))
        .catch(() => {
          localStorage.removeItem('fdr_token')
          setUser(null)
        })
        .finally(() => setChecking(false))
    }
    return () => window.removeEventListener('fdr-session-expired', expired)
  }, [])

  const signIn = async (email, password) => {
    const { data } = await authApi.login({ email, password })
    localStorage.setItem('fdr_token', data.accessToken)
    setUser(data.user)
    return data.user
  }
  const signOut = () => {
    localStorage.removeItem('fdr_token')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, checking, signIn, signOut }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
