const express = require('express');
const router = express.Router();
const firestoreService = require('../services/firestoreService');

/**
 * GET /api/notifications
 * Get user notifications
 * Query params: limit (default: 50)
 */
router.get('/', async (req, res, next) => {
  try {
    const userId = req.userId;
    const limit = parseInt(req.query.limit) || 50;
    
    const notifications = await firestoreService.getUserNotifications(userId, limit);
    res.json({ notifications });
  } catch (error) {
    next(error);
  }
});

/**
 * POST /api/notifications
 * Create a new notification
 */
router.post('/', async (req, res, next) => {
  try {
    const userId = req.userId;
    const notificationData = {
      ...req.body,
      userId: userId,
      timestamp: req.body.timestamp || Date.now(),
      isRead: req.body.isRead || false
    };
    
    // Validate required fields
    if (!notificationData.title || !notificationData.message) {
      return res.status(400).json({
        error: {
          message: 'Missing required fields: title, message',
          status: 400
        }
      });
    }
    
    const notification = await firestoreService.createNotification(notificationData);
    res.status(201).json({ notification });
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /api/notifications/:id
 * Update a notification (e.g., mark as read)
 */
router.put('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.userId;
    
    // Get notification to verify ownership
    const notifications = await firestoreService.getUserNotifications(userId, 1000);
    const notification = notifications.find(n => n.id === id);
    
    if (!notification) {
      return res.status(404).json({
        error: {
          message: 'Notification not found',
          status: 404
        }
      });
    }
    
    const updateData = { ...req.body };
    // Don't allow changing userId
    delete updateData.userId;
    
    const updatedNotification = await firestoreService.updateNotification(id, updateData);
    res.json({ notification: updatedNotification });
  } catch (error) {
    next(error);
  }
});

/**
 * PUT /api/notifications/mark-all-read
 * Mark all notifications as read for user
 */
router.put('/mark-all-read', async (req, res, next) => {
  try {
    const userId = req.userId;
    const count = await firestoreService.markAllNotificationsAsRead(userId);
    res.json({ message: `Marked ${count} notifications as read`, count });
  } catch (error) {
    next(error);
  }
});

/**
 * DELETE /api/notifications/:id
 * Delete a notification
 */
router.delete('/:id', async (req, res, next) => {
  try {
    const { id } = req.params;
    const userId = req.userId;
    
    // Get notification to verify ownership
    const notifications = await firestoreService.getUserNotifications(userId, 1000);
    const notification = notifications.find(n => n.id === id);
    
    if (!notification) {
      return res.status(404).json({
        error: {
          message: 'Notification not found',
          status: 404
        }
      });
    }
    
    await firestoreService.deleteNotification(id);
    res.json({ message: 'Notification deleted successfully' });
  } catch (error) {
    next(error);
  }
});

/**
 * DELETE /api/notifications
 * Delete all notifications for user
 */
router.delete('/', async (req, res, next) => {
  try {
    const userId = req.userId;
    const count = await firestoreService.deleteAllUserNotifications(userId);
    res.json({ message: `Deleted ${count} notifications`, count });
  } catch (error) {
    next(error);
  }
});

module.exports = router;

