import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation, Link } from 'react-router-dom'
import { CreditCard, CheckCircle, AlertCircle, Clock, Ticket } from 'lucide-react'
import { inventoryAPI, bookingAPI } from '../services/api.service'
import { format } from 'date-fns'
import useAuthStore from '../store/useAuthStore'

function BookingPage() {
  const { eventId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuthStore()
  const quantity = location.state?.quantity || 1

  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [booking, setBooking] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [timeRemaining, setTimeRemaining] = useState(300) // 5 minutes
  const [paymentInfo, setPaymentInfo] = useState({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: ''
  })

  useEffect(() => {
    fetchEvent()
  }, [eventId])

  useEffect(() => {
    // Countdown timer
    if (timeRemaining > 0 && !success) {
      const timer = setInterval(() => {
        setTimeRemaining((prev) => prev - 1)
      }, 1000)
      return () => clearInterval(timer)
    } else if (timeRemaining === 0 && !success) {
      setError('Time expired. Please try again.')
      setTimeout(() => navigate(`/events/${eventId}`), 3000)
    }
  }, [timeRemaining, success])

  const fetchEvent = async () => {
    try {
      const data = await inventoryAPI.getEvent(eventId)
      setEvent(data)
    } catch (err) {
      setError('Failed to load event details')
    } finally {
      setLoading(false)
    }
  }

  const handlePaymentChange = (e) => {
    let value = e.target.value
    const name = e.target.name

    // Format card number with spaces
    if (name === 'cardNumber') {
      value = value.replace(/\s/g, '').replace(/(\d{4})/g, '$1 ').trim()
      value = value.substring(0, 19) // Max 16 digits + 3 spaces
    }

    // Format expiry date
    if (name === 'expiryDate') {
      value = value.replace(/\D/g, '').substring(0, 4)
      if (value.length >= 2) {
        value = value.substring(0, 2) + '/' + value.substring(2)
      }
    }

    // Format CVV
    if (name === 'cvv') {
      value = value.replace(/\D/g, '').substring(0, 3)
    }

    setPaymentInfo({ ...paymentInfo, [name]: value })
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setBooking(true)
    setError(null)

    try {
      // Step 1: Reserve tickets
      await inventoryAPI.reserveTickets(eventId, quantity)

      // Step 2: Create booking
      const bookingData = {
        eventId: parseInt(eventId),
        quantity: quantity,
        totalPrice: (event.price * quantity).toFixed(2)
      }
      await bookingAPI.createBooking(bookingData)

      // Step 3: Simulate payment processing
      await new Promise(resolve => setTimeout(resolve, 2000))

      // Success!
      setSuccess(true)
      setTimeout(() => {
        navigate('/my-bookings')
      }, 3000)
    } catch (err) {
      setError(err.response?.data?.message || 'Booking failed. Please try again.')
      setBooking(false)
    }
  }

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex justify-center items-center h-64">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
        </div>
      </div>
    )
  }

  if (success) {
    return (
      <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full">
          <div className="card text-center">
            <div className="bg-green-100 w-20 h-20 rounded-full mx-auto mb-6 flex items-center justify-center">
              <CheckCircle className="h-10 w-10 text-green-600" />
            </div>
            <h2 className="text-3xl font-bold text-gray-900 mb-4">Booking Confirmed!</h2>
            <p className="text-gray-600 mb-8">
              Your tickets have been reserved successfully. Check your email for confirmation.
            </p>
            <div className="bg-green-50 border border-green-200 rounded-lg p-6 mb-6">
              <div className="text-sm text-gray-600 mb-1">Confirmation Number</div>
              <div className="text-2xl font-bold text-green-600">
                #{Math.random().toString(36).substring(2, 10).toUpperCase()}
              </div>
            </div>
            <Link to="/my-bookings" className="btn btn-primary w-full">
              View My Bookings
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Timer Warning */}
      <div className="mb-6 bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Clock className="h-5 w-5 text-yellow-600" />
          <div>
            <div className="font-semibold text-yellow-900">Complete your booking</div>
            <div className="text-sm text-yellow-800">
              Your tickets are reserved for a limited time
            </div>
          </div>
        </div>
        <div className="text-3xl font-bold text-yellow-600">
          {formatTime(timeRemaining)}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Payment Form */}
        <div className="lg:col-span-2">
          <div className="card">
            <h2 className="text-2xl font-bold text-gray-900 mb-6 flex items-center">
              <CreditCard className="h-6 w-6 mr-3 text-primary-600" />
              Payment Information
            </h2>

            {error && (
              <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
                <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-red-800">{error}</div>
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Cardholder Name
                </label>
                <input
                  type="text"
                  name="cardholderName"
                  required
                  value={paymentInfo.cardholderName}
                  onChange={handlePaymentChange}
                  className="input"
                  placeholder="John Doe"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Card Number
                </label>
                <input
                  type="text"
                  name="cardNumber"
                  required
                  value={paymentInfo.cardNumber}
                  onChange={handlePaymentChange}
                  className="input"
                  placeholder="1234 5678 9012 3456"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Expiry Date
                  </label>
                  <input
                    type="text"
                    name="expiryDate"
                    required
                    value={paymentInfo.expiryDate}
                    onChange={handlePaymentChange}
                    className="input"
                    placeholder="MM/YY"
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    CVV
                  </label>
                  <input
                    type="text"
                    name="cvv"
                    required
                    value={paymentInfo.cvv}
                    onChange={handlePaymentChange}
                    className="input"
                    placeholder="123"
                  />
                </div>
              </div>

              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800">
                <strong>🔒 Test Mode:</strong> This is a demo payment. Use any card number.
                No real payment will be processed.
              </div>

              <button
                type="submit"
                disabled={booking}
                className="btn btn-primary w-full text-lg"
              >
                {booking ? 'Processing...' : `Pay $${(event?.price * quantity).toFixed(2)}`}
              </button>
            </form>
          </div>
        </div>

        {/* Order Summary */}
        <div className="lg:col-span-1">
          <div className="card sticky top-24">
            <h2 className="text-xl font-bold text-gray-900 mb-6">Order Summary</h2>

            {event && (
              <>
                <div className="bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg p-4 mb-6 text-white">
                  <Ticket className="h-8 w-8 mb-2 opacity-75" />
                  <h3 className="font-bold text-lg mb-1">{event.name}</h3>
                  <p className="text-sm opacity-90">
                    {format(new Date(event.date), 'PPP p')}
                  </p>
                  <p className="text-sm opacity-90">{event.location}</p>
                </div>

                <div className="space-y-3 mb-6">
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Customer</span>
                    <span className="font-medium">{user?.name || user?.email}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Quantity</span>
                    <span className="font-medium">{quantity} {quantity === 1 ? 'ticket' : 'tickets'}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Price per ticket</span>
                    <span className="font-medium">${event.price.toFixed(2)}</span>
                  </div>
                  <div className="border-t border-gray-200 pt-3">
                    <div className="flex justify-between">
                      <span className="font-semibold text-gray-900">Total</span>
                      <span className="font-bold text-2xl text-primary-600">
                        ${(event.price * quantity).toFixed(2)}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="border-t border-gray-200 pt-6">
                  <h3 className="font-semibold text-gray-900 mb-3">Protected Purchase</h3>
                  <ul className="space-y-2 text-sm text-gray-600">
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>Secure 256-bit encryption</span>
                    </li>
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>Instant email confirmation</span>
                    </li>
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>24/7 customer support</span>
                    </li>
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>Money-back guarantee</span>
                    </li>
                  </ul>
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default BookingPage

