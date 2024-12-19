package com.piyush.googleauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
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

    private lateinit var googleSignInClient: GoogleSignInClient

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

    @Suppress("DEPRECATION")
    override fun onStart() {
        super.onStart()

        if(viewModel.currentUser()!= null){
            startActivity(
                Intent(
                    this@MainActivity, SecondActivity::class.java
                )
            )
        }

        lifecycleScope.launch {
            viewModel.state.collect{
                state -> if(state.isSignInSuccessful){
                    startActivity(
                        Intent(
                            this@MainActivity, SecondActivity::class.java
                        )
                    )
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
                bottomSheetLauncher.launch(
                    IntentSenderRequest.Builder(
                        intentSender ?: return@launch
                    ).build()
                )
            }
        }

        binding.firebaseGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.oauth_client))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,gso)


    }

    private fun signInWithGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        firebaseLauncher.launch(signInIntent)
    }

    @Suppress("DEPRECATION")
    private val firebaseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result -> if(result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    @Suppress("DEPRECATION")
    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if(task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if(account!= null){
                updateUi(account)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun updateUi(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken,null)
        Firebase.auth.signInWithCredential(credential).addOnCompleteListener {
            if(it.isSuccessful){
                startActivity(
                    Intent(
                        this@MainActivity, SecondActivity::class.java
                    )
                )
            }else{
                Snackbar.make(binding.root,"Sign in failed",Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private val bottomSheetLauncher = registerForActivityResult(
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