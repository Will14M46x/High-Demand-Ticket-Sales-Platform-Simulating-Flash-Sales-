import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/events': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/waiting-room': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/bookings': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/payments': {
        target: 'http://localhost:8085',
        changeOrigin: true,
      },
    }
  }
})

