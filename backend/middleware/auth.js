const admin = require('firebase-admin');

// Firebase Admin SDK should already be initialized in server.js
// This middleware just uses it to verify tokens

/**
 * Middleware to verify Firebase Auth token
 * Extracts user ID from token and attaches to request
 */
const verifyToken = async (req, res, next) => {
  try {
    const authHeader = req.headers.authorization;
    
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({
        error: {
          message: 'No authorization token provided',
          status: 401
        }
      });
    }

    const token = authHeader.split('Bearer ')[1];
    
    // Verify the token
    const decodedToken = await admin.auth().verifyIdToken(token);
    
    // Attach user ID to request
    req.userId = decodedToken.uid;
    req.userEmail = decodedToken.email;
    
    next();
  } catch (error) {
    console.error('Token verification error:', error);
    return res.status(401).json({
      error: {
        message: 'Invalid or expired token',
        status: 401
      }
    });
  }
};

module.exports = verifyToken;

