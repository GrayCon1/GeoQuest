const express = require('express');
const cors = require('cors');
require('dotenv').config();

// Initialize Firebase Admin SDK FIRST before importing routes
const admin = require('firebase-admin');
if (!admin.apps.length) {
  const serviceAccount = require(process.env.FIREBASE_SERVICE_ACCOUNT_PATH || './geoquest-7d94f-firebase-adminsdk-fbsvc-cc3c7ccbf2.json');
  
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount)
  });
  console.log('Firebase Admin SDK initialized');
}

const locationsRoutes = require('./routes/locations');
const notificationsRoutes = require('./routes/notifications');
const usersRoutes = require('./routes/users');
const authMiddleware = require('./middleware/auth');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors({
  origin: process.env.CORS_ORIGIN || '*',
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'ok', message: 'GeoQuest API is running' });
});

// API Routes
app.use('/api/locations', authMiddleware, locationsRoutes);
app.use('/api/notifications', authMiddleware, notificationsRoutes);
app.use('/api/users', authMiddleware, usersRoutes);

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    error: {
      message: err.message || 'Internal server error',
      status: err.status || 500
    }
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    error: {
      message: 'Route not found',
      status: 404
    }
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`GeoQuest API server running on port ${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}`);
});

module.exports = app;

