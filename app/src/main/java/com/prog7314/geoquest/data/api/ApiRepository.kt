package com.prog7314.geoquest.data.api

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.prog7314.geoquest.R
import com.prog7314.geoquest.data.data.LocationData
import com.prog7314.geoquest.data.data.NotificationData
import com.prog7314.geoquest.data.data.UserData
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository for API calls using Retrofit
 */
class ApiRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "ApiRepository"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val apiService: GeoQuestApiService
    
    init {
        val baseUrl = context.getString(R.string.api_base_url)
        
        // Create logging interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Create OkHttp client with interceptors
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(loggingInterceptor)
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
        
        // Create Retrofit instance
        val retrofit = Retrofit.Builder()
            .baseUrl("$baseUrl/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(GeoQuestApiService::class.java)
    }
    
    /**
     * Handle API errors and convert to Result
     */
    private inline fun <T> handleApiCall(apiCall: () -> retrofit2.Response<T>): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Empty response body"))
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = parseErrorMessage(errorBody) ?: "API error: ${response.code()}"
                Log.e(TAG, "API error: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP exception", e)
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: IOException) {
            Log.e(TAG, "Network IO exception", e)
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Parse error message from API error response
     */
    private fun parseErrorMessage(errorBody: String?): String? {
        return try {
            if (errorBody != null) {
                val gson = Gson()
                val apiError = gson.fromJson(errorBody, ApiError::class.java)
                apiError.error.message
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // ========== Location Methods ==========
    
    suspend fun getAllLocations(): Result<List<LocationData>> {
        return handleApiCall { apiService.getAllLocations() }.map { it.locations }
    }
    
    suspend fun getUserLocations(): Result<List<LocationData>> {
        return handleApiCall { apiService.getUserLocations() }.map { it.locations }
    }
    
    suspend fun getFilteredLocations(
        visibility: String? = null,
        startDate: Long? = null,
        endDate: Long? = null
    ): Result<List<LocationData>> {
        return handleApiCall {
            apiService.getFilteredLocations(visibility, startDate, endDate)
        }.map { it.locations }
    }
    
    suspend fun getLocationById(locationId: String): Result<LocationData?> {
        return handleApiCall { apiService.getLocationById(locationId) }.map { it.location }
    }
    
    suspend fun createLocation(location: LocationData): Result<LocationData> {
        return handleApiCall { apiService.createLocation(location) }.map { it.location }
    }
    
    suspend fun updateLocation(locationId: String, location: LocationData): Result<LocationData> {
        return handleApiCall { apiService.updateLocation(locationId, location) }.map { it.location }
    }
    
    suspend fun deleteLocation(locationId: String): Result<Unit> {
        return handleApiCall { apiService.deleteLocation(locationId) }.map { Unit }
    }
    
    // ========== Notification Methods ==========
    
    suspend fun getNotifications(limit: Int = 50): Result<List<NotificationData>> {
        return handleApiCall { apiService.getNotifications(limit) }.map { it.notifications }
    }
    
    suspend fun createNotification(notification: NotificationData): Result<NotificationData> {
        return handleApiCall { apiService.createNotification(notification) }.map { it.notification }
    }
    
    suspend fun updateNotification(
        notificationId: String,
        notification: NotificationData
    ): Result<NotificationData> {
        return handleApiCall {
            apiService.updateNotification(notificationId, notification)
        }.map { it.notification }
    }
    
    suspend fun markAllNotificationsAsRead(): Result<Int> {
        return handleApiCall { apiService.markAllNotificationsAsRead() }.map { it.count }
    }
    
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return handleApiCall { apiService.deleteNotification(notificationId) }.map { Unit }
    }
    
    suspend fun deleteAllNotifications(): Result<Unit> {
        return handleApiCall { apiService.deleteAllNotifications() }.map { Unit }
    }
    
    // ========== User Methods ==========
    
    suspend fun getUserProfile(): Result<UserData> {
        return handleApiCall { apiService.getUserProfile() }.map { it.user }
    }
    
    suspend fun updateUserProfile(user: UserData): Result<UserData> {
        return handleApiCall { apiService.updateUserProfile(user) }.map { it.user }
    }
}

