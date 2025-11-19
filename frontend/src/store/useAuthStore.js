import { create } from 'zustand'
import { authAPI } from '../services/api.service'

const useAuthStore = create((set) => ({
  user: JSON.parse(localStorage.getItem('user')) || null,
  token: localStorage.getItem('token') || null,
  isAuthenticated: !!localStorage.getItem('token'),
  loading: false,
  error: null,

      signup: async (userData) => {
        set({ loading: true, error: null })
        try {
          const response = await authAPI.signup(userData)
          
          // Store both access token and refresh token
          localStorage.setItem('token', response.token)
          localStorage.setItem('refreshToken', response.refreshToken)
          localStorage.setItem('user', JSON.stringify({
            id: response.userId,
            email: response.email,
            name: response.name
          }))
          set({
            user: {
              id: response.userId,
              email: response.email,
              name: response.name
            },
            token: response.token,
            isAuthenticated: true,
            loading: false
          })
          return response
        } catch (error) {
          console.error('Signup error details:', error)
          
          // Extract detailed error message
          let errorMessage = 'Signup failed'
          
          if (error.response?.data) {
            if (typeof error.response.data === 'string') {
              errorMessage = error.response.data
            } else if (error.response.data.message) {
              errorMessage = error.response.data.message
            } else if (error.response.data.error) {
              errorMessage = error.response.data.error
            }
          } else if (error.message) {
            if (error.message === 'Network Error' || error.code === 'ERR_NETWORK') {
              errorMessage = 'Cannot connect to server. Please make sure the Auth Service is running on port 8081.'
            } else {
              errorMessage = error.message
            }
          }
          
          set({ error: errorMessage, loading: false })
          throw error
        }
      },

      login: async (credentials) => {
        set({ loading: true, error: null })
        try {
          const response = await authAPI.login(credentials)
          
          // Validate response before proceeding
          if (!response || !response.token) {
            const errorMsg = 'Invalid response from server'
            set({ error: errorMsg, loading: false })
            throw new Error(errorMsg)
          }
          
          // Store both access token and refresh token
          localStorage.setItem('token', response.token)
          localStorage.setItem('refreshToken', response.refreshToken)
          localStorage.setItem('user', JSON.stringify({
            id: response.userId,
            email: response.email,
            name: response.name
          }))
          set({
            user: {
              id: response.userId,
              email: response.email,
              name: response.name
            },
            token: response.token,
            isAuthenticated: true,
            loading: false,
            error: null  // Explicitly clear error on success
          })
          return response
        } catch (error) {
          console.error('Login error:', error)
          
          // Extract error message
          let errorMessage = 'Login failed'
          if (error.response?.data?.message) {
            errorMessage = error.response.data.message
          } else if (error.response?.data) {
            errorMessage = typeof error.response.data === 'string' 
              ? error.response.data 
              : 'Invalid email or password'
          } else if (error.message) {
            errorMessage = error.message
          }
          
          // Set error and keep it
          set({ 
            error: errorMessage, 
            loading: false,
            isAuthenticated: false,
            user: null,
            token: null
          })
          
          throw error
        }
      },

  logout: async () => {
    try {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        await authAPI.logout(refreshToken)
      }
    } catch (error) {
      console.error('Logout error:', error)
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('user')
      set({
        user: null,
        token: null,
        isAuthenticated: false,
        error: null
      })
    }
  },

  clearError: () => set({ error: null })
}))

export default useAuthStore

