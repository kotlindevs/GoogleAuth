package com.piyush.googleauth

import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.piyush.googleauth.authentication.BottomSheetAuthenticationRepository
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModel
import com.piyush.googleauth.authentication.BottomSheetAuthenticationViewModelFactory
import com.piyush.googleauth.databinding.ActivitySecondBinding
import kotlinx.coroutines.launch

class SecondActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecondBinding
    private lateinit var repository: BottomSheetAuthenticationRepository
    private lateinit var viewModel: BottomSheetAuthenticationViewModel
    private lateinit var viewModelFactory: BottomSheetAuthenticationViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySecondBinding.inflate(layoutInflater)
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

        onBackPressedDispatcher.addCallback {  }

        if(viewModel.currentUser()!= null){
            val user = viewModel.currentUser()?: return
            binding.accountName.text = user.displayName
            binding.accountEmail.text = user.email
            Glide.with(this).load(user.photoUrl).into(binding.accountImage)
        }

        binding.signOut.setOnClickListener {
            lifecycleScope.launch {
                repository.signOut()
                viewModel.resetState()
                startActivity(
                    Intent(
                        this@SecondActivity, MainActivity::class.java
                    )
                )
            }
        }
    }
}