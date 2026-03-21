package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// ===================== ForgotPasswordActivity.kt =====================
class ForgotPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpasswordscreens)
        supportActionBar?.hide()

        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvGoLogin).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnSendOtp).setOnClickListener {
            // TODO: gọi API gửi OTP
            val email = findViewById<android.widget.EditText>(R.id.etEmail).text.toString()
            val intent = Intent(this, OtpVerifyActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
        }
    }
}


// ===================== OtpVerifyActivity.kt =====================
class OtpVerifyActivity : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.otp)
        supportActionBar?.hide()

        val email = intent.getStringExtra("email") ?: ""
        findViewById<TextView>(R.id.tvEmailHint).text = "Mã đã gửi đến $email"

        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener { finish() }

        startResendTimer()

        findViewById<TextView>(R.id.tvResend).setOnClickListener {
            // TODO: gọi API gửi lại OTP
            startResendTimer()
        }

        findViewById<Button>(R.id.btnConfirmOtp).setOnClickListener {
            // TODO: xác nhận OTP
            startActivity(Intent(this, NewPasswordActivity::class.java))
        }
    }

    private fun startResendTimer() {
        val tvResend = findViewById<TextView>(R.id.tvResend)
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(59000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                tvResend.text = "Gửi lại (${millisUntilFinished / 1000}s)"
                tvResend.isEnabled = false
                tvResend.alpha = 0.5f
            }
            override fun onFinish() {
                tvResend.text = "Gửi lại"
                tvResend.isEnabled = true
                tvResend.alpha = 1.0f
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}


// ===================== NewPasswordActivity.kt =====================
class NewPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.newpassword)
        supportActionBar?.hide()

        findViewById<LinearLayout>(R.id.btnBack).setOnClickListener { finish() }

        val etNew = findViewById<android.widget.EditText>(R.id.etNewPassword)
        val bars = listOf(
            findViewById<android.view.View>(R.id.strengthBar1),
            findViewById<android.view.View>(R.id.strengthBar2),
            findViewById<android.view.View>(R.id.strengthBar3),
            findViewById<android.view.View>(R.id.strengthBar4)
        )
        val tvStrength = findViewById<TextView>(R.id.tvStrengthLabel)

        etNew.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val len = s?.length ?: 0
                val strength = when {
                    len == 0 -> 0
                    len < 4 -> 1
                    len < 8 -> 2
                    len < 12 -> 3
                    else -> 4
                }
                bars.forEachIndexed { i, bar ->
                    bar.setBackgroundResource(
                        if (i < strength) R.drawable.bg_strength_active
                        else R.drawable.bg_strength_inactive
                    )
                }
                tvStrength.text = when (strength) {
                    0 -> ""
                    1 -> "Yếu"
                    2 -> "Trung bình"
                    3 -> "Mạnh"
                    else -> "Rất mạnh"
                }
                tvStrength.setTextColor(when (strength) {
                    1 -> android.graphics.Color.parseColor("#ef4444")
                    2 -> android.graphics.Color.parseColor("#F97316")
                    3 -> android.graphics.Color.parseColor("#22c55e")
                    4 -> android.graphics.Color.parseColor("#16a34a")
                    else -> android.graphics.Color.parseColor("#9aa5b4")
                })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.btnResetPassword).setOnClickListener {
            // TODO: gọi API đặt lại mật khẩu
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }
    }
}