# Ticket Sales Platform - Frontend

A modern, responsive frontend application for the High-Demand Ticket Sales Platform. Built with React, Vite, and Tailwind CSS to provide an intuitive interface for testing all microservices.

## 🎯 Purpose

This frontend makes it **easy to test** all the backend microservices by providing:
- ✅ Clean, intuitive UI for all operations
- ✅ Visual feedback for waiting room and booking flows
- ✅ Real-time service interaction
- ✅ Complete authentication flow testing
- ✅ Admin dashboard for monitoring

## 🚀 Quick Start

### Prerequisites

- **Node.js 18+** (download from [nodejs.org](https://nodejs.org/))
- **npm** or **yarn** package manager
- Backend services running (see main README)

### Installation

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The app will be available at **http://localhost:3000**

### First Time Setup

1. Make sure all backend services are running:
   - Auth Service: http://localhost:8081
   - Inventory Service: http://localhost:8082
   - Waiting Room: http://localhost:8083
   - Booking Service: http://localhost:8084

2. Open http://localhost:3000 in your browser

3. Create a test account (any email, min 6 chars password)

4. Start creating events and testing bookings!

## 📱 Features

### Authentication Flow
- **Sign Up** - Create new user accounts
- **Login** - Authenticate existing users
- **JWT Tokens** - Automatic token management
- **Protected Routes** - Secure pages require authentication

### Event Management
- **Browse Events** - View all available events
- **Event Details** - Detailed event information
- **Create Events** - Admin can create new events
- **Search & Filter** - Find events by name or location

### Booking Flow
1. **Select Event** - Choose an event and quantity
2. **Waiting Room** - Virtual queue simulation
3. **Booking** - Complete purchase with payment
4. **Confirmation** - View booking details

### Admin Dashboard
- **Statistics** - Real-time platform metrics
- **Event Management** - Monitor all events
- **Service Status** - Check microservice health

## 🧪 Testing Guide

### Complete Testing Flow

#### 1. Test Authentication Service (Port 8081)

```bash
# Sign up a new user
1. Click "Sign Up" in navigation
2. Fill in the form:
   - Name: Test User
   - Email: test@example.com
   - Password: Test123456
3. Submit form
4. You should be redirected to Events page

# Test login
1. Logout (button in header)
2. Click "Login"
3. Enter credentials
4. Verify successful login
```

#### 2. Test Inventory Service (Port 8082)

```bash
# Create an event
1. Navigate to Admin Dashboard or click "Create Event"
2. Fill in event details:
   - Name: Test Concert
   - Location: Arena
   - Date: Future date
   - Tickets: 10 (low number for easy testing)
   - Price: 50.00
3. Submit form
4. Event should appear in Events list

# Browse events
1. Navigate to "Events" page
2. Search for events
3. Click on an event to view details
```

#### 3. Test Waiting Room Service (Port 8083)

```bash
# Join waiting room
1. Click on an event
2. Select quantity (1-10 tickets)
3. Click "Proceed to Booking"
4. Watch the waiting room animation:
   - Queue position updates
   - Estimated wait time decreases
   - Auto-redirect when approved
```

#### 4. Test Booking Service (Port 8084)

```bash
# Complete booking
1. After waiting room, fill payment form:
   - Cardholder Name: Test User
   - Card Number: 1234 5678 9012 3456 (any number works)
   - Expiry: 12/25
   - CVV: 123
2. Click "Pay"
3. Booking is created and tickets reserved
4. View confirmation

# View bookings
1. Navigate to "My Bookings"
2. See all your bookings
3. Check booking details
```

### Testing Scenarios

#### Scenario 1: High-Demand Flash Sale

```bash
# Simulate sold-out scenario
1. Create event with only 5 tickets
2. Open multiple browser tabs
3. Try to book all tickets
4. Verify no overselling occurs
5. Check "Sold Out" badge appears
```

#### Scenario 2: Concurrent Bookings

```bash
# Test race conditions
1. Create event with 10 tickets
2. Open 3-5 browser windows
3. Simultaneously book tickets from each
4. Verify inventory is correctly decremented
5. Check admin dashboard for accurate stats
```

#### Scenario 3: Waiting Room Load

```bash
# Test queue management
1. Create popular event
2. Join waiting room from multiple tabs
3. Observe queue positions
4. Verify fair ordering
```

#### Scenario 4: Authentication & Authorization

```bash
# Test protected routes
1. Logout completely
2. Try accessing /admin directly
3. Should redirect to login
4. Login and verify access granted

# Test token persistence
1. Login
2. Refresh page
3. Should remain logged in
4. Close browser and reopen
5. May need to login again (depends on localStorage)
```

## 📂 Project Structure

```
frontend/
├── public/                 # Static assets
├── src/
│   ├── components/        # Reusable components
│   │   └── Layout.jsx     # Main layout with header/footer
│   ├── config/            # Configuration files
│   │   └── api.js         # API endpoints configuration
│   ├── pages/             # Page components
│   │   ├── HomePage.jsx           # Landing page
│   │   ├── LoginPage.jsx          # Login form
│   │   ├── SignupPage.jsx         # Registration form
│   │   ├── EventsPage.jsx         # Event listings
│   │   ├── EventDetailPage.jsx    # Event details
│   │   ├── CreateEventPage.jsx    # Create event form
│   │   ├── WaitingRoomPage.jsx    # Queue simulation
│   │   ├── BookingPage.jsx        # Booking & payment
│   │   ├── MyBookingsPage.jsx     # User bookings
│   │   └── AdminPage.jsx          # Admin dashboard
│   ├── services/          # API service layer
│   │   └── api.service.js # API client with axios
│   ├── store/             # State management
│   │   └── useAuthStore.js # Auth state with Zustand
│   ├── App.jsx            # Main app component & routes
│   ├── main.jsx           # App entry point
│   └── index.css          # Global styles (Tailwind)
├── index.html             # HTML template
├── package.json           # Dependencies
├── vite.config.js         # Vite configuration
└── tailwind.config.js     # Tailwind CSS config
```

## 🎨 UI Components

### Styling System
- **Tailwind CSS** - Utility-first CSS framework
- **Custom Components** - Reusable button, card, badge classes
- **Responsive Design** - Mobile-first approach
- **Icons** - Lucide React icon library

### Color Scheme
- **Primary**: Blue (#3b82f6) - Actions, links
- **Success**: Green - Confirmed bookings
- **Warning**: Yellow - Low stock, pending
- **Danger**: Red - Sold out, errors

## 🔧 Configuration

### API Endpoints

Edit `src/config/api.js` to change service URLs:

```javascript
export const API_BASE_URLS = {
  AUTH: 'http://localhost:8081/api/auth',
  INVENTORY: 'http://localhost:8082/api/events',
  WAITING_ROOM: 'http://localhost:8083/waiting-room',
  BOOKING: 'http://localhost:8084/api/bookings',
}
```

### Environment Variables (Optional)

Create `.env` file in frontend directory:

```bash
VITE_AUTH_SERVICE_URL=http://localhost:8081
VITE_INVENTORY_SERVICE_URL=http://localhost:8082
VITE_WAITING_ROOM_URL=http://localhost:8083
VITE_BOOKING_SERVICE_URL=http://localhost:8084
```

## 🛠️ Development

### Available Scripts

```bash
# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

### Adding New Features

1. **New Page**: Create component in `src/pages/`
2. **Add Route**: Update `src/App.jsx`
3. **API Calls**: Add to `src/services/api.service.js`
4. **State**: Use `useAuthStore` or create new store

## 🐛 Troubleshooting

### Common Issues

**Issue**: CORS errors in browser console
```bash
Solution: Check backend services have CORS enabled
         Auth service should allow http://localhost:3000
```

**Issue**: Cannot connect to backend services
```bash
Solution: 
1. Verify all services are running
2. Check ports match in api.js config
3. Test services with curl:
   curl http://localhost:8081/api/auth/health
```

**Issue**: Login/Signup fails with 401
```bash
Solution:
1. Check JWT_SECRET is set in auth-service
2. Verify Firebase is configured correctly
3. Check browser console for detailed errors
```

**Issue**: Events not loading
```bash
Solution:
1. Check Inventory Service is running (port 8082)
2. Verify JWT token is valid (check localStorage)
3. Check browser Network tab for failed requests
```

**Issue**: Waiting room skips immediately
```bash
Solution: This is expected if Waiting Room service is down
         The app gracefully degrades and proceeds to booking
```

## 📊 Testing Checklist

Use this checklist to verify all features:

### Authentication
- [ ] Sign up with new account
- [ ] Login with existing account
- [ ] Logout successfully
- [ ] Protected routes redirect to login
- [ ] Token persists across page refreshes

### Events
- [ ] View all events
- [ ] Search events
- [ ] View event details
- [ ] Create new event (requires login)
- [ ] Events display correct availability

### Booking Flow
- [ ] Select event and quantity
- [ ] Join waiting room
- [ ] Queue position updates
- [ ] Proceed to booking
- [ ] Complete payment form
- [ ] Receive confirmation

### Admin
- [ ] View dashboard statistics
- [ ] See recent events
- [ ] Check service status
- [ ] Access analytics

### Edge Cases
- [ ] Try booking sold-out event
- [ ] Test with low inventory (1-2 tickets)
- [ ] Multiple concurrent bookings
- [ ] Network error handling
- [ ] Invalid form submissions

## 🎓 Learning Resources

### Technologies Used
- [React 18](https://react.dev/) - UI library
- [Vite](https://vitejs.dev/) - Build tool
- [Tailwind CSS](https://tailwindcss.com/) - CSS framework
- [React Router](https://reactrouter.com/) - Routing
- [Axios](https://axios-http.com/) - HTTP client
- [Zustand](https://github.com/pmndrs/zustand) - State management
- [date-fns](https://date-fns.org/) - Date formatting
- [Lucide React](https://lucide.dev/) - Icons

## 📈 Performance

### Optimization Tips
- Components lazy load on route change
- API calls include proper error handling
- Forms validate before submission
- Images and assets are optimized

### Load Testing
To test with high load:
1. Use multiple browser tabs (5-10)
2. Simultaneously book tickets
3. Monitor browser Network tab
4. Check backend service logs

## 🤝 Contributing

This frontend is designed to be easily extended:

1. **Add new service**: Update `src/config/api.js`
2. **New feature**: Create component in appropriate folder
3. **Styling**: Follow Tailwind utility classes
4. **State**: Use Zustand for global state

## 📝 Notes

### Mock Features
Some features use mock data for demo purposes:
- Payment processing (no real charges)
- Queue simulation (simulated waiting)
- Demo bookings (if service unavailable)

### Production Considerations
Before deploying to production:
- [ ] Update API URLs to production endpoints
- [ ] Enable proper authentication
- [ ] Configure real payment gateway
- [ ] Add error tracking (e.g., Sentry)
- [ ] Set up analytics
- [ ] Add proper logging
- [ ] Configure HTTPS
- [ ] Implement proper session management

## 🎉 Success Metrics

Your testing is successful if:
1. ✅ Users can sign up and login
2. ✅ Events can be created and viewed
3. ✅ Bookings complete without errors
4. ✅ No tickets are oversold
5. ✅ UI is responsive and intuitive
6. ✅ All services integrate smoothly

## 📞 Support

For issues or questions:
1. Check troubleshooting section above
2. Review browser console for errors
3. Check backend service logs
4. Verify all services are running
5. Test with curl to isolate issues

---

**Built with ❤️ for the High-Demand Ticket Sales Platform**

**University of Limerick** - Distributed Systems Project

Last Updated: November 2024

