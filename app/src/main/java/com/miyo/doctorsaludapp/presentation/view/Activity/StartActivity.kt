package com.miyo.doctorsaludapp.presentation.view.Activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.FacebookSdk
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.remote.FirebaseService
import com.miyo.doctorsaludapp.data.repository.LoginRepositoryImpl
import com.miyo.doctorsaludapp.databinding.ActivityStartBinding
import com.miyo.doctorsaludapp.domain.usecase.LoginUseCase
import com.miyo.doctorsaludapp.domain.usecase.RegisterUseCase
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModel
import com.miyo.doctorsaludapp.presentation.viewmodel.AuthViewModelFactory

class StartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStartBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private val loginUseCase by lazy { LoginUseCase(LoginRepositoryImpl(FirebaseService())) }
    private val registerUseCase by lazy { RegisterUseCase(LoginRepositoryImpl(FirebaseService())) }
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(loginUseCase, registerUseCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        FacebookSdk.sdkInitialize(applicationContext)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        setupGoogleSignIn()
        setupFacebookSignIn()

        binding.googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        binding.facebookSignInButton.setOnClickListener {
            signInWithFacebook()
        }

        binding.registerTextView.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        authViewModel.user.observe(this, { user ->
            user?.let {
                // Handle successful login
                // Navegar a HomeActivity
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                finish()
            }
        })

        authViewModel.error.observe(this, { error ->
            error?.let {
                // Handle error
                if (error == "Cuenta creada") {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                authViewModel.loginWithCredential(credential)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Error al obtener la cuenta de Google", e)
                authViewModel.notifyError(e.message.toString())
            }
        } else {
            Log.e("GoogleSignIn", "Sign-in canceled or failed")
            authViewModel.notifyError("Google Sign-In canceled or failed")
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun setupFacebookSignIn() {
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    val credential = FacebookAuthProvider.getCredential(loginResult.accessToken.token)
                    authViewModel.loginWithCredential(credential)
                }

                override fun onCancel() {
                    // Handle cancel
                }

                override fun onError(error: FacebookException) {
                    // Handle error
                }
            })
    }


    private fun signInWithFacebook() {
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}