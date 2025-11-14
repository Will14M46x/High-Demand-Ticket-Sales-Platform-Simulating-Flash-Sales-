# 🧪 Testing the WOW Features - Quick Guide

## Prerequisites

### 1. Install and Start Redis

**Windows:**
```powershell
# Download from: https://github.com/microsoftarchive/redis/releases
# Or use WSL:
wsl
sudo apt-get install redis-server
redis-server
```

**Mac:**
```bash
brew install redis
redis-server
```

**Linux:**
```bash
sudo apt-get install redis-server
redis-server
```

**Verify Redis is running:**
```bash
redis-cli ping
# Should return: PONG
```

### 2. Set JAVA_HOME (if not already set)
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
```

### 3. Start Auth Service
```powershell
cd auth-service
..\mvnw.cmd clean install
..\mvnw.cmd spring-boot:run
```

Wait for:
```
✅ Firebase initialized successfully!
Started AuthServiceApplication in X seconds
```

### 4. Start Frontend
```powershell
cd frontend
npm run dev
```

---

## Test 1: Rate Limiting 🛡️

### Goal: Test brute force protection

**Step 1: Try logging in with wrong password**
1. Open browser: `http://localhost:5173/login`
2. Enter email: `test@example.com`
3. Enter wrong password: `wrongpass123`
4. Click "Sign In"

**Expected:**
- ❌ Error message appears
- ⚠️ Should show something like "Invalid email or password"

**Step 2: Repeat 4 more times (5 failures total)**

After the 5th attempt:
- 🔒 **Account should be LOCKED**
- ⚠️ Error message: "Account locked due to multiple failed login attempts. Try again in 15 minutes."

**Step 3: Check rate limit status via API**
```bash
curl http://localhost:8081/api/auth/rate-limit/test@example.com
```

**Expected Response:**
```json
{
  "isLockedOut": true,
  "remainingAttempts": 0,
  "maxAttempts": 5,
  "lockoutRemainingSeconds": 900,
  "message": "Account locked. Try again in 15 minutes."
}
```

**Step 4: Wait or manually unlock (for testing)**
```bash
# To unlock immediately (Redis CLI):
redis-cli DEL "lockout:test@example.com"
redis-cli DEL "attempt:test@example.com"

# Or wait 15 minutes for auto-unlock
```

---

## Test 2: Successful Login & Refresh Tokens 🔄

### Goal: Test login flow and automatic token refresh

**Step 1: Create a new account**
1. Go to: `http://localhost:5173/signup`
2. Fill in:
   - Name: Test User
   - Email: testuser@example.com
   - Password: testpass123
   - Confirm Password: testpass123
3. Click "Sign Up"

**Expected:**
- ✅ Account created
- ✅ Automatically logged in
- ✅ Redirected to Events page

**Step 2: Check browser's Local Storage**
1. Open DevTools (F12)
2. Go to Application tab (Chrome) or Storage tab (Firefox)
3. Check Local Storage

**Expected to see:**
```
token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...  (JWT access token)
refreshToken: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx  (Refresh token)
user: {"id":1,"email":"testuser@example.com","name":"Test User"}
```

**Step 3: Test manual refresh token call**
```bash
# Copy your refreshToken from browser Local Storage
curl -X POST http://localhost:8081/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"YOUR_REFRESH_TOKEN_HERE"}'
```

**Expected Response:**
```json
{
  "token": "NEW_ACCESS_TOKEN",
  "refreshToken": "NEW_REFRESH_TOKEN",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "testuser@example.com",
  "name": "Test User",
  "firebaseUid": "...",
  "expiresIn": 86400
}
```

**Note:** The refresh token was rotated! Old one is now invalid.

---

## Test 3: Login History & Device Tracking 📜

### Goal: See login history with device information

**Step 1: Login from multiple browsers**
1. Login using Chrome
2. Login using Firefox (or Edge)
3. Login using your phone's browser (optional)

