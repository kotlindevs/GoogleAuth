@file:Suppress("DEPRECATION")

package com.piyush.googleauth

import android.content.Intent
import android.credentials.GetCredentialException
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.GoogleAuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.piyush.googleauth.authentication.BottomSheetAuthenticationRepository
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModel
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModelFactory
import com.piyush.googleauth.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel : BottomSheetAuthenticationViewModel
    private lateinit var repository : BottomSheetAuthenticationRepository
    private lateinit var viewModelFactory : BottomSheetAuthenticationViewModelFactory
    private val credentialManager by lazy {
        CredentialManager.create(this)
    }
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onStart() {
        super.onStart()

        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md: MessageDigest = MessageDigest.getInstance("SHA-256")
        val digest: ByteArray = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it)}

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

        val googleIdOption : GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.oauth_client))
            .setNonce(hashedNonce)
            .setAutoSelectEnabled(false)
            .setFilterByAuthorizedAccounts(true)
            .build()

        val getCredentialRequest : GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption).build()

        binding.bottomSheetCredentialManager.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        request = getCredentialRequest,
                        context = this@MainActivity
                    )
                   repository.handleCredentials(result = result)
                }catch (exception : GetCredentialException){
                    Log.e(TAG,"onStart: ",exception)
                }
            }
        }

        val googleSignInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.oauth_client))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this,googleSignInOption)
    }

    private fun signInWithGoogle(){
        val signInIntent = googleSignInClient.signInIntent
        firebaseLauncher.launch(signInIntent)
    }

    private val firebaseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result -> if(result.resultCode == RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if(task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if(account!= null){
                updateUi(account)
            }
        }
    }

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