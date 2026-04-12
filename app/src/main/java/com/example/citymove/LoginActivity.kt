package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    // Google
    private lateinit var googleSignInClient: GoogleSignInClient

    // Facebook
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen_layout)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        val edtEmail = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogle = findViewById<ImageButton>(R.id.btnGoogle)
        val btnFacebook = findViewById<ImageButton>(R.id.btnFacebook)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // ================= EMAIL LOGIN =================
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        goHome()
                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "Tài khoản không tồn tại"
                            is FirebaseAuthInvalidCredentialsException -> "Sai mật khẩu"
                            else -> "Đăng nhập thất bại: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ================= GOOGLE LOGIN =================
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google login fail: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLauncher.launch(signInIntent)
        }

        // ================= FACEBOOK LOGIN =================
        callbackManager = CallbackManager.Factory.create()

        btnFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this, listOf("email", "public_profile")
            )
        }

        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    handleFacebookToken(result.accessToken)
                }

                override fun onCancel() {}
                override fun onError(error: FacebookException) {
                    Toast.makeText(this@LoginActivity, "Facebook lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // ================= GOOGLE → FIREBASE =================
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) goHome()
                else Toast.makeText(this, "Google auth fail", Toast.LENGTH_SHORT).show()
            }
    }

    // ================= FACEBOOK → FIREBASE =================
    private fun handleFacebookToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener {
                if (it.isSuccessful) goHome()
                else Toast.makeText(this, "Facebook auth fail", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun goHome() {
        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }
}