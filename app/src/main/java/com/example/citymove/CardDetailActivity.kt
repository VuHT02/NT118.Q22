package com.example.citymove

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CardDetailActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: FrameLayout
    private lateinit var tvCurrentBalance: TextView
    private lateinit var tvTopUpAmount: TextView
    private lateinit var etCustomAmount: EditText
    private lateinit var btnConfirm: TextView
    private lateinit var quickAmountViews: List<TextView>

    private val quickAmounts = listOf(50_000L, 100_000L, 200_000L, 500_000L, 1_000_000L)
    private var selectedAmount = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_card_detail)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        initViews()
        setupListeners()
        loadBalance()
    }

    private fun initViews() {
        btnBack          = findViewById(R.id.btnBack)
        tvCurrentBalance = findViewById(R.id.tvCurrentBalance)
        tvTopUpAmount    = findViewById(R.id.tvTopUpAmount)
        etCustomAmount   = findViewById(R.id.etCustomAmount)
        btnConfirm       = findViewById(R.id.btnConfirm)
        quickAmountViews = listOf(
            findViewById(R.id.btn50k),
            findViewById(R.id.btn100k),
            findViewById(R.id.btn200k),
            findViewById(R.id.btn500k),
            findViewById(R.id.btn1m)
        )
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }

        quickAmounts.forEachIndexed { i, amount ->
            quickAmountViews[i].setOnClickListener {
                selectedAmount = amount
                etCustomAmount.setText(amount.toString())
                etCustomAmount.clearFocus()
                updateHighlight(i)
                updateSummary()
            }
        }

        etCustomAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                selectedAmount = s?.toString()?.toLongOrNull() ?: 0L
                updateHighlight(quickAmounts.indexOf(selectedAmount))
                updateSummary()
            }
        })

        btnConfirm.setOnClickListener { topUp() }
    }

    private fun updateHighlight(selectedIndex: Int) {
        quickAmountViews.forEachIndexed { i, tv ->
            val sel = i == selectedIndex
            tv.setBackgroundResource(if (sel) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
            tv.setTextColor(if (sel) getColor(R.color.orange_primary) else getColor(R.color.text_hint))
        }
    }

    private fun updateSummary() {
        tvTopUpAmount.text = if (selectedAmount > 0) formatAmount(selectedAmount) else "0₫"
    }

    private fun loadBalance() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            tvCurrentBalance.text = formatAmount(doc.getLong("balance") ?: 0L)
        }
    }

    private fun topUp() {
        if (selectedAmount < 10_000) {
            Toast.makeText(this, "Số tiền nạp tối thiểu là 10.000₫", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirm.isEnabled = false
        btnConfirm.text = "Đang xử lý..."

        val userRef = db.collection("users").document(uid)
        db.runTransaction { tx ->
            val balance = tx.get(userRef).getLong("balance") ?: 0L
            tx.update(userRef, "balance", balance + selectedAmount)

            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            tx.set(userRef.collection("transactions").document(), mapOf(
                "title"     to "Nạp tiền vào ví",
                "amount"    to selectedAmount,
                "type"      to "TOP_UP",
                "timestamp" to System.currentTimeMillis(),
                "date"      to dateStr
            ))
        }.addOnSuccessListener {
            db.collection("users").document(uid)
                .collection("notifications")
                .add(mapOf(
                    "title"       to "Nạp tiền thành công",
                    "description" to "Đã nạp ${formatAmount(selectedAmount)} vào ví TransGo",
                    "type"        to "system",
                    "isRead"      to false,
                    "timestamp"   to System.currentTimeMillis()
                ))

            Toast.makeText(this, "Nạp ${formatAmount(selectedAmount)} thành công!", Toast.LENGTH_LONG).show()
            finish()
        }.addOnFailureListener {
            btnConfirm.isEnabled = true
            btnConfirm.text = "XÁC NHẬN NẠP TIỀN"
            Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatAmount(amount: Long): String =
        String.format("%,d₫", amount).replace(",", ".")
}