**Step 2: View login history via API**
```bash
# Replace 1 with your actual userId
curl http://localhost:8081/api/auth/login-history/1
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "ipAddress": "127.0.0.1",
    "deviceType": "Desktop",
    "browser": "Chrome",
    "operatingSystem": "Windows",
    "city": null,
    "country": null,
    "loggedInAt": "2024-11-12T10:30:00",
    "suspicious": false,
    "suspiciousReason": null
  },
  {
    "id": 2,
    "ipAddress": "127.0.0.1",
    "deviceType": "Desktop",
    "browser": "Firefox",
    "operatingSystem": "Windows",
    "loggedInAt": "2024-11-12T10:35:00",
    "suspicious": false,
    "suspiciousReason": null
  }
]
```

**Step 3: View active sessions**
```bash
curl http://localhost:8081/api/auth/active-sessions/1
```

**Expected Response:**
```json
[
  {
    "id": 1,
    "deviceInfo": "Mozilla/5.0 (Windows NT 10.0...",
    "ipAddress": "127.0.0.1",
    "createdAt": "2024-11-12T10:30:00",
    "expiresAt": "2024-12-12T10:30:00",
    "isCurrent": false
  },
  {
    "id": 2,
    "deviceInfo": "Mozilla/5.0 (Windows NT 10.0...",
    "ipAddress": "127.0.0.1",
    "createdAt": "2024-11-12T10:35:00",
    "expiresAt": "2024-12-12T10:35:00",
    "isCurrent": false
  }
]
```

---

## Test 4: Logout & Session Management 🚪

### Goal: Test logout from single device and all devices

**Step 1: Logout from current device**
1. Click "Logout" in the UI
2. Check Local Storage - all items should be cleared
3. Try accessing protected route - should redirect to login

**Step 2: Login again**
- Login with same credentials

**Step 3: Logout from all devices (API)**
```bash
# Replace 1 with your userId
curl -X POST http://localhost:8081/api/auth/logout-all/1
```

**Expected:**
- ✅ All refresh tokens revoked
- ✅ User must login again on all devices

**Step 4: Try using old refresh token**
```bash
# Use an old refresh token
curl -X POST http://localhost:8081/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"OLD_REVOKED_TOKEN"}'
```

**Expected:**
- ❌ 401 Unauthorized
- ❌ Error: "Invalid or expired refresh token"

---

## Test 5: Automatic Token Refresh (Frontend) 🔄

### Goal: Verify frontend automatically refreshes expired tokens

**Step 1: Shorten token expiration (for testing)**

Edit `auth-service/src/main/resources/application.properties`:
```properties
# Change from 86400000 (24 hours) to 60000 (1 minute)
jwt.expiration=60000
```

Restart auth service.

**Step 2: Login**
- Login to the frontend

**Step 3: Wait 1 minute**
- Keep the tab open
- Don't interact with the page

**Step 4: After 1 minute, navigate to Events page**
- Click on "Events" link

**Expected:**
- ⏱️ First request fails with 401 (token expired)
- 🔄 Frontend automatically calls `/refresh-token`
- ✅ New access token obtained
- ✅ Original request retried with new token
- ✅ Events page loads successfully
- 🎉 **User never sees an error!**

**Check browser console:**
```
Token expired, refreshing...
Token refreshed successfully
Retrying original request...
```

---

## Test 6: Suspicious Activity Detection 🚨

### Goal: Test automatic flagging of suspicious logins

**Scenario 1: New IP Address**

1. Login from your normal IP
2. Login from VPN or different network
3. Check login history

**Expected:**
```json
{
  "id": 3,
  "ipAddress": "123.456.789.012",
  "suspicious": true,
  "suspiciousReason": "Login from new IP address"
}
```

**Scenario 2: Different Country (if you can simulate)**

1. Login from Country A
2. Within 30 minutes, login from Country B (VPN)
3. Check login history

**Expected:**
```json
{
  "suspicious": true,
  "suspiciousReason": "Login from different country within 30 minutes"
}
```

---

## Test 7: Database Verification 🗄️

### Goal: Verify data is being stored correctly

**H2 Console (In-Memory Database):**

1. Open: `http://localhost:8081/h2-console`
2. JDBC URL: `jdbc:h2:mem:auth_db`
3. Username: `sa`
4. Password: (leave empty)
5. Click "Connect"

**Check Tables:**

