import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Ticket, Calendar, MapPin, CheckCircle, XCircle, Clock, AlertCircle } from 'lucide-react'
import { bookingAPI } from '../services/api.service'
import { format } from 'date-fns'

function MyBookingsPage() {
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchBookings()
  }, [])

  const fetchBookings = async () => {
    try {
      setLoading(true)
      const data = await bookingAPI.getUserBookings()
      setBookings(Array.isArray(data) ? data : [])
      setError(null)
    } catch (err) {
      // If endpoint doesn't exist yet, show mock data
      setError('Could not load bookings from server. Showing demo data.')
      setBookings([
        {
          id: 1,
          eventName: 'Summer Music Festival 2024',
          eventLocation: 'Madison Square Garden',
          eventDate: new Date('2024-12-31T20:00:00'),
          quantity: 2,
          totalPrice: 100.00,
          status: 'CONFIRMED',
          bookingDate: new Date(),
          confirmationNumber: 'ABC123XYZ'
        },
        {
          id: 2,
          eventName: 'Tech Conference 2024',
          eventLocation: 'Convention Center',
          eventDate: new Date('2024-12-15T09:00:00'),
          quantity: 1,
          totalPrice: 250.00,
          status: 'PENDING',
          bookingDate: new Date(),
          confirmationNumber: 'DEF456UVW'
        }
      ])
    } finally {
      setLoading(false)
    }
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">My Bookings</h1>
        <p className="text-gray-600">View and manage your ticket bookings</p>
      </div>

      {error && (
        <div className="mb-6 bg-yellow-50 border border-yellow-200 rounded-lg p-4 flex items-start space-x-3">
          <AlertCircle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-yellow-800">{error}</div>
        </div>
      )}

      {bookings.length === 0 ? (
        <div className="card text-center py-12">
          <Ticket className="h-16 w-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-600 mb-2">No bookings yet</h3>
          <p className="text-gray-500 mb-6">Start exploring events and book your first ticket!</p>
          <Link to="/events" className="btn btn-primary">
            Browse Events
          </Link>
        </div>
      ) : (
        <div className="space-y-6">
          {bookings.map((booking) => (
            <BookingCard key={booking.id} booking={booking} />
          ))}
        </div>
      )}
    </div>
  )
}

function BookingCard({ booking }) {
  const getStatusBadge = (status) => {
    switch (status?.toUpperCase()) {
      case 'CONFIRMED':
        return (
          <span className="badge badge-success flex items-center space-x-1">
            <CheckCircle className="h-4 w-4" />
            <span>Confirmed</span>
          </span>
        )
      case 'PENDING':
        return (
          <span className="badge badge-warning flex items-center space-x-1">
            <Clock className="h-4 w-4" />
            <span>Pending</span>
          </span>
        )
      case 'CANCELLED':
        return (
          <span className="badge badge-danger flex items-center space-x-1">
            <XCircle className="h-4 w-4" />
            <span>Cancelled</span>
          </span>
        )
      default:
        return (
          <span className="badge badge-info flex items-center space-x-1">
            <Clock className="h-4 w-4" />
            <span>{status}</span>
          </span>
        )
    }
  }

  return (
    <div className="card hover:shadow-lg transition-shadow duration-200">
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between">
        <div className="flex items-start space-x-4 mb-4 lg:mb-0">
          <div className="bg-primary-100 rounded-lg p-3 flex-shrink-0">
            <Ticket className="h-8 w-8 text-primary-600" />
          </div>
          <div className="flex-1">
            <div className="flex items-center space-x-3 mb-2">
              <h3 className="text-xl font-bold text-gray-900">
                {booking.eventName || booking.event?.name || 'Event'}
              </h3>
              {getStatusBadge(booking.status)}
            </div>
            <div className="space-y-1 text-sm text-gray-600">
              <div className="flex items-center">
                <Calendar className="h-4 w-4 mr-2" />
                {booking.eventDate 
                  ? format(new Date(booking.eventDate), 'PPP p')
                  : booking.event?.date 
                    ? format(new Date(booking.event.date), 'PPP p')
                    : 'Date TBA'
                }
              </div>
              <div className="flex items-center">
                <MapPin className="h-4 w-4 mr-2" />
                {booking.eventLocation || booking.event?.location || 'Location TBA'}
              </div>
            </div>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row items-start sm:items-center space-y-4 sm:space-y-0 sm:space-x-6 lg:text-right">
          <div>
            <div className="text-sm text-gray-500 mb-1">Confirmation Number</div>
            <div className="font-mono font-semibold text-gray-900">
              {booking.confirmationNumber || `#${booking.id?.toString().padStart(8, '0')}`}
            </div>
          </div>
          <div>
            <div className="text-sm text-gray-500 mb-1">Quantity</div>
            <div className="font-semibold text-gray-900">
              {booking.quantity} {booking.quantity === 1 ? 'ticket' : 'tickets'}
            </div>
          </div>
          <div>
            <div className="text-sm text-gray-500 mb-1">Total</div>
            <div className="text-2xl font-bold text-primary-600">
              ${typeof booking.totalPrice === 'number' 
                ? booking.totalPrice.toFixed(2) 
                : parseFloat(booking.totalPrice || 0).toFixed(2)
              }
            </div>
          </div>
        </div>
      </div>

      {booking.status === 'CONFIRMED' && (
        <div className="mt-4 pt-4 border-t border-gray-200 flex flex-wrap gap-3">
          <button className="btn btn-primary text-sm">
            View Tickets
          </button>
          <button className="btn btn-secondary text-sm">
            Add to Calendar
          </button>
          <button className="btn btn-secondary text-sm">
            Get Directions
          </button>
        </div>
      )}
    </div>
  )
}

export default MyBookingsPage

