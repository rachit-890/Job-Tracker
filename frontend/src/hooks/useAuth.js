import { useState, useCallback } from 'react'
import { login as loginApi } from '../api/auth'

export function useAuth() {
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(null)

    const isAuthenticated = () => !!localStorage.getItem('accessToken')

    const login = useCallback(async (credentials) => {
        setLoading(true)
        setError(null)
        try {
            const data = await loginApi(credentials)
            localStorage.setItem('accessToken', data.accessToken)
            localStorage.setItem('refreshToken', data.refreshToken)
            return true
        } catch (err) {
            setError(err.response?.data?.detail || 'Invalid username or password')
            return false
        } finally {
            setLoading(false)
        }
    }, [])

    const logout = useCallback(() => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        window.location.href = '/login'
    }, [])

    return { login, logout, isAuthenticated, loading, error }
}