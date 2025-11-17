import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { Clock, Users, Loader, CheckCircle } from 'lucide-react'
import { waitingRoomAPI } from '../services/api.service'

function WaitingRoomPage() {
  const { eventId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const quantity = location.state?.quantity || 1

  const [status, setStatus] = useState('joining') // joining, waiting, approved
  const [queuePosition, setQueuePosition] = useState(null)
  const [estimatedWait, setEstimatedWait] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    joinQueue()
    
    // Simulate queue progression
    const interval = setInterval(() => {
      checkQueueStatus()
    }, 2000)

    return () => clearInterval(interval)
  }, [])

  const joinQueue = async () => {
    try {
      setStatus('joining')
      await waitingRoomAPI.joinQueue()
      setStatus('waiting')
      setQueuePosition(Math.floor(Math.random() * 50) + 1)
      setEstimatedWait(Math.floor(Math.random() * 180) + 30) // 30-210 seconds
    } catch (err) {
      console.error('Error joining queue:', err)
      setError('Failed to join queue. You may proceed directly.')
      // Allow proceeding even if waiting room service is down
      setTimeout(() => {
        navigate(`/booking/${eventId}`, { state: { quantity } })
      }, 2000)
    }
  }

  const checkQueueStatus = () => {
    if (status === 'waiting' && queuePosition !== null) {
      // Simulate queue progression
      const newPosition = Math.max(0, queuePosition - Math.floor(Math.random() * 3) - 1)
      setQueuePosition(newPosition)
      
      if (newPosition === 0) {
        setStatus('approved')
        setTimeout(() => {
          navigate(`/booking/${eventId}`, { state: { quantity } })
        }, 2000)
      } else {
        // Update estimated wait time
        setEstimatedWait(Math.max(10, estimatedWait - Math.floor(Math.random() * 15) - 5))
      }
    }
  }

  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins}:${secs.toString().padStart(2, '0')}`
  }

  return (
    <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-b from-primary-50 to-white">
      <div className="max-w-md w-full">
        <div className="card text-center">
          {/* Animated Icon */}
          <div className="mb-8">
            {status === 'joining' && (
              <div className="animate-spin rounded-full h-20 w-20 border-b-4 border-primary-600 mx-auto"></div>
            )}
            {status === 'waiting' && (
              <div className="relative">
                <div className="animate-pulse bg-primary-100 w-20 h-20 rounded-full mx-auto flex items-center justify-center">
                  <Clock className="h-10 w-10 text-primary-600" />
                </div>
                <div className="absolute -top-2 -right-2 bg-primary-600 text-white rounded-full w-10 h-10 flex items-center justify-center text-sm font-bold">
                  {queuePosition}
                </div>
              </div>
            )}
            {status === 'approved' && (
              <div className="bg-green-100 w-20 h-20 rounded-full mx-auto flex items-center justify-center">
                <CheckCircle className="h-10 w-10 text-green-600" />
              </div>
            )}
          </div>

          {/* Status Messages */}
          {status === 'joining' && (
            <>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                Joining Queue
              </h2>
              <p className="text-gray-600">
                Please wait while we add you to the waiting room...
              </p>
            </>
          )}

          {status === 'waiting' && (
            <>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">
                You're in the Queue
              </h2>
              <p className="text-gray-600 mb-8">
                Please stay on this page. You'll be automatically redirected when it's your turn.
              </p>

              {/* Queue Stats */}
              <div className="grid grid-cols-2 gap-4 mb-8">
                <div className="bg-primary-50 rounded-lg p-4">
                  <Users className="h-6 w-6 text-primary-600 mx-auto mb-2" />
                  <div className="text-2xl font-bold text-primary-600">{queuePosition}</div>
                  <div className="text-sm text-gray-600">Position in Queue</div>
                </div>
                <div className="bg-primary-50 rounded-lg p-4">
                  <Clock className="h-6 w-6 text-primary-600 mx-auto mb-2" />
                  <div className="text-2xl font-bold text-primary-600">{formatTime(estimatedWait)}</div>
                  <div className="text-sm text-gray-600">Estimated Wait</div>
                </div>
              </div>

              {/* Progress Bar */}
              <div className="mb-6">
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div
                    className="bg-primary-600 h-2 rounded-full transition-all duration-500"
                    style={{ width: `${Math.max(10, 100 - (queuePosition * 2))}%` }}
                  />
                </div>
              </div>

              {/* Info */}
              <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-sm text-blue-800 text-left">
                <strong>What's happening?</strong>
                <ul className="mt-2 space-y-1">
                  <li>• You're in a virtual waiting room</li>
                  <li>• This ensures fair access during high demand</li>
                  <li>• Keep this page open to maintain your position</li>
                  <li>• You'll be redirected automatically</li>
                </ul>
              </div>
            </>
          )}

          {status === 'approved' && (
            <>
              <h2 className="text-2xl font-bold text-green-600 mb-2">
                You're In!
              </h2>
              <p className="text-gray-600 mb-4">
                Redirecting you to complete your booking...
              </p>
              <div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
                <Loader className="h-4 w-4 animate-spin" />
                <span>Please wait...</span>
              </div>
            </>
          )}

          {error && (
            <div className="mt-4 text-sm text-yellow-700 bg-yellow-50 border border-yellow-200 rounded-lg p-3">
              {error}
            </div>
          )}
        </div>

        {/* Fun Fact */}
        <div className="mt-6 text-center text-sm text-gray-500">
          <p className="font-medium mb-1">💡 Did you know?</p>
          <p>
            This waiting room system can handle thousands of concurrent users
            while preventing ticket overselling
          </p>
        </div>
      </div>
    </div>
  )
}

export default WaitingRoomPage

