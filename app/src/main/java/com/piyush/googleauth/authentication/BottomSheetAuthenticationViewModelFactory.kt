package com.piyush.googleauth.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BottomSheetAuthenticationViewModelFactory(
    private val repository: BottomSheetAuthenticationRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(BottomSheetAuthenticationViewModel::class.java)) {
            return BottomSheetAuthenticationViewModel(repository) as T
        }else{
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}