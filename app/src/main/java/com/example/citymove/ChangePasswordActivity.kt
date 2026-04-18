package com.example.citymove

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        supportActionBar?.hide()

        val edtCurrent   = findViewById<EditText>(R.id.edtCurrentPassword)
        val edtNew       = findViewById<EditText>(R.id.edtNewPassword)
        val edtConfirm   = findViewById<EditText>(R.id.edtConfirmPassword)
        val btnUpdate    = findViewById<Button>(R.id.btnChangePassword)
        val btnBack      = findViewById<ImageButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnUpdate.setOnClickListener {
            val current = edtCurrent.text.toString()
            val newPass = edtNew.text.toString()
            val confirm = edtConfirm.text.toString()

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirm) {
                Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null && user.email != null) {
                val credential = EmailAuthProvider.getCredential(user.email!!, current)

                // Re-authenticate user
                user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                    if (reAuthTask.isSuccessful) {
                        user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                finish()
                            } else {
                                Toast.makeText(this, "Lỗi: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
