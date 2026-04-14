package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
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

        val edtName            = findViewById<EditText>(R.id.edtName)
        val edtEmail           = findViewById<EditText>(R.id.edtEmail)
        val edtPassword        = findViewById<EditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<EditText>(R.id.edtConfirmPassword)
        val edtPhone           = findViewById<EditText>(R.id.edtPhone)
        val btnSignup          = findViewById<Button>(R.id.btnSignup)
        val btnBack            = findViewById<LinearLayout>(R.id.btnBack)
        val tvGoLogin          = findViewById<TextView>(R.id.tvGoLogin)
        val tvEmailError       = findViewById<TextView>(R.id.tvEmailError)
        val layoutEmail        = findViewById<LinearLayout>(R.id.layoutEmail)

        btnBack.setOnClickListener { finish() }

        tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Ẩn error khi user bắt đầu nhập lại email
        edtEmail.setOnFocusChangeListener { _, _ ->
            hideEmailError(layoutEmail, tvEmailError)
        }

        btnSignup.setOnClickListener {
            val name            = edtName.text.toString().trim()
            val email           = edtEmail.text.toString().trim()
            val password        = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()
            val phone           = edtPhone.text.toString().trim()

            // Reset error trước
            hideEmailError(layoutEmail, tvEmailError)

            // Validate
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || phone.isEmpty()) {
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

            btnSignup.isEnabled = false
            btnSignup.text = "Đang đăng ký..."

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: return@addOnCompleteListener

                        val userMap = hashMapOf(
                            "name"         to name,
                            "email"        to email,
                            "phone"        to phone,
                            "provider"     to "email",
                            "createdAt"    to System.currentTimeMillis(),
                            "balance"      to 0L,
                            "monthlySpend" to 0L,
                            "monthlyTrips" to 0L,
                            "todayTrips"   to 0L,
                            "co2Saved"     to 0.0,
                            "points"       to 0L,
                        )

                        db.collection("users").document(uid)
                            .set(userMap)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener {
                                startActivity(Intent(this, HomeActivity::class.java))
                                finish()
                            }

                    } else {
                        btnSignup.isEnabled = true
                        btnSignup.text = "ĐĂNG KÝ"

                        val msg = task.exception?.message ?: ""
                        when {
                            // ✅ Email đã tồn tại → hiện lỗi ngay dưới ô email
                            msg.contains("email address is already in use") ->
                                showEmailError(
                                    layoutEmail, tvEmailError,
                                    "⚠ Email này đã được đăng ký, vui lòng dùng email khác"
                                )
                            msg.contains("badly formatted") ->
                                showEmailError(
                                    layoutEmail, tvEmailError,
                                    "⚠ Email không hợp lệ"
                                )
                            msg.contains("password is invalid") ->
                                Toast.makeText(this, "Mật khẩu phải ít nhất 6 ký tự", Toast.LENGTH_LONG).show()
                            else ->
                                Toast.makeText(this, "Đăng ký thất bại: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    // ── Hiện lỗi email ────────────────────────────────────────────
    private fun showEmailError(
        layoutEmail: LinearLayout,
        tvError: TextView,
        message: String
    ) {
        // Đổi border ô email sang đỏ
        layoutEmail.setBackgroundResource(R.drawable.bg_input_field_error)
        tvError.text = message
        tvError.visibility = View.VISIBLE
        // Scroll / focus về ô email
        layoutEmail.requestFocus()
    }

    private fun hideEmailError(layoutEmail: LinearLayout, tvError: TextView) {
        layoutEmail.setBackgroundResource(R.drawable.bg_input_field)
        tvError.visibility = View.GONE
    }
}