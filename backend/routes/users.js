const express = require('express');
const router = express.Router();
const firestoreService = require('../services/firestoreService');

/**
 * GET /api/users/me
 * Get current user profile
 */
router.get('/me', async (req, res, next) => {
  try {
    const userId = req.userId;
    const user = await firestoreService.getUserProfile(userId);
    
    if (!user) {
      return res.status(404).json({
        error: {
          message: 'User profile not found',
          status: 404
        }
      });
    }
    
    res.json({ user });
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /api/users/me
 * Update current user profile
 */
router.put('/me', async (req, res, next) => {
  try {
    const userId = req.userId;
    const updateData = { ...req.body };
    
    // Don't allow changing user ID
    delete updateData.id;
    
    const user = await firestoreService.updateUserProfile(userId, updateData);
    res.json({ user });
  } catch (error) {
    next(error);
  }
});

module.exports = router;

