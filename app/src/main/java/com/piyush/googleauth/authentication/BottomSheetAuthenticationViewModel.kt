package com.piyush.googleauth.authentication

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BottomSheetAuthenticationViewModel(
    private val repository : BottomSheetAuthenticationRepository
) : ViewModel() {

    private val _state : MutableStateFlow<SignInState> = MutableStateFlow(SignInState())
    val state = _state.asStateFlow()

    fun onSignInResult(result : SignInResult){
        _state.update {
            it.copy(
                isSignInSuccessful = result.account != null,
                message = result.message
            )
        }
    }

    fun resetState(){
        _state.update {
            SignInState()
        }
    }

    fun currentUser() : FirebaseUser?{
        return repository.currentUser()
    }
}