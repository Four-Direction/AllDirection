package com.fourDirection.allDirection.data

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class Auth(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userRepository: UserRepository = UserRepository()
    
    val googleSignInClient: GoogleSignInClient by lazy {
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resId != 0) context.getString(resId) else ""
        
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            
        if (webClientId.isNotEmpty()) {
            gsoBuilder.requestIdToken(webClientId)
        }
        
        GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun register(name: String, email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                userRepository.saveUserToFirestore(user, name)
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after registration"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after login"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun firebaseAuthWithGoogle(acct: GoogleSignInAccount): Result<FirebaseUser> {
        val idToken = acct.idToken ?: return Result.failure(Exception("ID Token is null"))
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                userRepository.saveUserToFirestore(user)
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after Google Auth"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
