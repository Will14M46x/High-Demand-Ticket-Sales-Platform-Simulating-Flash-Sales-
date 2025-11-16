import axios from 'axios'
import { API_ENDPOINTS } from '../config/api'

// Create axios instance with default config
const api = axios.create({
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Track if we're currently refreshing the token
let isRefreshing = false
let failedQueue = []

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error)
    } else {
      prom.resolve(token)
    }
  })
  
  failedQueue = []
}

// Response interceptor to handle errors and auto-refresh tokens
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config
    
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Check if this is a login/signup/refresh request
      const url = originalRequest?.url || ''
      const isAuthRequest = url.includes('/login') || url.includes('/signup') || url.includes('/verify-firebase-token') || url.includes('/refresh-token')
      
      if (isAuthRequest) {
        // Don't try to refresh for auth requests, just pass the error through
        return Promise.reject(error)
      }
      
      // Try to refresh the token
      const refreshToken = localStorage.getItem('refreshToken')
      
      if (!refreshToken) {
        // No refresh token available, redirect to login
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
        return Promise.reject(error)
      }
      
      if (isRefreshing) {
        // If we're already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then(token => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return api(originalRequest)
        }).catch(err => {
          return Promise.reject(err)
        })
      }
      
      originalRequest._retry = true
      isRefreshing = true
      
      try {
        // Try to refresh the token
        const response = await axios.post(API_ENDPOINTS.REFRESH_TOKEN, { refreshToken })
        const { token, refreshToken: newRefreshToken } = response.data
        
        // Update stored tokens
        localStorage.setItem('token', token)
        localStorage.setItem('refreshToken', newRefreshToken)
        
        // Update the authorization header
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`
        originalRequest.headers.Authorization = `Bearer ${token}`
        
        processQueue(null, token)
        
        // Retry the original request
        return api(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError, null)
        
        // Refresh failed, clear everything and redirect to login
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        localStorage.removeItem('user')
        
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login'
        }
        
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }
    
    return Promise.reject(error)
  }
)

// Auth API
export const authAPI = {
  signup: async (userData) => {
    const response = await api.post(API_ENDPOINTS.SIGNUP, userData)
    return response.data
  },
  
  login: async (credentials) => {
    const response = await api.post(API_ENDPOINTS.LOGIN, credentials)
    return response.data
  },
  
  refreshToken: async (refreshToken) => {
    const response = await axios.post(API_ENDPOINTS.REFRESH_TOKEN, { refreshToken })
    return response.data
  },
  
  logout: async (refreshToken) => {
    const response = await api.post(API_ENDPOINTS.LOGOUT, { refreshToken })
    return response.data
  },
  
  logoutAll: async (userId) => {
    const response = await api.post(API_ENDPOINTS.LOGOUT_ALL(userId))
    return response.data
  },
  
  validateToken: async () => {
    const response = await api.get(API_ENDPOINTS.VALIDATE_TOKEN)
    return response.data
  },
  
  getUser: async (userId) => {
    const response = await api.get(API_ENDPOINTS.GET_USER(userId))
    return response.data
  },
  
  getRateLimitInfo: async (email) => {
    const response = await api.get(API_ENDPOINTS.RATE_LIMIT_INFO(email))
    return response.data
  },
  
  getLoginHistory: async (userId) => {
    const response = await api.get(API_ENDPOINTS.LOGIN_HISTORY(userId))
    return response.data
  },
  
  getActiveSessions: async (userId) => {
    const response = await api.get(API_ENDPOINTS.ACTIVE_SESSIONS(userId))
    return response.data
  }
}

// Inventory API
export const inventoryAPI = {
    getAllEvents: async () => {
        const response = await api.get(API_ENDPOINTS.GET_EVENTS)
        return response.data
    },

    getEvent: async (eventId) => {
        const response = await api.get(API_ENDPOINTS.GET_EVENT(eventId))
        return response.data
    },

    createEvent: async (eventData) => {
        // We update the date field to match our backend DTO
        const backendEventData = {
            ...eventData,
            saleStartTime: eventData.date // The frontend sends 'date'
        };
        const response = await api.post(API_ENDPOINTS.CREATE_EVENT, backendEventData)
        return response.data
    },

    updateEvent: async (eventId, eventData) => {
        const response = await api.put(API_ENDPOINTS.UPDATE_EVENT(eventId), eventData)
        return response.data
    },

    deleteEvent: async (eventId) => {
        const response = await api.delete(API_ENDPOINTS.DELETE_EVENT(eventId))
        return response.data
    },

    reserveTickets: async (eventId, quantity) => {
        // This is not used by the frontend flow, but good to have
        const response = await api.post(API_ENDPOINTS.RESERVE_TICKETS(eventId, quantity))
        return response.data
    }
}

// Waiting Room API
export const waitingRoomAPI = {
    // --- UPDATED THIS FUNCTION ---
    joinQueue: async (joinData) => {
        // It now sends the joinData object as the request body
        const response = await api.post(API_ENDPOINTS.JOIN_QUEUE, joinData)
        return response.data
    },

    // --- ADDED THIS FUNCTION ---
    getPosition: async (userId, eventId) => {
        const response = await api.get(API_ENDPOINTS.GET_POSITION(userId, eventId))
        return response.data
    },

    getQueueStatus: async (eventId) => {
        const response = await api.get(API_ENDPOINTS.GET_STATUS(eventId))
        return response.data
    },

    // --- ADDED THIS FUNCTION ---
    admitBatch: async (eventId, batchSize) => {
        const response = await api.post(API_ENDPOINTS.ADMIT_BATCH, { eventId, batchSize })
        return response.data
    }
}

// Booking API
export const bookingAPI = {
    createBooking: async (bookingData) => {
        const response = await api.post(API_ENDPOINTS.CREATE_BOOKING, bookingData)
        return response.data
    },

    getUserBookings: async () => {
        const response = await api.get(API_ENDPOINTS.GET_BOOKINGS)
        return response.data
    },

    getBooking: async (bookingId) => {
        const response = await api.get(API_ENDPOINTS.GET_BOOKING(bookingId))
        return response.data
    },

    confirmBooking: async (bookingId) => {
        const response = await api.post(API_ENDPOINTS.CONFIRM_BOOKING(bookingId))
        return response.data
    },

    cancelBooking: async (bookingId) => {
        const response = await api.post(API_ENDPOINTS.CANCEL_BOOKING(bookingId))
        return response.data
    }
}

export default api
