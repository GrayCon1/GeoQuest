const admin = require('firebase-admin');
const db = admin.firestore();

/**
 * Firestore service layer for database operations
 */
class FirestoreService {
  
  /**
   * Get all locations for a user
   */
  async getUserLocations(userId) {
    try {
      const snapshot = await db.collection('locations')
        .where('userId', '==', userId)
        .orderBy('dateAdded', 'desc')
        .get();
      
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    } catch (error) {
      console.error('Error getting user locations:', error);
      throw error;
    }
  }

  /**
   * Get all locations (public or user's own)
   */
  async getAllLocations(userId) {
    try {
      // Get user's locations
      const userLocations = await this.getUserLocations(userId);
      
      // Get public locations
      const publicSnapshot = await db.collection('locations')
        .where('visibility', '==', 'public')
        .orderBy('dateAdded', 'desc')
        .get();
      
      const publicLocations = publicSnapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
      
      // Combine and remove duplicates
      const allLocations = [...userLocations];
      const publicIds = new Set(userLocations.map(l => l.id));
      
      publicLocations.forEach(loc => {
        if (!publicIds.has(loc.id)) {
          allLocations.push(loc);
        }
      });
      
      // Sort by dateAdded descending
      return allLocations.sort((a, b) => b.dateAdded - a.dateAdded);
    } catch (error) {
      console.error('Error getting all locations:', error);
      throw error;
    }
  }

  /**
   * Get location by ID
   */
  async getLocationById(locationId) {
    try {
      const doc = await db.collection('locations').doc(locationId).get();
      
      if (!doc.exists) {
        return null;
      }
      
      return {
        id: doc.id,
        ...doc.data()
      };
    } catch (error) {
      console.error('Error getting location by ID:', error);
      throw error;
    }
  }

  /**
   * Create a new location
   */
  async createLocation(locationData) {
    try {
      const docRef = db.collection('locations').doc();
      const locationWithId = {
        ...locationData,
        id: docRef.id,
        dateAdded: locationData.dateAdded || Date.now()
      };
      
      await docRef.set(locationWithId);
      return locationWithId;
    } catch (error) {
      console.error('Error creating location:', error);
      throw error;
    }
  }

  /**
   * Update a location
   */
  async updateLocation(locationId, updateData) {
    try {
      const docRef = db.collection('locations').doc(locationId);
      await docRef.update(updateData);
      
      const updatedDoc = await docRef.get();
      return {
        id: updatedDoc.id,
        ...updatedDoc.data()
      };
    } catch (error) {
      console.error('Error updating location:', error);
      throw error;
    }
  }

  /**
   * Delete a location
   */
  async deleteLocation(locationId) {
    try {
      await db.collection('locations').doc(locationId).delete();
      return true;
    } catch (error) {
      console.error('Error deleting location:', error);
      throw error;
    }
  }

  /**
   * Get filtered locations for a user
   */
  async getFilteredUserLocations(userId, filters) {
    try {
      let query = db.collection('locations').where('userId', '==', userId);
      
      if (filters.visibility) {
        query = query.where('visibility', '==', filters.visibility);
      }
      
      if (filters.startDate) {
        query = query.where('dateAdded', '>=', filters.startDate);
      }
      
      if (filters.endDate) {
        query = query.where('dateAdded', '<=', filters.endDate);
      }
      
      const snapshot = await query.orderBy('dateAdded', 'desc').get();
      
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    } catch (error) {
      console.error('Error getting filtered locations:', error);
      throw error;
    }
  }

  /**
   * Get user notifications
   */
  async getUserNotifications(userId, limit = 50) {
    try {
      const snapshot = await db.collection('notifications')
        .where('userId', '==', userId)
        .orderBy('timestamp', 'desc')
        .limit(limit)
        .get();
      
      return snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      }));
    } catch (error) {
      console.error('Error getting user notifications:', error);
      throw error;
    }
  }

  /**
   * Create a notification
   */
  async createNotification(notificationData) {
    try {
      const docRef = db.collection('notifications').doc();
      const notificationWithId = {
        ...notificationData,
        id: docRef.id,
        timestamp: notificationData.timestamp || Date.now(),
        isRead: notificationData.isRead || false
      };
      
      await docRef.set(notificationWithId);
      return notificationWithId;
    } catch (error) {
      console.error('Error creating notification:', error);
      throw error;
    }
  }

  /**
   * Update a notification
   */
  async updateNotification(notificationId, updateData) {
    try {
      const docRef = db.collection('notifications').doc(notificationId);
      await docRef.update(updateData);
      
      const updatedDoc = await docRef.get();
      return {
        id: updatedDoc.id,
        ...updatedDoc.data()
      };
    } catch (error) {
      console.error('Error updating notification:', error);
      throw error;
    }
  }

  /**
   * Delete a notification
   */
  async deleteNotification(notificationId) {
    try {
      await db.collection('notifications').doc(notificationId).delete();
      return true;
    } catch (error) {
      console.error('Error deleting notification:', error);
      throw error;
    }
  }

  /**
   * Mark all notifications as read for a user
   */
  async markAllNotificationsAsRead(userId) {
    try {
      const snapshot = await db.collection('notifications')
        .where('userId', '==', userId)
        .where('isRead', '==', false)
        .get();
      
      const batch = db.batch();
      snapshot.docs.forEach(doc => {
        batch.update(doc.ref, { isRead: true });
      });
      
      await batch.commit();
      return snapshot.size;
    } catch (error) {
      console.error('Error marking all notifications as read:', error);
      throw error;
    }
  }

  /**
   * Delete all notifications for a user
   */
  async deleteAllUserNotifications(userId) {
    try {
      const snapshot = await db.collection('notifications')
        .where('userId', '==', userId)
        .get();
      
      const batch = db.batch();
      snapshot.docs.forEach(doc => {
        batch.delete(doc.ref);
      });
      
      await batch.commit();
      return snapshot.size;
    } catch (error) {
      console.error('Error deleting all notifications:', error);
      throw error;
    }
  }

  /**
   * Get user profile
   */
  async getUserProfile(userId) {
    try {
      const doc = await db.collection('users').doc(userId).get();
      
      if (!doc.exists) {
        return null;
      }
      
      return {
        id: doc.id,
        ...doc.data()
      };
    } catch (error) {
      console.error('Error getting user profile:', error);
      throw error;
    }
  }

  /**
   * Update user profile
   */
  async updateUserProfile(userId, updateData) {
    try {
      const docRef = db.collection('users').doc(userId);
      await docRef.set(updateData, { merge: true });
      
      const updatedDoc = await docRef.get();
      return {
        id: updatedDoc.id,
        ...updatedDoc.data()
      };
    } catch (error) {
      console.error('Error updating user profile:', error);
      throw error;
    }
  }
}

module.exports = new FirestoreService();

