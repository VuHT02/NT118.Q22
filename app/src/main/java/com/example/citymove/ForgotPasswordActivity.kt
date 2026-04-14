package com.example.citymove

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var edtEmail: EditText
    private lateinit var btnSend: Button
    private lateinit var layoutForm: View
    private lateinit var layoutSuccess: View
    private lateinit var tvSuccessEmail: TextView
    private lateinit var btnResend: TextView
    private lateinit var btnOpenGmail: TextView
    private lateinit var btnOpenEmail: TextView

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpasswordscreens)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()

        initViews()
        setupListeners()
    }

    private fun initViews() {
        edtEmail       = findViewById(R.id.edtEmail)
        btnSend        = findViewById(R.id.btnSend)
        layoutForm     = findViewById(R.id.layoutForm)
        layoutSuccess  = findViewById(R.id.layoutSuccess)
        tvSuccessEmail = findViewById(R.id.tvSuccessEmail)
        btnResend      = findViewById(R.id.btnResend)
        btnOpenGmail   = findViewById(R.id.btnOpenGmail)
        btnOpenEmail   = findViewById(R.id.btnOpenEmail)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Auto fill email từ Login nếu có
        intent.getStringExtra("email")?.let { edtEmail.setText(it) }
    }

    private fun setupListeners() {
        btnSend.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            if (!validateEmail(email)) return@setOnClickListener
            sendResetEmail(email)
        }

        btnResend.setOnClickListener {
            if (!btnResend.isEnabled) return@setOnClickListener
            val email = tvSuccessEmail.text.toString()
            sendResetEmail(email)
        }

        // ✅ Mở thẳng Gmail
        btnOpenGmail.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_EMAIL)
                    setPackage("com.google.android.gm")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // Gmail không cài → fallback mở app email bất kỳ
                openAnyEmailApp()
            }
        }

        // ✅ Mở app email bất kỳ
        btnOpenEmail.setOnClickListener {
            openAnyEmailApp()
        }
    }

    private fun openAnyEmailApp() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_EMAIL)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không tìm thấy app email", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Validate ──────────────────────────────────────────────────
    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                edtEmail.error = "Vui lòng nhập email"
                edtEmail.requestFocus()
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                edtEmail.error = "Email không hợp lệ"
                edtEmail.requestFocus()
                false
            }
            else -> true
        }
    }

    // ── Send email ────────────────────────────────────────────────
    private fun sendResetEmail(email: String) {
        setLoading(true)

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                setLoading(false)
                showSuccess(email)
                startResendCountdown()
            }
            .addOnFailureListener { e ->
                setLoading(false)
                Toast.makeText(this, getErrorMessage(e), Toast.LENGTH_LONG).show()
            }
    }

    // ── Countdown gửi lại ─────────────────────────────────────────
    private fun startResendCountdown() {
        countDownTimer?.cancel()
        btnResend.isEnabled = false
        btnResend.alpha = 0.5f

        countDownTimer = object : CountDownTimer(60_000L, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                btnResend.text = "Không nhận được? Gửi lại (${sec}s)"
            }
            override fun onFinish() {
                btnResend.text = "Không nhận được? Gửi lại"
                btnResend.isEnabled = true
                btnResend.alpha = 1f
            }
        }.start()
    }

    // ── UI state ──────────────────────────────────────────────────
    private fun setLoading(isLoading: Boolean) {
        btnSend.isEnabled = !isLoading
        btnSend.text = if (isLoading) "Đang gửi..." else "GỬI LINK ĐẶT LẠI"
    }

    private fun showSuccess(email: String) {
        tvSuccessEmail.text = email
        layoutForm.visibility    = View.GONE
        layoutSuccess.visibility = View.VISIBLE
    }

    // ── Error message ─────────────────────────────────────────────
    private fun getErrorMessage(e: Exception): String {
        val msg = e.message?.lowercase() ?: ""
        return when {
            "badly formatted" in msg -> "Email không hợp lệ"
            "network" in msg         -> "Lỗi mạng, kiểm tra internet"
            else                     -> "Gửi thất bại, thử lại sau"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}