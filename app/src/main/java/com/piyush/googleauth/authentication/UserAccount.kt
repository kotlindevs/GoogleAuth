package com.piyush.googleauth.authentication

data class UserAccount(
    val accountId : String,
    val displayName : String?,
    val accountEmail : String?,
    val accountPicture : String?
)
