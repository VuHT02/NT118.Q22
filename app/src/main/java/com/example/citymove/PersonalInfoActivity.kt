package com.example.citymove

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class PersonalInfoActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private lateinit var tvAvatarInitials: TextView
    private lateinit var tvDisplayName:   TextView
    private lateinit var tvDisplayEmail:  TextView
    private lateinit var tvName:          TextView
    private lateinit var tvEmail:         TextView
    private lateinit var tvCccd:          TextView
    private lateinit var edtName:         EditText
    private lateinit var edtCccd:         EditText
    private lateinit var btnSave:         Button
    private lateinit var progressBar:     ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_personal_info)
        supportActionBar?.hide()

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)
        tvDisplayName    = findViewById(R.id.tvDisplayName)
        tvDisplayEmail   = findViewById(R.id.tvDisplayEmail)
        tvName           = findViewById(R.id.tvName)
        tvEmail          = findViewById(R.id.tvEmail)
        tvCccd           = findViewById(R.id.tvCccd)
        edtName          = findViewById(R.id.edtName)
        edtCccd          = findViewById(R.id.edtCccd)
        btnSave          = findViewById(R.id.btnSave)
        progressBar      = findViewById(R.id.progressBar)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        btnSave.setOnClickListener { saveChanges() }

        loadUserData()
    }

    private fun loadUserData() {
        val uid   = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""

        progressBar.visibility = View.VISIBLE

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE

                val name = doc.getString("name")?.takeIf { it.isNotEmpty() } ?: ""
                val cccd = doc.getString("cccd") ?: ""

                // Cập nhật avatar + header card
                tvDisplayName.text  = name.ifEmpty { "Chưa cập nhật" }
                tvDisplayEmail.text = email
                updateInitials(name)

                // Cập nhật info rows
                tvName.text  = name.ifEmpty { "Chưa cập nhật" }
                tvEmail.text = email.ifEmpty { "Chưa cập nhật" }
                tvCccd.text  = if (cccd.isNotEmpty()) maskCccd(cccd) else "Chưa cập nhật"

                // Điền sẵn vào ô chỉnh sửa
                edtName.setText(name)
                edtCccd.setText(cccd)
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Không thể tải thông tin", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveChanges() {
        val uid  = auth.currentUser?.uid ?: return
        val name = edtName.text.toString().trim()
        val cccd = edtCccd.text.toString().trim()

        if (name.isEmpty()) {
            edtName.error = "Vui lòng nhập họ và tên"
            return
        }
        if (cccd.isNotEmpty() && cccd.length != 12) {
            edtCccd.error = "Số CCCD phải đủ 12 chữ số"
            return
        }

        btnSave.isEnabled = false
        btnSave.text = "Đang lưu..."

        db.collection("users").document(uid)
            .set(mapOf("name" to name, "cccd" to cccd), SetOptions.merge())
            .addOnSuccessListener {
                // Cập nhật lại hiển thị
                tvDisplayName.text = name
                tvName.text = name
                tvCccd.text = if (cccd.isNotEmpty()) maskCccd(cccd) else "Chưa cập nhật"
                updateInitials(name)

                Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Lưu thay đổi"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
                btnSave.isEnabled = true
                btnSave.text = "Lưu thay đổi"
            }
    }

    private fun updateInitials(name: String) {
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
        tvAvatarInitials.text = if (initials.isNotEmpty()) initials else "U"
    }

    // Che giữa CCCD: 123456***901
    private fun maskCccd(cccd: String): String {
        return if (cccd.length >= 12)
            cccd.take(6) + "***" + cccd.takeLast(3)
        else cccd
    }
}
