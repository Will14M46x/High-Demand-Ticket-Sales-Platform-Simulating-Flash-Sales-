import { Outlet, Link, useNavigate } from 'react-router-dom'
import { Ticket, LogOut, User, Calendar, Settings } from 'lucide-react'
import useAuthStore from '../store/useAuthStore'

function Layout() {
  const { isAuthenticated, user, logout } = useAuthStore()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="bg-white border-b border-gray-200 sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2">
              <Ticket className="h-8 w-8 text-primary-600" />
              <span className="text-xl font-bold text-gray-900">TicketMaster</span>
            </Link>

            {/* Navigation */}
            <nav className="hidden md:flex items-center space-x-8">
              <Link to="/events" className="text-gray-700 hover:text-primary-600 transition-colors">
                Events
              </Link>
              {isAuthenticated && (
                <>
                  <Link to="/my-bookings" className="text-gray-700 hover:text-primary-600 transition-colors">
                    My Bookings
                  </Link>
                  <Link to="/admin" className="text-gray-700 hover:text-primary-600 transition-colors">
                    Admin
                  </Link>
                </>
              )}
            </nav>

            {/* User Menu */}
            <div className="flex items-center space-x-4">
              {isAuthenticated ? (
                <>
                  <div className="flex items-center space-x-2 text-sm text-gray-700">
                    <User className="h-4 w-4" />
                    <span>{user?.name || user?.email}</span>
                  </div>
                  <button
                    onClick={handleLogout}
                    className="btn btn-secondary flex items-center space-x-2"
                  >
                    <LogOut className="h-4 w-4" />
                    <span>Logout</span>
                  </button>
                </>
              ) : (
                <>
                  <Link to="/login" className="btn btn-secondary">
                    Login
                  </Link>
                  <Link to="/signup" className="btn btn-primary">
                    Sign Up
                  </Link>
                </>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="bg-white border-t border-gray-200 py-8 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            <div>
              <div className="flex items-center space-x-2 mb-4">
                <Ticket className="h-6 w-6 text-primary-600" />
                <span className="font-bold text-gray-900">TicketMaster</span>
              </div>
              <p className="text-sm text-gray-600">
                High-demand ticket sales platform simulating flash sales scenarios
              </p>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 mb-4">Quick Links</h3>
              <ul className="space-y-2 text-sm text-gray-600">
                <li><Link to="/events" className="hover:text-primary-600">Browse Events</Link></li>
                <li><Link to="/my-bookings" className="hover:text-primary-600">My Bookings</Link></li>
                <li><Link to="/admin" className="hover:text-primary-600">Admin Panel</Link></li>
              </ul>
            </div>
            <div>
              <h3 className="font-semibold text-gray-900 mb-4">API Status</h3>
              <ul className="space-y-2 text-sm text-gray-600">
                <li>Auth Service: <span className="text-green-600">✓</span> 8081</li>
                <li>Inventory Service: <span className="text-green-600">✓</span> 8082</li>
                <li>Booking Service: <span className="text-green-600">✓</span> 8084</li>
                <li>Waiting Room: <span className="text-green-600">✓</span> 8083</li>
              </ul>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-gray-200 text-center text-sm text-gray-600">
            <p>© 2024 High-Demand Ticket Sales Platform - University of Limerick</p>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default Layout

