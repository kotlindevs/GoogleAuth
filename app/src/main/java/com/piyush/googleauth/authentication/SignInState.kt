package com.piyush.googleauth.authentication

data class SignInState(
    val isSignInSuccessful : Boolean = false,
    val message : String? = null
)
