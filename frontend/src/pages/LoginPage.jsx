import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LogIn, AlertCircle, XCircle } from 'lucide-react'
import useAuthStore from '../store/useAuthStore'

function LoginPage() {
  const navigate = useNavigate()
  const { login, loading, error, clearError } = useAuthStore()
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  })
  const [loginFailed, setLoginFailed] = useState(false)

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    // Don't clear error immediately - only clear when submitting again
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    // Clear previous errors
    clearError()
    setLoginFailed(false)
    
    try {
      const response = await login(formData)
      console.log('Login successful:', response)
      
      // Only navigate if we actually got a valid response
      if (response && response.token) {
        navigate('/events')
      }
    } catch (err) {
      console.error('Login failed in component:', err)
      // Error is handled by the store
      setLoginFailed(true)
      
      // Shake animation on error
      const formElement = e.target
      formElement.classList.add('shake')
      setTimeout(() => formElement.classList.remove('shake'), 500)
      
      // Make sure error state persists
      // Don't do anything that might clear it
    }
  }

  const handleDismissError = () => {
    clearError()
    setLoginFailed(false)
  }

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="card">
          <div className="text-center mb-8">
            {loginFailed ? (
              <XCircle className="h-12 w-12 text-red-600 mx-auto mb-4 animate-pulse" />
            ) : (
              <LogIn className="h-12 w-12 text-primary-600 mx-auto mb-4" />
            )}
            <h2 className="text-3xl font-bold text-gray-900">
              {loginFailed ? 'Login Failed' : 'Welcome Back'}
            </h2>
            <p className="mt-2 text-gray-600">
              {loginFailed ? 'Please check your credentials' : 'Sign in to your account'}
            </p>
          </div>

          {error && (
            <div className="mb-6 bg-red-50 border-2 border-red-300 rounded-lg p-4 animate-in">
              <div className="flex items-start space-x-3">
                <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <h3 className="text-sm font-semibold text-red-900 mb-1">Authentication Failed</h3>
                  <p className="text-sm text-red-800">{error}</p>
                  <p className="text-xs text-red-700 mt-2">
                    Please verify your email and password are correct.
                  </p>
                </div>
                <button
                  onClick={handleDismissError}
                  className="text-red-600 hover:text-red-800 transition-colors"
                  type="button"
                >
                  <XCircle className="h-5 w-5" />
                </button>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                value={formData.email}
                onChange={handleChange}
                className={`input ${loginFailed ? 'border-red-300 focus:ring-red-500' : ''}`}
                placeholder="you@example.com"
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                value={formData.password}
                onChange={handleChange}
                className={`input ${loginFailed ? 'border-red-300 focus:ring-red-500' : ''}`}
                placeholder="••••••••"
                autoComplete="current-password"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary w-full"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm">
            <span className="text-gray-600">Don't have an account? </span>
            <Link to="/signup" className="text-primary-600 hover:text-primary-700 font-medium">
              Sign up
            </Link>
          </div>

          {/* Security Notice */}
          {loginFailed && (
            <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="text-xs text-yellow-800">
                <strong>🔒 Security Note:</strong> For your security, accounts are locked after 5 failed login attempts.
              </p>
            </div>
          )}

          {/* Test Credentials */}
          <div className="mt-8 pt-6 border-t border-gray-200">
            <div className="text-xs text-gray-500 text-center">
              <p className="font-medium mb-2">For Testing Firebase Authentication:</p>
              <p>✅ Passwords are now verified with Firebase</p>
              <p className="mt-1">❌ Wrong passwords will be rejected</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default LoginPage

