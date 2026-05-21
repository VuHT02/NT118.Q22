package com.example.citymove

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

class TicketQrActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TICKET_CODE  = "ticket_code"
        const val EXTRA_ROUTE_NAME   = "route_name"
        const val EXTRA_QUANTITY     = "quantity"
        const val EXTRA_TOTAL_PRICE  = "total_price"
        const val EXTRA_DATE         = "date"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ticket_qr)
        supportActionBar?.hide()

        val ticketCode = intent.getStringExtra(EXTRA_TICKET_CODE) ?: ""
        val routeName  = intent.getStringExtra(EXTRA_ROUTE_NAME)  ?: ""
        val quantity   = intent.getIntExtra(EXTRA_QUANTITY, 1)
        val total      = intent.getIntExtra(EXTRA_TOTAL_PRICE, 0)
        val date       = intent.getStringExtra(EXTRA_DATE) ?: ""

        findViewById<TextView>(R.id.tvTicketRoute).text = routeName
        findViewById<TextView>(R.id.tvTicketCode).text  = "Mã vé: $ticketCode"
        findViewById<TextView>(R.id.tvTicketQty).text   = "$quantity vé"
        findViewById<TextView>(R.id.tvTicketTotal).text = formatPrice(total)
        findViewById<TextView>(R.id.tvTicketDate).text  = date

        val qrContent = "CITYMOVE:$ticketCode:$quantity:$total"
        generateQr(qrContent)?.let { bitmap ->
            findViewById<ImageView>(R.id.ivQrCode).setImageBitmap(bitmap)
        }

        findViewById<ImageButton>(R.id.btnClose).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    private fun generateQr(content: String): Bitmap? {
        return try {
            val hints = mapOf(EncodeHintType.MARGIN to 1)
            val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) {
                for (y in 0 until 512) {
                    bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            null
        }
    }

    private fun formatPrice(amount: Int): String = String.format("%,dđ", amount).replace(",", ".")
}
