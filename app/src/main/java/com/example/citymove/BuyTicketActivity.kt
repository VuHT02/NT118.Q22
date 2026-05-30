package com.example.citymove

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BuyTicketActivity : AppCompatActivity() {

    // Views
    private lateinit var btnBack: FrameLayout
    private lateinit var btnHelp: FrameLayout
    private lateinit var btnTicketSingle: LinearLayout
    private lateinit var btnTicketMonth: LinearLayout
    private lateinit var containerSingleTicket: LinearLayout
    private lateinit var btnBusSingle: LinearLayout
    private lateinit var btnMetroSingle: LinearLayout
    private lateinit var btnWaterbusSwingle: LinearLayout
    private lateinit var btnMinus: FrameLayout
    private lateinit var btnPlus: FrameLayout
    private lateinit var tvQuantity: TextView
    private lateinit var tvSubtotal: TextView
    private lateinit var tvFee: TextView
    private lateinit var tvTotal: TextView
    private lateinit var cardWalletPayment: MaterialCardView
    private lateinit var cardCreditCard: MaterialCardView
    private lateinit var cardEWallet: MaterialCardView
    private lateinit var containerPaymentMethod: LinearLayout
    private lateinit var cardQuantityPrice: CardView
    private lateinit var tvWalletBalance: TextView
    private lateinit var btnCheckout: TextView
    private lateinit var btnContinueShopping: TextView
    private lateinit var fabAI: FrameLayout

    // Firebase
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // State
    private var selectedTicketType = "single"
    private var selectedTransport = "bus"
    private var quantity = 1
    private var basePrice = 7000
    private val serviceFee = 500
    private var selectedPaymentMethod = "wallet"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_ticket)
        
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        initViews()
        setupListeners()
        updatePrice()
        setupBottomNav()
        selectPaymentMethod("wallet")
        loadWalletBalance()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnHelp = findViewById(R.id.btnHelp)
        btnTicketSingle = findViewById(R.id.btnTicketSingle)
        btnTicketMonth = findViewById(R.id.btnTicketMonth)
        containerSingleTicket = findViewById(R.id.containerSingleTicket)
        btnBusSingle = findViewById(R.id.btnBusSingle)
        btnMetroSingle = findViewById(R.id.btnMetroSingle)
        btnWaterbusSwingle = findViewById(R.id.btnWaterbusSwingle)
        btnMinus = findViewById(R.id.btnMinus)
        btnPlus = findViewById(R.id.btnPlus)
        tvQuantity = findViewById(R.id.tvQuantity)
        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvFee = findViewById(R.id.tvFee)
        tvTotal = findViewById(R.id.tvTotal)
        cardWalletPayment = findViewById(R.id.cardWalletPayment)
        cardCreditCard = findViewById(R.id.cardCreditCard)
        cardEWallet = findViewById(R.id.cardEWallet)
        containerPaymentMethod = findViewById(R.id.containerPaymentMethod)
        btnCheckout = findViewById(R.id.btnCheckout)
        btnContinueShopping = findViewById(R.id.btnContinueShopping)
        fabAI = findViewById(R.id.fabAI)
        cardQuantityPrice = findViewById(R.id.cardQuantityPrice)
        tvWalletBalance = findViewById(R.id.tvWalletBalance)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener { finish() }
        btnHelp.setOnClickListener { Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show() }
        btnTicketSingle.setOnClickListener { selectTicketType("single") }
        btnTicketMonth.setOnClickListener { selectTicketType("month") }
        btnBusSingle.setOnClickListener { selectTransport("bus", 7000) }
        btnMetroSingle.setOnClickListener { selectTransport("metro", 6000) }
        btnWaterbusSwingle.setOnClickListener { selectTransport("waterbus", 15000) }
        btnMinus.setOnClickListener { decreaseQuantity() }
        btnPlus.setOnClickListener { increaseQuantity() }
        cardWalletPayment.setOnClickListener { selectPaymentMethod("wallet") }
        cardCreditCard.setOnClickListener { selectPaymentMethod("card") }
        cardEWallet.setOnClickListener { selectPaymentMethod("ewallet") }
        btnCheckout.setOnClickListener { checkout() }
        btnContinueShopping.setOnClickListener { finish() }
        fabAI.setOnClickListener { Toast.makeText(this, "AI Assistant đang khởi động...", Toast.LENGTH_SHORT).show() }
    }

    private fun selectTicketType(type: String) {
        selectedTicketType = type
        if (type == "single") {
            containerSingleTicket.visibility = View.VISIBLE
            cardQuantityPrice.visibility = View.VISIBLE
            btnTicketSingle.setBackgroundResource(R.drawable.bg_tab_selected)
            btnTicketMonth.setBackgroundResource(R.drawable.bg_tab_unselected)
            (btnTicketSingle.getChildAt(1) as? TextView)?.setTextColor(getColor(R.color.orange_primary))
            (btnTicketMonth.getChildAt(1) as? TextView)?.setTextColor(getColor(R.color.text_hint))
            basePrice = if (selectedTransport == "bus") 7000 else if (selectedTransport == "metro") 6000 else 15000
        } else {
            containerSingleTicket.visibility = View.GONE
            cardQuantityPrice.visibility = View.GONE
            quantity = 1
            tvQuantity.text = "1"
            btnTicketSingle.setBackgroundResource(R.drawable.bg_tab_unselected)
            btnTicketMonth.setBackgroundResource(R.drawable.bg_tab_selected)
            (btnTicketSingle.getChildAt(1) as? TextView)?.setTextColor(getColor(R.color.text_hint))
            (btnTicketMonth.getChildAt(1) as? TextView)?.setTextColor(getColor(R.color.orange_primary))
            basePrice = 200000
        }
        updatePrice()
    }

    private fun selectTransport(transport: String, price: Int) {
        selectedTransport = transport
        basePrice = price
        btnBusSingle.setBackgroundResource(if (transport == "bus") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        btnMetroSingle.setBackgroundResource(if (transport == "metro") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        btnWaterbusSwingle.setBackgroundResource(if (transport == "waterbus") R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected)
        
        (btnBusSingle.getChildAt(1) as? TextView)?.setTextColor(if (transport == "bus") getColor(R.color.orange_primary) else getColor(R.color.text_hint))
        (btnMetroSingle.getChildAt(1) as? TextView)?.setTextColor(if (transport == "metro") getColor(R.color.blue_primary) else getColor(R.color.text_hint))
        (btnWaterbusSwingle.getChildAt(1) as? TextView)?.setTextColor(if (transport == "waterbus") getColor(R.color.green_500) else getColor(R.color.text_hint))
        
        updatePrice()
    }

    private fun increaseQuantity() {
        if (quantity < 10) {
            quantity++
            tvQuantity.text = quantity.toString()
            updatePrice()
        }
    }

    private fun decreaseQuantity() {
        if (quantity > 1) {
            quantity--
            tvQuantity.text = quantity.toString()
            updatePrice()
        }
    }

    private fun updatePrice() {
        val subtotal = basePrice * quantity
        val totalAmount = subtotal + serviceFee
        tvSubtotal.text = formatPrice(subtotal)
        tvFee.text = formatPrice(serviceFee)
        tvTotal.text = formatPrice(totalAmount)
        btnCheckout.text = "Thanh Toán ${formatPrice(totalAmount)}"
    }

    private fun selectPaymentMethod(method: String) {
        selectedPaymentMethod = method
        cardWalletPayment.strokeWidth = if (method == "wallet") 4 else 0
        cardCreditCard.strokeWidth = if (method == "card") 4 else 0
        cardEWallet.strokeWidth = if (method == "ewallet") 4 else 0
    }

    private fun loadWalletBalance() {
        val user = auth.currentUser ?: return
        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val balance = doc.getLong("balance") ?: 0L
            tvWalletBalance.text = "Số dư: ${formatPrice(balance.toInt())}"
        }
    }

    private fun checkout() {
        if (selectedPaymentMethod != "wallet") {
            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show()
            return
        }
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = (basePrice * quantity) + serviceFee
        btnCheckout.isEnabled = false
        btnCheckout.text = "Đang xử lý..."

        db.collection("users").document(user.uid).get().addOnSuccessListener { doc ->
            val balance = doc.getLong("balance") ?: 0L
            if (balance < totalAmount) {
                btnCheckout.isEnabled = true
                updatePrice()
                Toast.makeText(this, "Số dư ví không đủ, vui lòng nạp thêm!", Toast.LENGTH_LONG).show()
                startActivity(Intent(this, CardDetailActivity::class.java))
                return@addOnSuccessListener
            }

            val batch = db.batch()
            val userRef = db.collection("users").document(user.uid)
            batch.update(userRef, "balance", balance - totalAmount)

            val ticketCode = (1..10).map { "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }.joinToString("")
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val transportName = when(selectedTransport) { "bus" -> "Xe Buýt"; "metro" -> "Metro"; else -> "Buýt Sông" }
            val typeName = if (selectedTicketType == "single") "Vé Lẻ" else "Vé Tháng"

            val transaction = mapOf(
                "title" to "$typeName $transportName",
                "amount" to totalAmount.toLong(),
                "type" to "PAYMENT",
                "timestamp" to System.currentTimeMillis(),
                "date" to dateStr,
                "routeName" to "$transportName · $typeName",
                "quantity" to quantity,
                "ticketCode" to ticketCode,
                "ticketType" to selectedTicketType,
                "transport" to selectedTransport
            )
            batch.set(userRef.collection("transactions").document(), transaction)

            batch.commit().addOnSuccessListener {
                db.collection("users").document(user.uid)
                    .collection("notifications")
                    .add(mapOf(
                        "title"       to "Mua vé thành công",
                        "description" to "Đã mua $quantity $typeName $transportName · ${formatPrice(totalAmount)}",
                        "type"        to "trip",
                        "isRead"      to false,
                        "timestamp"   to System.currentTimeMillis()
                    ))

                val intent = Intent(this, TicketQrActivity::class.java).apply {
                    putExtra(TicketQrActivity.EXTRA_TICKET_CODE, ticketCode)
                    putExtra(TicketQrActivity.EXTRA_ROUTE_NAME, "$transportName - $typeName")
                    putExtra(TicketQrActivity.EXTRA_QUANTITY, quantity)
                    putExtra(TicketQrActivity.EXTRA_TOTAL_PRICE, totalAmount)
                    putExtra(TicketQrActivity.EXTRA_DATE, dateStr)
                }
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                btnCheckout.isEnabled = true
                updatePrice()
                Toast.makeText(this, "Lỗi: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatPrice(amount: Int): String = String.format("%,d₫", amount).replace(",", ".")

    private fun setupBottomNav() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNav)
        bottomNav.setOnItemSelectedListener { item ->
            if (item.itemId == R.id.nav_home) { finish(); true } else false
        }
    }
}
