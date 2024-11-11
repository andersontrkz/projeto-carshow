package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    private var verificationId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        setupGoogleLogin()
        setupView()
        verifyLoggedUser()
    }

    private fun setupView() {
        binding.btLoginGoogle.setOnClickListener { signIn() }
        binding.btSendCode.setOnClickListener { sendVerificationCode() }
        binding.btValidateCode.setOnClickListener { verifyCode() }
    }

    private fun sendVerificationCode() {
        val phoneNumber = binding.etPhone.text.toString()
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // TODO
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@LoginActivity.verificationId = verificationId

                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.codigo_enviado),
                        Toast.LENGTH_SHORT
                    ).show()

                    binding.etCode.visibility = View.VISIBLE
                    binding.btValidateCode.visibility = View.VISIBLE
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(
                        this@LoginActivity,
                        getString(R.string.login_invalido),
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode() {
        val code = binding.etCode.text.toString()
        val credential = PhoneAuthProvider.getCredential(verificationId, code)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                onCredentialCompleteListener(task)
            }
    }

    private fun onCredentialCompleteListener(
        task: Task<AuthResult>
    ) {
        if (task.isSuccessful) {
            navigateToMainActivity()
        } else {
            Toast.makeText(
                this,
                "${task.exception?.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun verifyLoggedUser() {
        if (auth.currentUser != null) {
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        startActivity(MainActivity.newIntent(this))
        finish()
    }

    private fun setupGoogleLogin() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("695990605075-hopg1errjcsgt770utmph8g68gb49alc.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    firebaseAuthWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Log.e("LoginActivity", getString(R.string.login_invalido), e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                onCredentialCompleteListener(task)
            }
    }

    private fun signIn() {
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    companion object {
        fun newIntent(context: Context) = Intent(context, LoginActivity::class.java)
    }
}