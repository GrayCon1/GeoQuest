package com.prog7314.geoquest.data.api

import com.prog7314.geoquest.data.data.LocationData
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.data.UserData
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for GeoQuest REST API
 */
interface GeoQuestApiService {
    
    // ========== Locations Endpoints ==========
    
    /**
     * GET /api/locations
     * Get all locations (user's own + public locations)
     */
    @GET("locations")
    suspend fun getAllLocations(): Response<LocationsResponse>
    
    /**
     * GET /api/locations/user
     * Get user's own locations
     */
    @GET("locations/user")
    suspend fun getUserLocations(): Response<LocationsResponse>
    
    /**
     * GET /api/locations/filtered
     * Get filtered locations
     */
    @GET("locations/filtered")
    suspend fun getFilteredLocations(
        @Query("visibility") visibility: String? = null,
        @Query("startDate") startDate: Long? = null,
        @Query("endDate") endDate: Long? = null
    ): Response<LocationsResponse>
    
    /**
     * GET /api/locations/:id
     * Get location by ID
     */
    @GET("locations/{id}")
    suspend fun getLocationById(@Path("id") locationId: String): Response<LocationResponse>
    
    /**
     * POST /api/locations
     * Create a new location
     */
    @POST("locations")
    suspend fun createLocation(@Body location: LocationData): Response<LocationResponse>
    
    /**
     * PUT /api/locations/:id
     * Update a location
     */
    @PUT("locations/{id}")
    suspend fun updateLocation(
        @Path("id") locationId: String,
        @Body location: LocationData
    ): Response<LocationResponse>
    
    /**
     * DELETE /api/locations/:id
     * Delete a location
     */
    @DELETE("locations/{id}")
    suspend fun deleteLocation(@Path("id") locationId: String): Response<MessageResponse>
    
    // ========== Notifications Endpoints ==========
    
    /**
     * GET /api/notifications
     * Get user notifications
     */
    @GET("notifications")
    suspend fun getNotifications(@Query("limit") limit: Int = 50): Response<NotificationsResponse>
    
    /**
     * POST /api/notifications
     * Create a notification
     */
    @POST("notifications")
    suspend fun createNotification(@Body notification: NotificationData): Response<NotificationResponse>
    
    /**
     * PUT /api/notifications/:id
     * Update a notification
     */
    @PUT("notifications/{id}")
    suspend fun updateNotification(
        @Path("id") notificationId: String,
        @Body notification: NotificationData
    ): Response<NotificationResponse>
    
    /**
     * PUT /api/notifications/mark-all-read
     * Mark all notifications as read
     */
    @PUT("notifications/mark-all-read")
    suspend fun markAllNotificationsAsRead(): Response<MarkAllReadResponse>
    
    /**
     * DELETE /api/notifications/:id
     * Delete a notification
     */
    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") notificationId: String): Response<MessageResponse>
    
    /**
     * DELETE /api/notifications
     * Delete all notifications
     */
    @DELETE("notifications")
    suspend fun deleteAllNotifications(): Response<MessageResponse>
    
    // ========== Users Endpoints ==========
    
    /**
     * GET /api/users/me
     * Get current user profile
     */
    @GET("users/me")
    suspend fun getUserProfile(): Response<UserResponse>
    
    /**
     * PUT /api/users/me
     * Update user profile
     */
    @PUT("users/me")
    suspend fun updateUserProfile(@Body user: UserData): Response<UserResponse>
}

// ========== Response Models ==========

data class LocationsResponse(
    val locations: List<LocationData>
)

data class LocationResponse(
    val location: LocationData
)

data class NotificationsResponse(
    val notifications: List<NotificationData>
)

data class NotificationResponse(
    val notification: NotificationData
)

data class UserResponse(
    val user: UserData
)

data class MessageResponse(
    val message: String
)

data class MarkAllReadResponse(
    val message: String,
    val count: Int
)

data class ApiError(
    val error: ErrorDetail
)

data class ErrorDetail(
    val message: String,
    val status: Int
)

