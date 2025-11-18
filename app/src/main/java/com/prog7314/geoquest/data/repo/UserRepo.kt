package com.prog7314.geoquest.data.repo

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.prog7314.geoquest.data.data.UserData
import com.prog7314.geoquest.data.preferences.UserPreferences
import com.prog7314.geoquest.utils.NetworkHelper
import kotlinx.coroutines.tasks.await

class UserRepo(private val context: Context? = null) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    companion object {
        private const val TAG = "UserRepo"
    }

    // ... existing registerUser, loginUser, etc. methods

    suspend fun signInWithGoogle(idToken: String): Result<UserData> {
        return try {
            Log.d(TAG, "Starting Google Sign-In with token")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user ?: throw Exception("Google Sign-In failed: No user returned")

            Log.d(TAG, "Google Auth successful for user: ${user.uid}")

            val userDocRef = usersCollection.document(user.uid)
            val userDoc = userDocRef.get().await()

            if (userDoc.exists()) {
                // User exists in Firestore, fetch their data
                Log.d(TAG, "User profile exists in Firestore, fetching...")
                val userData = userDoc.toObject(UserData::class.java)?.copy(id = user.uid)
                    ?: throw Exception("Failed to parse user profile")
                Result.success(userData)
            } else {
                // User does not exist in Firestore, create a new profile
                Log.d(TAG, "User profile does not exist in Firestore, creating new one...")
                val newUser = UserData(
                    id = user.uid,
                    name = user.displayName ?: "N/A",
                    username = user.email?.substringBefore('@') ?: "user_${user.uid.take(6)}",
                    email = user.email ?: ""
                )
                userDocRef.set(newUser).await()
                Log.d(TAG, "New user profile created successfully")
                Result.success(newUser)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            Result.failure(Exception("Google Sign-In failed: ${e.message}", e))
        }
    }

    // ... rest of the UserRepo class
    suspend fun registerUser(userData: UserData, password: String): Result<UserData> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(userData.email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("User creation failed")

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userData.name)
                .build()
            authResult.user?.updateProfile(profileUpdates)?.await()

            // Create user profile in Firestore with Auth UID
            val userDataWithId = userData.copy(id = userId)
            usersCollection.document(userId).set(userDataWithId).await()

            // Cache credentials for offline use
            context?.let {
                UserPreferences.saveUserData(it, userDataWithId, password)
            }

            Result.success(userDataWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<UserData> {
        return try {
            // Check if network is available
            val isOnline = context?.let { NetworkHelper.isNetworkAvailable(it) } ?: true

            if (!isOnline) {
                // Try offline login with cached credentials
                Log.d(TAG, "No network available, attempting offline login")
                return loginOffline(email, password)
            }

            // Online login
            Log.d(TAG, "Network available, attempting online login")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: throw Exception("Login failed")

            // Get user profile from Firestore
            val doc = usersCollection.document(userId).get().await()
            val userData = doc.toObject(UserData::class.java)?.copy(id = userId)
                ?: throw Exception("User profile not found")

            // Cache credentials for offline use
            context?.let {
                UserPreferences.saveUserData(it, userData, password)
            }

            Result.success(userData)
        } catch (e: Exception) {
            Log.e(TAG, "Online login failed", e)

            // If online login fails, try offline login as fallback
            if (context != null) {
                Log.d(TAG, "Attempting offline login as fallback")
                return loginOffline(email, password)
            }

            Result.failure(e)
        }
    }

    /**
     * Login using cached credentials (offline mode)
     */
    private fun loginOffline(email: String, password: String): Result<UserData> {
        return try {
            if (context == null) {
                throw Exception("Offline login not available")
            }

            val cachedUserData = UserPreferences.getCachedUserData(context)
                ?: throw Exception("No cached credentials found. Please connect to the internet to login.")

            val cachedPassword = UserPreferences.getCachedPassword(context)
                ?: throw Exception("No cached credentials found. Please connect to the internet to login.")

            // Verify email and password match cached credentials
            if (cachedUserData.email.equals(email, ignoreCase = true) && cachedPassword == password) {
                Log.d(TAG, "Offline login successful")
                Result.success(cachedUserData)
            } else {
                throw Exception("Invalid credentials. Please connect to the internet to login.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Offline login failed", e)
            Result.failure(e)
        }
    }

    /**
     * Login as guest (no authentication required)
     */
    fun loginAsGuest(): Result<UserData> {
        return try {
            if (context == null) {
                throw Exception("Guest login not available")
            }

            UserPreferences.setGuestMode(context, true)
            val guestUser = UserPreferences.getCachedUserData(context)
                ?: throw Exception("Failed to create guest user")

            Log.d(TAG, "Guest login successful")
            Result.success(guestUser)
        } catch (e: Exception) {
            Log.e(TAG, "Guest login failed", e)
            Result.failure(e)
        }
    }

    /**
     * Auto sign-in with cached credentials
     */
    suspend fun autoSignIn(): Result<UserData> {
        return try {
            if (context == null) {
                throw Exception("Auto sign-in not available")
            }

            // If guest mode is set, do NOT auto sign-in as guest; require explicit user action
            if (UserPreferences.isGuestMode(context)) {
                throw Exception("Guest mode requires explicit selection")
            }

            if (!UserPreferences.isAutoSignInEnabled(context)) {
                throw Exception("Auto sign-in is disabled")
            }

            val cachedUserData = UserPreferences.getCachedUserData(context)
                ?: throw Exception("No cached user data found")
            val cachedPassword = UserPreferences.getCachedPassword(context)
                ?: throw Exception("No cached password found")

            return loginUser(cachedUserData.email, cachedPassword)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userData: UserData): Result<UserData> {
        return try {
            // Update Firestore document
            usersCollection.document(userData.id).set(userData).await()

            // Update display name in Firebase Auth
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userData.name)
                .build()
            auth.currentUser?.updateProfile(profileUpdates)?.await()

            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> {
        return try {
            val user = auth.currentUser ?: throw Exception("No user logged in")
            val email = user.email ?: throw Exception("Email not found")

            // Re-authenticate before password change
            val credential = com.google.firebase.auth.EmailAuthProvider
                .getCredential(email, currentPassword)
            user.reauthenticate(credential).await()

            // Update password
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<UserData> {
        return try {
            val doc = usersCollection.document(userId).get().await()
            val userData = doc.toObject(UserData::class.java)?.copy(id = userId)
                ?: throw Exception("User not found")
            Result.success(userData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid ?: throw Exception("No user logged in")

            // Delete user profile from Firestore
            usersCollection.document(userId).delete().await()

            // Delete user from Firebase Auth
            auth.currentUser?.delete()?.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserLoggedIn(): Boolean = auth.currentUser != null

    fun logoutUser() {
        auth.signOut()
        // Clear cached credentials
        context?.let {
            UserPreferences.clearAll(it)
        }
    }
}
