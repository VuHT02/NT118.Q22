package com.example.citymove

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PersonalInfoActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var tvAvatarInitials: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)
        supportActionBar?.hide()

        initViews()
        loadUserData()

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveUserData() }
    }

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail    = findViewById(R.id.edtEmail)
        edtPhone    = findViewById(R.id.edtPhone)
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val name  = doc.getString("name") ?: ""
                    val email = doc.getString("email") ?: auth.currentUser?.email ?: ""
                    val phone = doc.getString("phone") ?: ""

                    edtFullName.setText(name)
                    edtEmail.setText(email)
                    edtPhone.setText(phone)

                    val initials = name.trim().split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .joinToString("") { it.first().uppercase() }
                    tvAvatarInitials.text = if (initials.isNotEmpty()) initials else "U"
                }
            }
    }

    private fun saveUserData() {
        val uid = auth.currentUser?.uid ?: return
        val name  = edtFullName.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập họ tên", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "name" to name,
            "phone" to phone
        )

        db.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
