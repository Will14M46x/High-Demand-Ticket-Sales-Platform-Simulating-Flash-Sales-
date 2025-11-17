# 🚀 Frontend Quick Start

## In 3 Steps

### 1. Install
```bash
cd frontend
npm install
```

### 2. Start
```bash
npm run dev
```

### 3. Open
```
http://localhost:3000
```

---

## First-Time Test (5 min)

1. **Sign Up** → Create account
2. **Create Event** → Name: "Test", Tickets: 10, Price: 50
3. **Book Tickets** → Select quantity → Complete booking
4. **View Bookings** → See your booking

---

## Pages

| URL | Description |
|-----|-------------|
| `/` | Home page |
| `/login` | Sign in |
| `/signup` | Create account |
| `/events` | Browse events |
| `/events/:id` | Event details |
| `/events/create` | Create event |
| `/my-bookings` | Your bookings |
| `/admin` | Dashboard |

---

## Common Commands

```bash
# Install dependencies
npm install

# Start dev server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint
```

---

## Troubleshooting

**CORS errors?**
→ Check backend services allow `http://localhost:3000`

**Can't connect to backend?**
→ Verify services running on ports 8081-8084

**Login fails?**
→ Clear localStorage and try again

**Port 3000 in use?**
→ Kill process or edit `vite.config.js`

---

## Quick Links

- **Full Setup Guide**: [FRONTEND_SETUP.md](../FRONTEND_SETUP.md)
- **Technical Docs**: [README.md](README.md)
- **Backend Docs**: [SETUP_COMPLETE.md](../SETUP_COMPLETE.md)

---

## Service Ports

| Service | Port |
|---------|------|
| **Frontend** | **3000** |
| Auth | 8081 |
| Inventory | 8082 |
| Waiting Room | 8083 |
| Booking | 8084 |

---

## Test Credentials

**Any email works!**
- Email: `test@example.com`
- Password: `Test123456` (min 6 chars)

**Card Numbers (test mode):**
- Any 16 digits work
- Example: `1234 5678 9012 3456`

---

## Need Help?

1. Check browser console
2. Verify backend services running
3. Read [README.md](README.md) troubleshooting
4. Check Network tab for API errors

---

**Ready in < 2 minutes!** 🎉

