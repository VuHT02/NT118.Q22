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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_screen_layout)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val edtEmail    = findViewById<EditText>(R.id.edtEmail)
        val edtPassword = findViewById<EditText>(R.id.edtPassword)
        val btnLogin    = findViewById<Button>(R.id.btnLogin)
        val btnGoogle   = findViewById<ImageButton>(R.id.btnGoogle)
        val btnFacebook = findViewById<ImageButton>(R.id.btnFacebook)
        val tvRegister  = findViewById<TextView>(R.id.tvRegister)

        // ── Email login ────────────────────────────────────────────
        btnLogin.setOnClickListener {
            val email    = edtEmail.text.toString().trim()
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
                        val msg = when (task.exception) {
                            is FirebaseAuthInvalidUserException        -> "Tài khoản không tồn tại"
                            is FirebaseAuthInvalidCredentialsException -> "Sai mật khẩu"
                            else -> "Đăng nhập thất bại: ${task.exception?.message}"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // ── Google login ───────────────────────────────────────────
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google login thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        btnGoogle.setOnClickListener {
            googleLauncher.launch(googleSignInClient.signInIntent)
        }

        // ── Facebook login ─────────────────────────────────────────
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

    // ── Google → Firebase ──────────────────────────────────────────
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    ensureFirestoreUser(
                        uid   = user.uid,
                        name  = user.displayName ?: "",
                        email = user.email ?: "",
                    )
                } else {
                    Toast.makeText(this, "Google auth thất bại", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ── Facebook → Firebase ────────────────────────────────────────
    private fun handleFacebookToken(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser ?: return@addOnCompleteListener
                    ensureFirestoreUser(
                        uid   = user.uid,
                        name  = user.displayName ?: "",
                        email = user.email ?: "",
                    )
                } else {
                    Toast.makeText(this, "Facebook auth thất bại", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ── Tạo document Firestore nếu chưa có ────────────────────────
    //
    // Dùng SetOptions.merge() để:
    //   - Lần đầu login  → tạo mới document với data đầy đủ
    //   - Lần sau login  → KHÔNG ghi đè balance/points đã có
    //
    private fun ensureFirestoreUser(uid: String, name: String, email: String) {
        val userRef = db.collection("users").document(uid)

        userRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                // User mới → tạo document với giá trị mặc định
                val newUser = mapOf(
                    "name"         to name,
                    "email"        to email,
                    "balance"      to 0L,
                    "monthlySpend" to 0L,
                    "monthlyTrips" to 0L,
                    "co2Saved"     to 0.0,
                    "points"       to 0L,
                    "todayTrips"   to 0L,
                    "createdAt"    to System.currentTimeMillis(),
                    "provider"     to getProvider(),
                )
                userRef.set(newUser)
                    .addOnSuccessListener { goHome() }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lỗi tạo tài khoản: ${it.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // User cũ → chỉ cập nhật name/email nếu thay đổi, giữ nguyên số dư
                userRef.set(
                    mapOf("name" to name, "email" to email),
                    SetOptions.merge()
                )
                goHome()
            }
        }.addOnFailureListener {
            // Firestore lỗi nhưng auth thành công → vẫn vào home
            goHome()
        }
    }

    private fun getProvider(): String {
        val providers = auth.currentUser?.providerData?.map { it.providerId } ?: emptyList()
        return when {
            "google.com"   in providers -> "google"
            "facebook.com" in providers -> "facebook"
            else                        -> "email"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun goHome() {
        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}