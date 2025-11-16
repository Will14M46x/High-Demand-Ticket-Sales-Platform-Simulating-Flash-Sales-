import { useState, useEffect } from 'react'
import { useParams, useNavigate, useLocation } from 'react-router-dom'
import { Clock, Users, Loader, CheckCircle } from 'lucide-react'
import { waitingRoomAPI } from '../services/api.service'
import useAuthStore from '../store/useAuthStore'

function WaitingRoomPage() {
    const { eventId } = useParams()
    const navigate = useNavigate()
    const location = useLocation()
    const { user } = useAuthStore() // Get the logged-in user
    const quantity = location.state?.quantity || 1

    const [status, setStatus] = useState('joining') // joining, waiting, approved
    const [queuePosition, setQueuePosition] = useState(null)
    const [estimatedWait, setEstimatedWait] = useState(null)
    const [error, setError] = useState(null)

    useEffect(() => {
        // 1. Join the queue as soon as the page loads
        const join = async () => {
            if (!user) {
                navigate('/login'); // Should be handled by ProtectedRoute, but as a fallback
                return;
            }

            try {
                setStatus('joining');
                const joinData = {
                    userId: String(user.id), // Use the real user ID from the auth store
                    eventId: parseInt(eventId),
                    requestedQuantity: quantity,
                };
                // Call the real API
                const response = await waitingRoomAPI.joinQueue(joinData);

                if (response.position === 0) {
                    // Already admitted (e.g., queue is empty)
                    setStatus('approved');
                    setTimeout(() => {
                        navigate(`/booking/${eventId}`, { state: { quantity } });
                    }, 2000);
                } else {
                    // We are in the queue
                    setQueuePosition(response.position);
                    setEstimatedWait(response.estimatedWaitTime);
                    setStatus('waiting');
                }
            } catch (err) {
                console.error('Error joining queue:', err);
                setError('Failed to join queue. Please try again later.');
            }
        };
        join();
    }, [eventId, user, quantity, navigate]);

    useEffect(() => {
        // 2. Start polling for position if we are waiting
        if (status !== 'waiting') return;

        // Poll every 5 seconds
        const pollInterval = setInterval(async () => {
            try {
                // Call the real API to get our position
                const response = await waitingRoomAPI.getPosition(String(user.id), eventId);

                if (response.position === 0) {
                    // We are admitted!
                    setStatus('approved');
                    clearInterval(pollInterval); // Stop polling
                    setTimeout(() => {
                        // Navigate to the real booking page
                        navigate(`/booking/${eventId}`, { state: { quantity } });
                    }, 2000);
                } else {
                    // Still waiting, update position
                    setQueuePosition(response.position);
                    setEstimatedWait(response.estimatedWaitTime);
                }
            } catch (err) {
                // 404 means user is not in queue (maybe admitted and removed?)
                if (err.response?.status === 404) {
                    setStatus('approved');
                    clearInterval(pollInterval);
                    setTimeout(() => {
                        navigate(`/booking/${eventId}`, { state: { quantity } });
                    }, 2000);
                } else {
                    console.error('Error polling queue position:', err);
                    setError('Error checking queue status. Please hold on.');
                }
            }
        }, 5000);

        return () => clearInterval(pollInterval); // Cleanup on unmount
    }, [status, eventId, user.id, navigate, quantity]);


    const formatTime = (timeString) => {
        // The backend returns a string like "30 seconds"
        return timeString || 'Calculating...';
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
                                {queuePosition && (
                                    <div className="absolute -top-2 -right-2 bg-primary-600 text-white rounded-full w-10 h-10 flex items-center justify-center text-sm font-bold">
                                        {queuePosition}
                                    </div>
                                )}
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
                                    <div className="text-2xl font-bold text-primary-600">{queuePosition || '...'}</div>
                                    <div className="text-sm text-gray-600">Position in Queue</div>
                                </div>
                                <div className="bg-primary-50 rounded-lg p-4">
                                    <Clock className="h-6 w-6 text-primary-600 mx-auto mb-2" />
                                    <div className="text-2xl font-bold text-primary-600">{formatTime(estimatedWait)}</div>
                                    <div className="text-sm text-gray-600">Estimated Wait</div>
                                </div>
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
                        <div className="mt-4 text-sm text-red-700 bg-red-50 border border-red-200 rounded-lg p-3">
                            {error}
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export default WaitingRoomPage