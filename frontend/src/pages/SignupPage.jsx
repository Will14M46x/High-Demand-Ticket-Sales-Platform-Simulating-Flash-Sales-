import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPlus, AlertCircle, XCircle, Check, X } from 'lucide-react'
import useAuthStore from '../store/useAuthStore'

function SignupPage() {
  const navigate = useNavigate()
  const { signup, loading, error, clearError } = useAuthStore()
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    confirmPassword: '',
    name: '',
    phoneNumber: ''
  })
  const [validationError, setValidationError] = useState('')
  const [signupFailed, setSignupFailed] = useState(false)
  
  // Real-time validation states
  const [validation, setValidation] = useState({
    emailValid: false,
    emailTouched: false,
    phoneValid: false,
    phoneTouched: false,
    passwordLength: false,
    passwordHasNumber: false,
    passwordHasLetter: false,
    passwordsMatch: false,
    passwordTouched: false,
    confirmPasswordTouched: false
  })

  // Validate email format
  const validateEmail = (email) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    return emailRegex.test(email)
  }

  // Validate phone format (accepts various formats)
  const validatePhone = (phone) => {
    if (!phone) return true // Phone is optional
    const phoneRegex = /^[\+]?[(]?[0-9]{1,4}[)]?[-\s\.]?[(]?[0-9]{1,4}[)]?[-\s\.]?[0-9]{1,9}$/
    return phoneRegex.test(phone)
  }

  // Validate password requirements
  const validatePassword = (password) => {
    return {
      length: password.length >= 6,
      hasNumber: /\d/.test(password),
      hasLetter: /[a-zA-Z]/.test(password)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData({ ...formData, [name]: value })
    
    // Real-time validation
    if (name === 'email') {
      setValidation(prev => ({
        ...prev,
        emailValid: validateEmail(value),
        emailTouched: true
      }))
    }
    
    if (name === 'phoneNumber') {
      setValidation(prev => ({
        ...prev,
        phoneValid: validatePhone(value),
        phoneTouched: value.length > 0
      }))
    }
    
    if (name === 'password') {
      const passwordChecks = validatePassword(value)
      setValidation(prev => ({
        ...prev,
        passwordLength: passwordChecks.length,
        passwordHasNumber: passwordChecks.hasNumber,
        passwordHasLetter: passwordChecks.hasLetter,
        passwordTouched: true,
        passwordsMatch: value === formData.confirmPassword
      }))
    }
    
    if (name === 'confirmPassword') {
      setValidation(prev => ({
        ...prev,
        passwordsMatch: value === formData.password,
        confirmPasswordTouched: true
      }))
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    // Clear previous errors
    clearError()
    setValidationError('')
    setSignupFailed(false)
    
    // Validate email format
    if (!validateEmail(formData.email)) {
      setValidationError('Please enter a valid email address')
      setSignupFailed(true)
      return
    }
    
    // Validate phone format (if provided)
    if (formData.phoneNumber && !validatePhone(formData.phoneNumber)) {
      setValidationError('Please enter a valid phone number')
      setSignupFailed(true)
      return
    }
    
    // Validate name
    if (formData.name.trim().length < 2) {
      setValidationError('Name must be at least 2 characters')
      setSignupFailed(true)
      return
    }
    
    // Validate password requirements
    const passwordChecks = validatePassword(formData.password)
    if (!passwordChecks.length) {
      setValidationError('Password must be at least 6 characters')
      setSignupFailed(true)
      return
    }
    if (!passwordChecks.hasLetter) {
      setValidationError('Password must contain at least one letter')
      setSignupFailed(true)
      return
    }
    if (!passwordChecks.hasNumber) {
      setValidationError('Password must contain at least one number')
      setSignupFailed(true)
      return
    }
    
    // Validate passwords match
    if (formData.password !== formData.confirmPassword) {
      setValidationError('Passwords do not match')
      setSignupFailed(true)
      return
    }

    try {
      const { confirmPassword, ...signupData } = formData
      await signup(signupData)
      navigate('/events')
    } catch (err) {
      // Error is handled by the store
      setSignupFailed(true)
      
      // Shake animation on error
      const formElement = e.target
      formElement.classList.add('shake')
      setTimeout(() => formElement.classList.remove('shake'), 500)
    }
  }

  const handleDismissError = () => {
    clearError()
    setValidationError('')
    setSignupFailed(false)
  }

  const displayError = validationError || error

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full">
        <div className="card">
          <div className="text-center mb-8">
            {signupFailed ? (
              <XCircle className="h-12 w-12 text-red-600 mx-auto mb-4 animate-pulse" />
            ) : (
              <UserPlus className="h-12 w-12 text-primary-600 mx-auto mb-4" />
            )}
            <h2 className="text-3xl font-bold text-gray-900">
              {signupFailed ? 'Signup Failed' : 'Create Account'}
            </h2>
            <p className="mt-2 text-gray-600">
              {signupFailed ? 'Please check the information below' : 'Join us and start booking tickets'}
            </p>
          </div>

          {displayError && (
            <div className="mb-6 bg-red-50 border-2 border-red-300 rounded-lg p-4 animate-in">
              <div className="flex items-start space-x-3">
                <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <h3 className="text-sm font-semibold text-red-900 mb-1">
                    {validationError ? 'Validation Error' : 'Signup Failed'}
                  </h3>
                  <p className="text-sm text-red-800">{displayError}</p>
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
              <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                Full Name *
              </label>
              <input
                id="name"
                name="name"
                type="text"
                required
                value={formData.name}
                onChange={handleChange}
                className={`input ${signupFailed && formData.name.length < 2 ? 'border-red-300' : ''}`}
                placeholder="John Doe"
                minLength={2}
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                Email Address *
              </label>
              <div className="relative">
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  className={`input ${
                    validation.emailTouched
                      ? validation.emailValid
                        ? 'border-green-300 focus:ring-green-500'
                        : 'border-red-300 focus:ring-red-500'
                      : ''
                  }`}
                  placeholder="you@example.com"
                />
                {validation.emailTouched && (
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                    {validation.emailValid ? (
                      <Check className="h-5 w-5 text-green-600" />
                    ) : (
                      <X className="h-5 w-5 text-red-600" />
                    )}
                  </div>
                )}
              </div>
              {validation.emailTouched && !validation.emailValid && (
                <p className="mt-1 text-xs text-red-600">Please enter a valid email address</p>
              )}
            </div>

            <div>
              <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700 mb-2">
                Phone Number <span className="text-gray-400">(optional)</span>
              </label>
              <div className="relative">
                <input
                  id="phoneNumber"
                  name="phoneNumber"
                  type="tel"
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  className={`input ${
                    validation.phoneTouched
                      ? validation.phoneValid
                        ? 'border-green-300 focus:ring-green-500'
                        : 'border-red-300 focus:ring-red-500'
                      : ''
                  }`}
                  placeholder="+1234567890 or (555) 123-4567"
                />
                {validation.phoneTouched && (
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                    {validation.phoneValid ? (
                      <Check className="h-5 w-5 text-green-600" />
                    ) : (
                      <X className="h-5 w-5 text-red-600" />
                    )}
                  </div>
                )}
              </div>
              {validation.phoneTouched && !validation.phoneValid && (
                <p className="mt-1 text-xs text-red-600">Please enter a valid phone number</p>
              )}
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                Password *
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                value={formData.password}
                onChange={handleChange}
                className={`input ${
                  validation.passwordTouched && !validation.passwordLength
                    ? 'border-red-300 focus:ring-red-500'
                    : validation.passwordTouched && validation.passwordLength
                    ? 'border-green-300 focus:ring-green-500'
                    : ''
                }`}
                placeholder="••••••••"
              />
              
              {/* Password Requirements */}
              {validation.passwordTouched && (
                <div className="mt-2 space-y-1">
                  <div className="flex items-center text-xs">
                    {validation.passwordLength ? (
                      <Check className="h-4 w-4 text-green-600 mr-2" />
                    ) : (
                      <X className="h-4 w-4 text-red-600 mr-2" />
                    )}
                    <span className={validation.passwordLength ? 'text-green-700' : 'text-red-700'}>
                      At least 6 characters
                    </span>
                  </div>
                  <div className="flex items-center text-xs">
                    {validation.passwordHasLetter ? (
                      <Check className="h-4 w-4 text-green-600 mr-2" />
                    ) : (
                      <X className="h-4 w-4 text-red-600 mr-2" />
                    )}
                    <span className={validation.passwordHasLetter ? 'text-green-700' : 'text-red-700'}>
                      Contains at least one letter
                    </span>
                  </div>
                  <div className="flex items-center text-xs">
                    {validation.passwordHasNumber ? (
                      <Check className="h-4 w-4 text-green-600 mr-2" />
                    ) : (
                      <X className="h-4 w-4 text-red-600 mr-2" />
                    )}
                    <span className={validation.passwordHasNumber ? 'text-green-700' : 'text-red-700'}>
                      Contains at least one number
                    </span>
                  </div>
                </div>
              )}
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
                Confirm Password *
              </label>
              <div className="relative">
                <input
                  id="confirmPassword"
                  name="confirmPassword"
                  type="password"
                  required
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  className={`input ${
                    validation.confirmPasswordTouched
                      ? validation.passwordsMatch && formData.confirmPassword
                        ? 'border-green-300 focus:ring-green-500'
                        : 'border-red-300 focus:ring-red-500'
                      : ''
                  }`}
                  placeholder="••••••••"
                />
                {validation.confirmPasswordTouched && formData.confirmPassword && (
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                    {validation.passwordsMatch ? (
                      <Check className="h-5 w-5 text-green-600" />
                    ) : (
                      <X className="h-5 w-5 text-red-600" />
                    )}
                  </div>
                )}
              </div>
              {validation.confirmPasswordTouched && !validation.passwordsMatch && formData.confirmPassword && (
                <p className="mt-1 text-xs text-red-600">Passwords do not match</p>
              )}
              {validation.confirmPasswordTouched && validation.passwordsMatch && formData.confirmPassword && (
                <p className="mt-1 text-xs text-green-600">Passwords match ✓</p>
              )}
            </div>

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary w-full"
            >
              {loading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm">
            <span className="text-gray-600">Already have an account? </span>
            <Link to="/login" className="text-primary-600 hover:text-primary-700 font-medium">
              Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

export default SignupPage

