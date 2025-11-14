import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { 
  BarChart3, 
  Users, 
  Ticket, 
  DollarSign, 
  TrendingUp,
  Calendar,
  Settings,
  Plus,
  RefreshCw
} from 'lucide-react'
import { inventoryAPI, bookingAPI } from '../services/api.service'

function AdminPage() {
  const [stats, setStats] = useState({
    totalEvents: 0,
    totalBookings: 0,
    totalRevenue: 0,
    activeUsers: 0
  })
  const [recentEvents, setRecentEvents] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchDashboardData()
  }, [])

  const fetchDashboardData = async () => {
    try {
      setLoading(true)
      const events = await inventoryAPI.getAllEvents()
      
      // Calculate stats
      const totalEvents = events.length
      const totalTicketsSold = events.reduce((sum, event) => 
        sum + ((event.totalTickets || event.availableTickets) - event.availableTickets), 0
      )
      const totalRevenue = events.reduce((sum, event) => 
        sum + (((event.totalTickets || event.availableTickets) - event.availableTickets) * (event.price || 0)), 0
      )

      setStats({
        totalEvents,
        totalBookings: totalTicketsSold,
        totalRevenue,
        activeUsers: Math.floor(Math.random() * 1000) + 100 // Mock data
      })

      setRecentEvents(events.slice(0, 5))
    } catch (err) {
      console.error('Error fetching dashboard data:', err)
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
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Admin Dashboard</h1>
          <p className="text-gray-600">Monitor and manage your ticket sales platform</p>
        </div>
        <div className="flex space-x-3 mt-4 md:mt-0">
          <button 
            onClick={fetchDashboardData}
            className="btn btn-secondary flex items-center space-x-2"
          >
            <RefreshCw className="h-4 w-4" />
            <span>Refresh</span>
          </button>
          <Link to="/events/create" className="btn btn-primary flex items-center space-x-2">
            <Plus className="h-5 w-5" />
            <span>Create Event</span>
          </Link>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatsCard
          icon={<Calendar className="h-6 w-6" />}
          title="Total Events"
          value={stats.totalEvents}
          change="+12%"
          iconColor="bg-blue-100 text-blue-600"
        />
        <StatsCard
          icon={<Ticket className="h-6 w-6" />}
          title="Tickets Sold"
          value={stats.totalBookings}
          change="+18%"
          iconColor="bg-green-100 text-green-600"
        />
        <StatsCard
          icon={<DollarSign className="h-6 w-6" />}
          title="Total Revenue"
          value={`$${stats.totalRevenue.toFixed(2)}`}
          change="+23%"
          iconColor="bg-purple-100 text-purple-600"
        />
        <StatsCard
          icon={<Users className="h-6 w-6" />}
          title="Active Users"
          value={stats.activeUsers}
          change="+8%"
          iconColor="bg-yellow-100 text-yellow-600"
        />
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
        <Link to="/events/create" className="card hover:shadow-lg transition-shadow cursor-pointer">
          <div className="flex items-center space-x-4">
            <div className="bg-primary-100 rounded-lg p-3">
              <Plus className="h-6 w-6 text-primary-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Create Event</h3>
              <p className="text-sm text-gray-600">Add a new event to sell tickets</p>
            </div>
          </div>
        </Link>

        <div className="card hover:shadow-lg transition-shadow cursor-pointer">
          <div className="flex items-center space-x-4">
            <div className="bg-green-100 rounded-lg p-3">
              <BarChart3 className="h-6 w-6 text-green-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">View Analytics</h3>
              <p className="text-sm text-gray-600">Detailed sales reports</p>
            </div>
          </div>
        </div>

        <div className="card hover:shadow-lg transition-shadow cursor-pointer">
          <div className="flex items-center space-x-4">
            <div className="bg-orange-100 rounded-lg p-3">
              <Settings className="h-6 w-6 text-orange-600" />
            </div>
            <div>
              <h3 className="font-semibold text-gray-900">Settings</h3>
              <p className="text-sm text-gray-600">Configure platform settings</p>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Events */}
      <div className="card">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-xl font-bold text-gray-900">Recent Events</h2>
          <Link to="/events" className="text-primary-600 hover:text-primary-700 text-sm font-medium">
            View All →
          </Link>
        </div>

        {recentEvents.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Calendar className="h-12 w-12 text-gray-300 mx-auto mb-3" />
            <p>No events yet. Create your first event to get started!</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-gray-200">
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">Event Name</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">Location</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">Available</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">Price</th>
                  <th className="text-left py-3 px-4 text-sm font-semibold text-gray-700">Status</th>
                </tr>
              </thead>
              <tbody>
                {recentEvents.map((event) => {
                  const availability = event.availableTickets / (event.totalTickets || event.availableTickets)
                  const isSoldOut = event.availableTickets === 0
                  const isLowStock = availability < 0.2 && availability > 0

                  return (
                    <tr key={event.id} className="border-b border-gray-100 hover:bg-gray-50">
                      <td className="py-4 px-4">
                        <Link to={`/events/${event.id}`} className="font-medium text-gray-900 hover:text-primary-600">
                          {event.name}
                        </Link>
                      </td>
                      <td className="py-4 px-4 text-sm text-gray-600">{event.location}</td>
                      <td className="py-4 px-4 text-sm text-gray-600">
                        {event.availableTickets} / {event.totalTickets || event.availableTickets}
                      </td>
                      <td className="py-4 px-4 text-sm font-medium text-gray-900">
                        ${event.price?.toFixed(2)}
                      </td>
                      <td className="py-4 px-4">
                        {isSoldOut ? (
                          <span className="badge badge-danger">Sold Out</span>
                        ) : isLowStock ? (
                          <span className="badge badge-warning">Low Stock</span>
                        ) : (
                          <span className="badge badge-success">Available</span>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* System Status */}
      <div className="mt-8 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="card">
          <h3 className="font-semibold text-gray-900 mb-4">Service Status</h3>
          <div className="space-y-3">
            <ServiceStatus name="Auth Service" port="8081" status="online" />
            <ServiceStatus name="Inventory Service" port="8082" status="online" />
            <ServiceStatus name="Waiting Room" port="8083" status="online" />
            <ServiceStatus name="Booking Service" port="8084" status="online" />
          </div>
        </div>

        <div className="card">
          <h3 className="font-semibold text-gray-900 mb-4">Quick Stats</h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Average Ticket Price</span>
              <span className="font-semibold text-gray-900">
                ${stats.totalBookings > 0 ? (stats.totalRevenue / stats.totalBookings).toFixed(2) : '0.00'}
              </span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Conversion Rate</span>
              <span className="font-semibold text-green-600">18.5%</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Avg. Response Time</span>
              <span className="font-semibold text-gray-900">45ms</span>
            </div>
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Uptime</span>
              <span className="font-semibold text-green-600">99.9%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

function StatsCard({ icon, title, value, change, iconColor }) {
  return (
    <div className="card">
      <div className="flex items-center justify-between mb-4">
        <div className={`${iconColor} rounded-lg p-3`}>
          {icon}
        </div>
        <span className="text-sm font-medium text-green-600 flex items-center">
          <TrendingUp className="h-4 w-4 mr-1" />
          {change}
        </span>
      </div>
      <h3 className="text-sm text-gray-600 mb-1">{title}</h3>
      <div className="text-3xl font-bold text-gray-900">{value}</div>
    </div>
  )
}

function ServiceStatus({ name, port, status }) {
  return (
    <div className="flex items-center justify-between">
      <div className="flex items-center space-x-3">
        <div className={`w-2 h-2 rounded-full ${status === 'online' ? 'bg-green-500' : 'bg-red-500'}`} />
        <span className="text-sm font-medium text-gray-900">{name}</span>
      </div>
      <span className="text-sm text-gray-600">:{port}</span>
    </div>
  )
}

export default AdminPage

