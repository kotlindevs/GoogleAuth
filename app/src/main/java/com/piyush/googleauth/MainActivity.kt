package com.piyush.googleauth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.piyush.googleauth.authentication.BottomSheetAuthenticationRepository
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModel
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModelFactory
import com.piyush.googleauth.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel : BottomSheetAuthenticationViewModel
    private lateinit var repository : BottomSheetAuthenticationRepository
    private lateinit var viewModelFactory : BottomSheetAuthenticationViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        repository = BottomSheetAuthenticationRepository(context = this)
        viewModelFactory = BottomSheetAuthenticationViewModelFactory(repository = repository)
        viewModel = ViewModelProvider(this, viewModelFactory)[BottomSheetAuthenticationViewModel::class.java]
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            viewModel.state.collect{
                state -> if(state.isSignInSuccessful){
                    Snackbar.make(binding.root,"Sign in successful",Snackbar.LENGTH_SHORT).show()
                    viewModel.resetState()
                }

                if(state.message!= null){
                    Snackbar.make(binding.root,state.message,Snackbar.LENGTH_SHORT).show()
                }
            }
        }

        binding.bottomSheetGoogleSignIn.setOnClickListener {
            lifecycleScope.launch {
                val intentSender = repository.googleSignIn()
                launcher.launch(
                    IntentSenderRequest.Builder(
                        intentSender ?: return@launch
                    ).build()
                )
            }
        }
    }

    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ){
        result -> if(result.resultCode == RESULT_OK){
            lifecycleScope.launch {
                val signInResult = repository.signInWithIntent(
                    intent = result?.data ?: return@launch
                )
                viewModel.onSignInResult(signInResult)
            }
        }else{
            Snackbar.make(
                binding.root,"Sign in failed",Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}