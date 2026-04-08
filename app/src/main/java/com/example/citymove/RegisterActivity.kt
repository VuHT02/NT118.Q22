package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sign_up)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val edtEmail           = findViewById<EditText>(R.id.edtEmail)
        val edtPassword        = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val edtPhone           = findViewById<EditText>(R.id.edtPhone)
        val btnSignup          = findViewById<Button>(R.id.btnSignup)
        val btnBack            = findViewById<LinearLayout>(R.id.btnBack)
        val tvGoLogin          = findViewById<TextView>(R.id.tvGoLogin)

        // ── Quay lại ──────────────────────────────────────────
        btnBack.setOnClickListener { finish() }

        // ── Đã có tài khoản → Login ───────────────────────────
        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // ── Đăng ký ───────────────────────────────────────────
        btnSignup.setOnClickListener {
            val email           = edtEmail.text.toString().trim()
            val password        = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()
            val phone           = edtPhone.text.toString().trim()

            // Validate
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ Chỉ gọi 1 lần
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // ✅ Lưu thông tin vào Firestore
                        val uid     = auth.currentUser?.uid ?: return@addOnCompleteListener
                        val userMap = hashMapOf(
                            "email"     to email,
                            "phone"     to phone,
                            "createdAt" to System.currentTimeMillis()
                        )

                        db.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                // Firestore lỗi nhưng Auth ok → vẫn vào Home
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }

                    } else {
                        // ✅ Bắt lỗi tiếng Việt
                        val errorMessage = when {
                            task.exception?.message?.contains("email address is already in use") == true ->
                                "Email này đã được đăng ký, vui lòng dùng email khác"
                            task.exception?.message?.contains("badly formatted") == true ->
                                "Email không hợp lệ"
                            task.exception?.message?.contains("password is invalid") == true ->
                                "Mật khẩu phải ít nhất 6 ký tự"
                            else -> "Đăng ký thất bại: ${task.exception?.message}"
                        }
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}