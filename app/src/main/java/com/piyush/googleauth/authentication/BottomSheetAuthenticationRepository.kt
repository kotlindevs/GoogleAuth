package com.piyush.googleauth.authentication

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.piyush.googleauth.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class BottomSheetAuthenticationRepository(
    private val context: Context
) {

    private val oneTapClient : SignInClient = Identity.getSignInClient(context)
    private val auth : FirebaseAuth = Firebase.auth

    @Suppress("DEPRECATION")
    private fun buildSignInRequest() : BeginSignInRequest{
        return BeginSignInRequest.Builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.Builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.oauth_client))
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    @Suppress("DEPRECATION")
    suspend fun googleSignIn() : IntentSender?{
        val result = try{
            oneTapClient.beginSignIn(
                buildSignInRequest()
            ).await()
        }catch (exception : Exception){
            exception.printStackTrace()
            if(exception is CancellationException) throw exception
            null
        }
        return result?.pendingIntent?.intentSender
    }

    @Suppress("DEPRECATION")
    suspend fun signInWithIntent(intent: Intent) : SignInResult{
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val googleCredential = GoogleAuthProvider.getCredential(
            googleIdToken,null
        )

        return try{
            val account = auth.signInWithCredential(
                googleCredential
            ).await().user
            SignInResult(
                account = account?.run {
                    UserAccount(
                        accountId = uid,
                        displayName = displayName,
                        accountEmail = email,
                        accountPicture = photoUrl?.toString()
                    )
                },
                message = null
            )
        }catch (exception : Exception){
            exception.printStackTrace()
            if(exception is CancellationException) throw exception
            SignInResult(
                account = null,
                message = exception.message
            )
        }
    }

    @Suppress("DEPRECATION")
    suspend fun signOut() {
        try{
            oneTapClient.signOut().await()
            auth.signOut()
        }catch (exception : Exception){
            exception.printStackTrace()
            if(exception is CancellationException) throw exception
        }
    }

    fun currentUser() : FirebaseUser?{
        return auth.currentUser
    }
}