```sql
-- View all users
SELECT * FROM users;

-- View all refresh tokens
SELECT * FROM refresh_tokens;

-- View all login attempts
SELECT * FROM login_attempts ORDER BY attempted_at DESC LIMIT 10;

-- View login history
SELECT * FROM login_history ORDER BY logged_in_at DESC LIMIT 10;

-- Count active sessions per user
SELECT u.email, COUNT(*) as active_sessions
FROM refresh_tokens rt
JOIN users u ON rt.user_id = u.id
WHERE rt.is_revoked = false
  AND rt.expires_at > NOW()
GROUP BY u.email;

-- Failed login attempts by email
SELECT email, COUNT(*) as failures
FROM login_attempts
WHERE successful = false
  AND attempted_at > DATEADD('HOUR', -1, NOW())
GROUP BY email
ORDER BY failures DESC;
```

---

## Test 8: Redis Verification 🔴

### Goal: Verify Redis is storing rate limit data

**Check Redis keys:**

```bash
# List all rate limit keys
redis-cli KEYS "lockout:*"
redis-cli KEYS "attempt:*"

# Check specific email's data
redis-cli GET "attempt:test@example.com"
redis-cli TTL "attempt:test@example.com"

# Check if locked out
redis-cli EXISTS "lockout:test@example.com"
redis-cli TTL "lockout:test@example.com"

# View all keys (debug only)
redis-cli KEYS "*"

# Flush all (DANGER: deletes everything)
# redis-cli FLUSHALL
```

---

## Common Issues & Solutions 🔧

### Issue: "Cannot connect to Redis"
**Solution:**
```bash
# Check if Redis is running
redis-cli ping

# If not running, start it
redis-server
```

### Issue: "Account locked and won't unlock"
**Solution:**
```bash
# Manually clear Redis lockout
redis-cli DEL "lockout:YOUR_EMAIL"
redis-cli DEL "attempt:YOUR_EMAIL"
```

### Issue: "Refresh token not working"
**Solution:**
- Check browser Local Storage has both `token` and `refreshToken`
- Check backend logs for errors
- Try logout and login again

### Issue: "Login history shows 'Unknown' devices"
**Solution:**
- This is normal if User-Agent header is missing
- Frontend should automatically send User-Agent
- Check browser DevTools > Network tab > Request Headers

### Issue: "Firebase errors during login"
**Solution:**
- Ensure Firebase is configured correctly
- Check `firebase.enabled=true` in application.properties
- Verify Firebase service account JSON is in place

---

## Performance Testing 📊

### Test Concurrent Logins

```bash
# Use Apache Bench or wrk
ab -n 100 -c 10 \
   -p login.json \
   -T "application/json" \
   http://localhost:8081/api/auth/login

# Where login.json contains:
# {"email":"test@example.com","password":"testpass123"}
```

### Test Refresh Token Performance

```bash
ab -n 100 -c 10 \
   -p refresh.json \
   -T "application/json" \
   http://localhost:8081/api/auth/refresh-token
```

---

## Success Criteria ✅

After all tests, you should have:

- ✅ Rate limiting working (lockout after 5 failures)
- ✅ Refresh tokens being created and stored
- ✅ Automatic token refresh on frontend
- ✅ Login history being recorded with device info
- ✅ Logout properly revoking tokens
- ✅ Suspicious logins being flagged
- ✅ All data persisted in database
- ✅ Redis keys being set and expiring correctly

---

## Next Steps 🚀

1. **Build a Login History UI page** - Show users their recent activity
2. **Add email notifications** - Alert on suspicious logins
3. **Create admin dashboard** - View all login attempts, unlock accounts
4. **Add device naming** - Let users name their devices
5. **Implement 2FA** - Two-factor authentication for extra security

---

## Monitoring & Analytics 📈

### Redis Monitoring
```bash
# Monitor Redis commands in real-time
redis-cli MONITOR

# Get Redis info
redis-cli INFO

# Check memory usage
redis-cli INFO memory
```

### Application Logs
```bash
# Watch logs for login attempts
tail -f logs/spring.log | grep "Login request"

# Watch for rate limit events
tail -f logs/spring.log | grep "locked"

# Watch for token refresh
tail -f logs/spring.log | grep "Token refreshed"
```

---

**🎉 Happy Testing!**

If everything works, you now have a production-grade authentication system with:
- 🛡️ Advanced security (rate limiting)
- 🔄 Seamless UX (refresh tokens)
- 📜 Complete audit trail (login history)

This is enterprise-level authentication! 🚀

