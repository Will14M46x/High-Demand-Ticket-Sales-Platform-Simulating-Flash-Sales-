import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Plus, ArrowLeft, AlertCircle, CheckCircle } from 'lucide-react'
import { inventoryAPI } from '../services/api.service'

function CreateEventPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(false)
  const [formData, setFormData] = useState({
    name: '',
    location: '',
    date: '',
    time: '',
    availableTickets: '',
    price: '',
    description: ''
  })

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value })
    setError(null)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)

    try {
      // Combine date and time
      const dateTime = `${formData.date}T${formData.time}:00`
      
      const eventData = {
        name: formData.name,
        location: formData.location,
        date: dateTime,
        availableTickets: parseInt(formData.availableTickets),
        totalTickets: parseInt(formData.availableTickets),
        price: parseFloat(formData.price),
        description: formData.description
      }

      await inventoryAPI.createEvent(eventData)
      setSuccess(true)
      setTimeout(() => {
        navigate('/events')
      }, 2000)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create event. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
      {/* Back Button */}
      <Link to="/events" className="flex items-center text-gray-600 hover:text-gray-900 mb-6">
        <ArrowLeft className="h-5 w-5 mr-2" />
        Back to Events
      </Link>

      <div className="card">
        <div className="flex items-center space-x-3 mb-8">
          <Plus className="h-8 w-8 text-primary-600" />
          <div>
            <h1 className="text-3xl font-bold text-gray-900">Create New Event</h1>
            <p className="text-gray-600">Add a new event to the ticket sales platform</p>
          </div>
        </div>

        {error && (
          <div className="mb-6 bg-red-50 border border-red-200 rounded-lg p-4 flex items-start space-x-3">
            <AlertCircle className="h-5 w-5 text-red-600 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-red-800">{error}</div>
          </div>
        )}

        {success && (
          <div className="mb-6 bg-green-50 border border-green-200 rounded-lg p-4 flex items-start space-x-3">
            <CheckCircle className="h-5 w-5 text-green-600 flex-shrink-0 mt-0.5" />
            <div className="text-sm text-green-800">
              Event created successfully! Redirecting...
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Event Name */}
          <div>
            <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
              Event Name *
            </label>
            <input
              id="name"
              name="name"
              type="text"
              required
              value={formData.name}
              onChange={handleChange}
              className="input"
              placeholder="e.g., Summer Music Festival 2024"
            />
          </div>

          {/* Location */}
          <div>
            <label htmlFor="location" className="block text-sm font-medium text-gray-700 mb-2">
              Location *
            </label>
            <input
              id="location"
              name="location"
              type="text"
              required
              value={formData.location}
              onChange={handleChange}
              className="input"
              placeholder="e.g., Madison Square Garden, New York"
            />
          </div>

          {/* Date and Time */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="date" className="block text-sm font-medium text-gray-700 mb-2">
                Date *
              </label>
              <input
                id="date"
                name="date"
                type="date"
                required
                value={formData.date}
                onChange={handleChange}
                className="input"
                min={new Date().toISOString().split('T')[0]}
              />
            </div>

            <div>
              <label htmlFor="time" className="block text-sm font-medium text-gray-700 mb-2">
                Time *
              </label>
              <input
                id="time"
                name="time"
                type="time"
                required
                value={formData.time}
                onChange={handleChange}
                className="input"
              />
            </div>
          </div>

          {/* Tickets and Price */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label htmlFor="availableTickets" className="block text-sm font-medium text-gray-700 mb-2">
                Total Tickets *
              </label>
              <input
                id="availableTickets"
                name="availableTickets"
                type="number"
                required
                min="1"
                value={formData.availableTickets}
                onChange={handleChange}
                className="input"
                placeholder="e.g., 1000"
              />
            </div>

            <div>
              <label htmlFor="price" className="block text-sm font-medium text-gray-700 mb-2">
                Price per Ticket ($) *
              </label>
              <input
                id="price"
                name="price"
                type="number"
                required
                min="0"
                step="0.01"
                value={formData.price}
                onChange={handleChange}
                className="input"
                placeholder="e.g., 50.00"
              />
            </div>
          </div>

          {/* Description */}
          <div>
            <label htmlFor="description" className="block text-sm font-medium text-gray-700 mb-2">
              Description (optional)
            </label>
            <textarea
              id="description"
              name="description"
              rows="4"
              value={formData.description}
              onChange={handleChange}
              className="input"
              placeholder="Describe the event, performers, schedule, etc."
            />
          </div>

          {/* Submit Button */}
          <div className="flex space-x-4">
            <button
              type="submit"
              disabled={loading || success}
              className="btn btn-primary flex-1"
            >
              {loading ? 'Creating Event...' : success ? 'Event Created!' : 'Create Event'}
            </button>
            <Link to="/events" className="btn btn-secondary">
              Cancel
            </Link>
          </div>
        </form>

        {/* Info Box */}
        <div className="mt-8 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <h3 className="font-semibold text-blue-900 mb-2">Testing Tips</h3>
          <ul className="space-y-1 text-sm text-blue-800">
            <li>• Create events with different ticket quantities to test inventory management</li>
            <li>• Use low ticket counts (e.g., 5-10) to easily simulate "sold out" scenarios</li>
            <li>• Set events in the near future to test the complete booking flow</li>
            <li>• Create multiple events to test the waiting room and high-demand scenarios</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export default CreateEventPage

