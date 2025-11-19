// This file is now corrected to match our backend ports and paths
export const API_BASE_URLS = {
    AUTH: 'http://localhost:8081/api/auth',
    INVENTORY: 'http://localhost:8082/api/inventory/events',
    WAITING_ROOM: 'http://localhost:8083/waiting-room',
    BOOKING: 'http://localhost:8084/api/bookings',
    PAYMENT: 'http://localhost:8085/api/payments' // This is a mock, but we'll leave it
}

export const API_ENDPOINTS = {
    // === Auth endpoints ===
    SIGNUP: `${API_BASE_URLS.AUTH}/signup`,
    LOGIN: `${API_BASE_URLS.AUTH}/login`,
    REFRESH_TOKEN: `${API_BASE_URLS.AUTH}/refresh-token`,
    LOGOUT: `${API_BASE_URLS.AUTH}/logout`,
    LOGOUT_ALL: (userId) => `${API_BASE_URLS.AUTH}/logout-all/${userId}`,
    VALIDATE_TOKEN: `${API_BASE_URLS.AUTH}/validate-token`,
    GET_USER: (userId) => `${API_BASE_URLS.AUTH}/user/${userId}`,
    RATE_LIMIT_INFO: (email) => `${API_BASE_URLS.AUTH}/rate-limit/${email}`,
    LOGIN_HISTORY: (userId) => `${API_BASE_URLS.AUTH}/login-history/${userId}`,
    ACTIVE_SESSIONS: (userId) => `${API_BASE_URLS.AUTH}/active-sessions/${userId}`,

    // === Inventory endpoints ===
    GET_EVENTS: `${API_BASE_URLS.INVENTORY}`,
    GET_EVENT: (eventId) => `${API_BASE_URLS.INVENTORY}/${eventId}`,
    CREATE_EVENT: `${API_BASE_URLS.INVENTORY}`,
    UPDATE_EVENT: (eventId) => `${API_BASE_URLS.INVENTORY}/${eventId}`,
    DELETE_EVENT: (eventId) => `${API_BASE_URLS.INVENTORY}/${eventId}`,
    // This endpoint is what our booking-service calls, not the frontend
    RESERVE_TICKETS: (eventId, quantity) => `${API_BASE_URLS.INVENTORY}/reserve?quantity=${quantity}`,

    // === Waiting Room endpoints ===
    JOIN_QUEUE: `${API_BASE_URLS.WAITING_ROOM}/join`,
    // --- ADDED THIS MISSING ENDPOINT ---
    GET_POSITION: (userId, eventId) => `${API_BASE_URLS.WAITING_ROOM}/position/${userId}?eventId=${eventId}`,
    GET_STATUS: (eventId) => `${API_BASE_URLS.WAITING_ROOM}/status?eventId=${eventId}`,
    // This endpoint doesn't exist in our controller, but we'll leave the definition
    LEAVE_QUEUE: `${API_BASE_URLS.WAITING_ROOM}/leave`,
    // --- ADDED THIS MISSING ENDPOINT ---
    ADMIT_BATCH: `${API_BASE_URLS.WAITING_ROOM}/admit`,

    // === Booking endpoints ===
    CREATE_BOOKING: `${API_BASE_URLS.BOOKING}`,
    GET_BOOKINGS: `${API_BASE_URLS.BOOKING}/user`, // We will need to build this
    GET_BOOKING: (bookingId) => `${API_BASE_URLS.BOOKING}/${bookingId}`,
    CONFIRM_BOOKING: (bookingId) => `${API_BASE_URLS.BOOKING}/${bookingId}/confirm`,
    CANCEL_BOOKING: (bookingId) => `${API_BASE_URLS.BOOKING}/${bookingId}/cancel`,
}