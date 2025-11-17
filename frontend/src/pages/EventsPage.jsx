import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { Calendar, MapPin, Ticket, Plus, Search, AlertCircle } from 'lucide-react'
import { inventoryAPI } from '../services/api.service'
import { format } from 'date-fns'
import useAuthStore from '../store/useAuthStore'

function EventsPage() {
  const { isAuthenticated } = useAuthStore()
  const [events, setEvents] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchEvents()
  }, [])

  const fetchEvents = async () => {
    try {
      setLoading(true)
      const data = await inventoryAPI.getAllEvents()
      setEvents(Array.isArray(data) ? data : [])
      setError(null)
    } catch (err) {
      setError('Failed to load events. Please try again later.')
      console.error('Error fetching events:', err)
    } finally {
      setLoading(false)
    }
  }

  const filteredEvents = events.filter(event =>
    event.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    event.location?.toLowerCase().includes(searchTerm.toLowerCase())
  )

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
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Upcoming Events</h1>
          <p className="text-gray-600">Browse and book tickets for exciting events</p>
        </div>
        {isAuthenticated && (
          <Link to="/events/create" className="btn btn-primary flex items-center space-x-2 mt-4 md:mt-0">
            <Plus className="h-5 w-5" />
            <span>Create Event</span>
          </Link>
        )}
      </div>

      {/* Search */}
      <div className="mb-8">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search events by name or location..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="input pl-10"
          />
        </div>
      </div>

      {/* Error Message */}
      {error && (
        <div className="mb-8 bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
          <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
          <div className="text-sm text-red-800">{error}</div>
        </div>
      )}

      {/* Events Grid */}
      {filteredEvents.length === 0 ? (
        <div className="text-center py-12">
          <Ticket className="h-16 w-16 text-gray-300 mx-auto mb-4" />
          <h3 className="text-xl font-semibold text-gray-600 mb-2">No events found</h3>
          <p className="text-gray-500">
            {searchTerm ? 'Try adjusting your search' : 'Check back later for new events'}
          </p>
          {isAuthenticated && (
            <Link to="/events/create" className="btn btn-primary mt-6 inline-flex items-center space-x-2">
              <Plus className="h-5 w-5" />
              <span>Create First Event</span>
            </Link>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredEvents.map((event) => (
            <EventCard key={event.id} event={event} />
          ))}
        </div>
      )}
    </div>
  )
}

function EventCard({ event }) {
  const { isAuthenticated } = useAuthStore()
  const availability = event.availableTickets / event.totalTickets || 0
  const isLowStock = availability < 0.2 && availability > 0
  const isSoldOut = event.availableTickets === 0

  return (
    <Link to={`/events/${event.id}`} className="card hover:shadow-lg transition-shadow duration-200">
      <div className="aspect-video bg-gradient-to-br from-primary-400 to-primary-600 rounded-lg mb-4 flex items-center justify-center">
        <Ticket className="h-16 w-16 text-white opacity-50" />
      </div>

      <h3 className="text-xl font-bold text-gray-900 mb-2">{event.name}</h3>

      <div className="space-y-2 mb-4">
        <div className="flex items-center text-sm text-gray-600">
          <Calendar className="h-4 w-4 mr-2" />
          {event.date ? format(new Date(event.date), 'PPP p') : 'Date TBA'}
        </div>
        <div className="flex items-center text-sm text-gray-600">
          <MapPin className="h-4 w-4 mr-2" />
          {event.location || 'Location TBA'}
        </div>
      </div>

      <div className="flex items-center justify-between pt-4 border-t border-gray-200">
        <div>
          <div className="text-2xl font-bold text-primary-600">
            ${event.price?.toFixed(2) || '0.00'}
          </div>
          <div className="text-xs text-gray-500">per ticket</div>
        </div>
        <div className="text-right">
          {isSoldOut ? (
            <span className="badge badge-danger">Sold Out</span>
          ) : isLowStock ? (
            <span className="badge badge-warning">
              Only {event.availableTickets} left
            </span>
          ) : (
            <span className="badge badge-success">
              {event.availableTickets} available
            </span>
          )}
        </div>
      </div>

      {!isSoldOut && isAuthenticated && (
        <button className="btn btn-primary w-full mt-4">
          Book Now
        </button>
      )}
    </Link>
  )
}

export default EventsPage

