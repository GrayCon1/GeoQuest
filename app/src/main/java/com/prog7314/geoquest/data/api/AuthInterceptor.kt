package com.prog7314.geoquest.data.api

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Interceptor to add Firebase Auth token to API requests
 * Note: Firebase getIdToken() is async, so we use a blocking approach with timeout
 */
class AuthInterceptor : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val TOKEN_TIMEOUT_SECONDS = 5L
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Get Firebase Auth token
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            try {
                // Get ID token using blocking approach with timeout
                val latch = CountDownLatch(1)
                var token: String? = null
                var error: Exception? = null
                
                currentUser.getIdToken(false)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            token = task.result?.token
                        } else {
                            error = task.exception
                        }
                        latch.countDown()
                    }
                
                // Wait for token with timeout
                val success = latch.await(TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                
                if (success && token != null) {
                    val authenticatedRequest = request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                    
                    Log.d(TAG, "Added auth token to request: ${request.url}")
                    return chain.proceed(authenticatedRequest)
                } else {
                    if (error != null) {
                        Log.e(TAG, "Error getting auth token", error)
                    } else if (!success) {
                        Log.w(TAG, "Timeout waiting for auth token")
                    } else {
                        Log.w(TAG, "Failed to get auth token")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception getting auth token", e)
            }
        } else {
            Log.w(TAG, "No authenticated user, proceeding without token")
        }
        
        // Proceed without token (will fail on protected endpoints)
        return chain.proceed(request)
    }
}

