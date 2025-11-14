import { Link } from 'react-router-dom'
import { Ticket, Zap, Shield, Clock } from 'lucide-react'

function HomePage() {
  return (
    <div className="bg-gradient-to-b from-primary-50 to-white">
      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <div className="text-center">
          <h1 className="text-5xl md:text-6xl font-bold text-gray-900 mb-6">
            Experience <span className="text-primary-600">Flash Sales</span>
            <br />
            Done Right
          </h1>
          <p className="text-xl text-gray-600 mb-8 max-w-2xl mx-auto">
            A production-ready microservices platform designed to handle extreme concurrency
            and ensure absolute inventory integrity under peak load.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link to="/events" className="btn btn-primary text-lg px-8 py-3">
              Browse Events
            </Link>
            <Link to="/signup" className="btn btn-secondary text-lg px-8 py-3">
              Get Started
            </Link>
          </div>
        </div>

        {/* Stats */}
        <div className="mt-20 grid grid-cols-1 md:grid-cols-4 gap-8">
          <div className="text-center">
            <div className="text-4xl font-bold text-primary-600">99.9%</div>
            <div className="text-gray-600 mt-2">Uptime</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-primary-600">&lt;100ms</div>
            <div className="text-gray-600 mt-2">Response Time</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-primary-600">0</div>
            <div className="text-gray-600 mt-2">Oversold Tickets</div>
          </div>
          <div className="text-center">
            <div className="text-4xl font-bold text-primary-600">1000+</div>
            <div className="text-gray-600 mt-2">Concurrent Users</div>
          </div>
        </div>
      </div>

      {/* Features */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20">
        <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
          Built for Scale & Reliability
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          <div className="card text-center">
            <div className="bg-primary-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Zap className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Lightning Fast</h3>
            <p className="text-gray-600">
              Optimized for handling thousands of simultaneous ticket purchases
            </p>
          </div>

          <div className="card text-center">
            <div className="bg-primary-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Shield className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Secure</h3>
            <p className="text-gray-600">
              Firebase Authentication with JWT tokens across all microservices
            </p>
          </div>

          <div className="card text-center">
            <div className="bg-primary-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Clock className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Fair Queue</h3>
            <p className="text-gray-600">
              Intelligent waiting room system ensures fair access during high demand
            </p>
          </div>

          <div className="card text-center">
            <div className="bg-primary-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
              <Ticket className="h-8 w-8 text-primary-600" />
            </div>
            <h3 className="text-xl font-semibold mb-2">Never Oversell</h3>
            <p className="text-gray-600">
              Atomic inventory updates with distributed locking mechanisms
            </p>
          </div>
        </div>
      </div>

      {/* Architecture */}
      <div className="bg-gray-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <h2 className="text-3xl font-bold text-center text-gray-900 mb-12">
            Microservices Architecture
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div className="card">
              <div className="text-primary-600 font-semibold mb-2">Auth Service</div>
              <div className="text-2xl font-bold text-gray-900 mb-2">:8081</div>
              <p className="text-sm text-gray-600">User authentication with Firebase & JWT</p>
            </div>
            <div className="card">
              <div className="text-primary-600 font-semibold mb-2">Inventory Service</div>
              <div className="text-2xl font-bold text-gray-900 mb-2">:8082</div>
              <p className="text-sm text-gray-600">Event and ticket inventory management</p>
            </div>
            <div className="card">
              <div className="text-primary-600 font-semibold mb-2">Waiting Room</div>
              <div className="text-2xl font-bold text-gray-900 mb-2">:8083</div>
              <p className="text-sm text-gray-600">Queue management for load control</p>
            </div>
            <div className="card">
              <div className="text-primary-600 font-semibold mb-2">Booking Service</div>
              <div className="text-2xl font-bold text-gray-900 mb-2">:8084</div>
              <p className="text-sm text-gray-600">Ticket reservation and orders</p>
            </div>
          </div>
        </div>
      </div>

      {/* CTA */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-20 text-center">
        <h2 className="text-3xl font-bold text-gray-900 mb-4">
          Ready to Get Started?
        </h2>
        <p className="text-xl text-gray-600 mb-8">
          Sign up now and experience the future of ticket sales
        </p>
        <Link to="/signup" className="btn btn-primary text-lg px-8 py-3 inline-block">
          Create Account
        </Link>
      </div>
    </div>
  )
}

export default HomePage

