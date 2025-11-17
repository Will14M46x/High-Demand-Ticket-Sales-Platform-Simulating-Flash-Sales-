import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { Calendar, MapPin, Ticket, Users, DollarSign, ArrowLeft, AlertCircle } from 'lucide-react'
import { inventoryAPI } from '../services/api.service'
import { format } from 'date-fns'
import useAuthStore from '../store/useAuthStore'

function EventDetailPage() {
  const { eventId } = useParams()
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()
  const [event, setEvent] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [quantity, setQuantity] = useState(1)

  useEffect(() => {
    fetchEvent()
  }, [eventId])

  const fetchEvent = async () => {
    try {
      setLoading(true)
      const data = await inventoryAPI.getEvent(eventId)
      setEvent(data)
      setError(null)
    } catch (err) {
      setError('Failed to load event details')
      console.error('Error fetching event:', err)
    } finally {
      setLoading(false)
    }
  }

  const handleBooking = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: `/events/${eventId}` } })
      return
    }
    
    // Navigate to waiting room first for high-demand simulation
    navigate(`/waiting-room/${eventId}`, { state: { quantity } })
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

  if (error || !event) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="card text-center">
          <AlertCircle className="h-16 w-16 text-red-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Event Not Found</h2>
          <p className="text-gray-600 mb-6">{error || 'The event you are looking for does not exist.'}</p>
          <Link to="/events" className="btn btn-primary">
            Back to Events
          </Link>
        </div>
      </div>
    )
  }

  const isSoldOut = event.availableTickets === 0
  const availability = event.availableTickets / event.totalTickets || 0
  const isLowStock = availability < 0.2 && availability > 0
  const maxQuantity = Math.min(event.availableTickets, 10)

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Back Button */}
      <Link to="/events" className="flex items-center text-gray-600 hover:text-gray-900 mb-6">
        <ArrowLeft className="h-5 w-5 mr-2" />
        Back to Events
      </Link>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Event Details */}
        <div className="lg:col-span-2">
          <div className="card">
            <div className="aspect-video bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg mb-6 flex items-center justify-center">
              <Ticket className="h-32 w-32 text-white opacity-50" />
            </div>

            <h1 className="text-4xl font-bold text-gray-900 mb-4">{event.name}</h1>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
              <div className="flex items-center text-gray-700">
                <Calendar className="h-5 w-5 mr-3 text-primary-600" />
                <div>
                  <div className="text-sm text-gray-500">Date & Time</div>
                  <div className="font-medium">
                    {event.date ? format(new Date(event.date), 'PPP p') : 'Date TBA'}
                  </div>
                </div>
              </div>

              <div className="flex items-center text-gray-700">
                <MapPin className="h-5 w-5 mr-3 text-primary-600" />
                <div>
                  <div className="text-sm text-gray-500">Location</div>
                  <div className="font-medium">{event.location || 'Location TBA'}</div>
                </div>
              </div>

              <div className="flex items-center text-gray-700">
                <Users className="h-5 w-5 mr-3 text-primary-600" />
                <div>
                  <div className="text-sm text-gray-500">Available Tickets</div>
                  <div className="font-medium">{event.availableTickets} / {event.totalTickets}</div>
                </div>
              </div>

              <div className="flex items-center text-gray-700">
                <DollarSign className="h-5 w-5 mr-3 text-primary-600" />
                <div>
                  <div className="text-sm text-gray-500">Price per Ticket</div>
                  <div className="font-medium">${event.price?.toFixed(2) || '0.00'}</div>
                </div>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="mb-6">
              <div className="flex justify-between text-sm text-gray-600 mb-2">
                <span>Availability</span>
                <span>{(availability * 100).toFixed(0)}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full transition-all ${
                    isSoldOut ? 'bg-red-500' :
                    isLowStock ? 'bg-yellow-500' :
                    'bg-green-500'
                  }`}
                  style={{ width: `${availability * 100}%` }}
                />
              </div>
            </div>

            {/* Description */}
            <div className="prose max-w-none">
              <h2 className="text-xl font-semibold text-gray-900 mb-3">About This Event</h2>
              <p className="text-gray-600">
                {event.description || 'Join us for an unforgettable experience! This event promises to be spectacular with amazing performances, great atmosphere, and memories that will last a lifetime. Don\'t miss out on this incredible opportunity!'}
              </p>
            </div>
          </div>
        </div>

        {/* Booking Card */}
        <div className="lg:col-span-1">
          <div className="card sticky top-24">
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Book Tickets</h2>

            {isSoldOut ? (
              <div className="text-center py-8">
                <div className="badge badge-danger text-lg mb-4">Sold Out</div>
                <p className="text-gray-600">
                  Unfortunately, all tickets for this event have been sold.
                </p>
              </div>
            ) : (
              <>
                {isLowStock && (
                  <div className="mb-4 bg-yellow-50 border border-yellow-200 rounded-lg p-3 flex items-start space-x-2">
                    <AlertCircle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
                    <p className="text-sm text-yellow-800">
                      Hurry! Only {event.availableTickets} tickets remaining
                    </p>
                  </div>
                )}

                <div className="mb-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Number of Tickets
                  </label>
                  <select
                    value={quantity}
                    onChange={(e) => setQuantity(parseInt(e.target.value))}
                    className="input"
                  >
                    {Array.from({ length: maxQuantity }, (_, i) => i + 1).map((num) => (
                      <option key={num} value={num}>
                        {num} {num === 1 ? 'ticket' : 'tickets'}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="mb-6 p-4 bg-gray-50 rounded-lg">
                  <div className="flex justify-between text-sm text-gray-600 mb-2">
                    <span>Price per ticket</span>
                    <span>${event.price?.toFixed(2)}</span>
                  </div>
                  <div className="flex justify-between text-sm text-gray-600 mb-2">
                    <span>Quantity</span>
                    <span>× {quantity}</span>
                  </div>
                  <div className="border-t border-gray-300 my-2"></div>
                  <div className="flex justify-between text-lg font-bold text-gray-900">
                    <span>Total</span>
                    <span>${((event.price || 0) * quantity).toFixed(2)}</span>
                  </div>
                </div>

                {isAuthenticated ? (
                  <button
                    onClick={handleBooking}
                    className="btn btn-primary w-full text-lg"
                  >
                    Proceed to Booking
                  </button>
                ) : (
                  <div>
                    <Link to="/login" className="btn btn-primary w-full text-lg block text-center mb-2">
                      Sign In to Book
                    </Link>
                    <p className="text-sm text-gray-600 text-center">
                      Don't have an account?{' '}
                      <Link to="/signup" className="text-primary-600 hover:text-primary-700">
                        Sign up
                      </Link>
                    </p>
                  </div>
                )}

                <div className="mt-6 pt-6 border-t border-gray-200">
                  <h3 className="font-semibold text-gray-900 mb-2">Booking Protection</h3>
                  <ul className="space-y-2 text-sm text-gray-600">
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>Secure payment processing</span>
                    </li>
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>5-minute ticket hold guarantee</span>
                    </li>
                    <li className="flex items-start">
                      <span className="text-green-600 mr-2">✓</span>
                      <span>Instant confirmation</span>
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

export default EventDetailPage

