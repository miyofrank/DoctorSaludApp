package com.miyo.doctorsaludapp.presentation.view.Activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.miyo.doctorsaludapp.R
import com.miyo.doctorsaludapp.data.repository.FirestoreUserRepository
import com.miyo.doctorsaludapp.domain.model.UserProfile
import com.miyo.doctorsaludapp.databinding.ActivityLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val userRepo by lazy { FirestoreUserRepository(FirebaseFirestore.getInstance()) }

    private lateinit var googleClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    private val googleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                CoroutineScope(Dispatchers.Main).launch {
                    auth.signInWithCredential(credential).await()
                    ensureProfile(
                        nombres = account.givenName,
                        apellidos = account.familyName,
                        email = account.email
                    )
                    goToHome()
                }
            } catch (e: Exception) {
                toast("Google sign-in falló: ${e.message}")
            }
        }


    private fun printFacebookKeyHash() {
        try {
            val pkg = packageName

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // API 28+ usa SigningInfo
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        pkg,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNING_CERTIFICATES)
                }

                val signingInfo = info.signingInfo
                // signingInfo puede ser null en algunos escenarios -> null-safe
                val sigs = when {
                    signingInfo == null -> emptyArray()
                    signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                    else -> signingInfo.signingCertificateHistory
                }

                sigs.forEach { sig ->
                    val md = MessageDigest.getInstance("SHA")
                    md.update(sig.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    Log.d("FB_KEY_HASH", keyHash)
                }

            } else {
                // API < 28 usa signatures (deprecated, pero válido para compatibilidad)
                @Suppress("DEPRECATION")
                val info = packageManager.getPackageInfo(pkg, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val sigs = info.signatures ?: emptyArray()

                sigs.forEach { sig ->
                    val md = MessageDigest.getInstance("SHA")
                    md.update(sig.toByteArray())
                    val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                    Log.d("FB_KEY_HASH", keyHash)
                }
            }
        } catch (e: Exception) {
            Log.e("FB_KEY_HASH", "Error generando key hash: ${e.message}", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogle()
        setupFacebook()
        setupEmailLogin()
        setupRegisterLink()
        printFacebookKeyHash()
    }

    private fun setupGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // de google-services.json
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)

        binding.btnGoogle.setOnClickListener {
            googleLauncher.launch(googleClient.signInIntent)
        }
    }

    private fun setupFacebook() {
        callbackManager = CallbackManager.Factory.create()
        binding.btnFacebook.setOnClickListener {
            // Permisos mínimos
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("email", "public_profile"))
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        val token = result.accessToken ?: return
                        val credential = FacebookAuthProvider.getCredential(token.token)
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val res = auth.signInWithCredential(credential).await()
                                val user = res.user
                                ensureProfile(
                                    nombres = user?.displayName?.split(" ")?.firstOrNull(),
                                    apellidos = user?.displayName?.split(" ")?.drop(1)?.joinToString(" "),
                                    email = user?.email
                                )
                                goToHome()
                            } catch (e: Exception) {
                                toast("Facebook sign-in falló: ${e.message}")
                            }
                        }
                    }

                    override fun onCancel() { /* usuario canceló */ }
                    override fun onError(error: FacebookException) {
                        toast("Facebook error: ${error.message}")
                    }
                })
        }
    }

    private fun setupEmailLogin() {
        binding.btnLoginEmail.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            val pass = binding.etPassword.text?.toString().orEmpty()
            if (email.isEmpty() || pass.length < 6) {
                toast("Completa email y contraseña (mín. 6)")
                return@setOnClickListener
            }
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val res = auth.signInWithEmailAndPassword(email, pass).await()
                    ensureProfile(email = res.user?.email)
                    goToHome()
                } catch (e: Exception) {
                    toast("Inicio de sesión falló: ${e.message}")
                }
            }
        }
    }

    private fun setupRegisterLink() {
        binding.tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private suspend fun ensureProfile(
        nombres: String? = null,
        apellidos: String? = null,
        email: String? = null
    ) {
        val uid = auth.currentUser?.uid ?: return
        // Merge “soft”: si el doc existe se mantiene; si no, se crea con lo básico
        val userDoc = FirebaseFirestore.getInstance().collection("usuarios").document(uid).get().await()
        if (!userDoc.exists()) {
            userRepo.set(
                uid,
                UserProfile(
                    id = uid,
                    nombres = nombres,
                    apellidos = apellidos,
                    email = email,
                    autoAnalisis = false
                )
            )
        } else if (email != null) {
            // Si existe, al menos asegura email (por si estaba vacío)
            userRepo.updateFields(uid, mapOf("email" to email))
        }
    }

    private fun goToHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Necesario para Facebook LoginManager
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }
}
