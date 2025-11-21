const express = require('express');
const router = express.Router();
const firestoreService = require('../services/firestoreService');

/**
 * GET /api/locations
 * Get all locations (user's own + public locations)
 */
router.get('/', async (req, res, next) => {
  try {
    const userId = req.userId;
    const locations = await firestoreService.getAllLocations(userId);
    res.json({ locations });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/locations/user
 * Get user's own locations
 */
router.get('/user', async (req, res, next) => {
  try {
    const userId = req.userId;
    const locations = await firestoreService.getUserLocations(userId);
    res.json({ locations });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/locations/filtered
 * Get filtered locations for user
 * Query params: visibility, startDate, endDate
 */
router.get('/filtered', async (req, res, next) => {
  try {
    const userId = req.userId;
    const { visibility, startDate, endDate } = req.query;
    
    const filters = {};
    if (visibility) filters.visibility = visibility;
    if (startDate) filters.startDate = parseInt(startDate);
    if (endDate) filters.endDate = parseInt(endDate);
    
    const locations = await firestoreService.getFilteredUserLocations(userId, filters);
    res.json({ locations });
  } catch (error) {
    next(error);
  }
});

/**
 * GET /api/locations/:id
 * Get location by ID
 */
router.get('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const location = await firestoreService.getLocationById(id);
    
    if (!location) {
      return res.status(404).json({
        error: {
          message: 'Location not found',
          status: 404
        }
      });
    }
    
    // Check if user has access (own location or public)
    if (location.userId !== req.userId && location.visibility !== 'public') {
      return res.status(403).json({
        error: {
          message: 'Access denied',
          status: 403
        }
      });
    }
    
    res.json({ location });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /api/locations
 * Create a new location
 */
router.post('/', async (req, res, next) => {
  try {
    const userId = req.userId;
    const locationData = {
      ...req.body,
      userId: userId,
      dateAdded: req.body.dateAdded || Date.now()
    };
    
    // Validate required fields
    if (!locationData.name || !locationData.latitude || !locationData.longitude) {
      return res.status(400).json({
        error: {
          message: 'Missing required fields: name, latitude, longitude',
          status: 400
        }
      });
    }
    
    const location = await firestoreService.createLocation(locationData);
    res.status(201).json({ location });
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /api/locations/:id
 * Update a location
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.userId;
    
    // Check if location exists and user owns it
    const existingLocation = await firestoreService.getLocationById(id);
    if (!existingLocation) {
      return res.status(404).json({
        error: {
          message: 'Location not found',
          status: 404
        }
      });
    }
    
    if (existingLocation.userId !== userId) {
      return res.status(403).json({
        error: {
          message: 'Access denied. You can only update your own locations',
          status: 403
        }
      });
    }
    
    const updateData = { ...req.body };
    // Don't allow changing userId
    delete updateData.userId;
    
    const location = await firestoreService.updateLocation(id, updateData);
    res.json({ location });
  } catch (error) {
    next(error);
  }
});

/**
 * DELETE /api/locations/:id
 * Delete a location
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.userId;
    
    // Check if location exists and user owns it
    const existingLocation = await firestoreService.getLocationById(id);
    if (!existingLocation) {
      return res.status(404).json({
        error: {
          message: 'Location not found',
          status: 404
        }
      });
    }
    
    if (existingLocation.userId !== userId) {
      return res.status(403).json({
        error: {
          message: 'Access denied. You can only delete your own locations',
          status: 403
        }
      });
    }
    
    await firestoreService.deleteLocation(id);
    res.json({ message: 'Location deleted successfully' });
  } catch (error) {
    next(error);
  }
});

module.exports = router;

