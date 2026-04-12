package com.example.citymove.ui.auth

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.citymove.R
import com.example.citymove.databinding.FragmentOtpBinding

class OtpFragment : Fragment() {

    private var _binding: FragmentOtpBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private val OTP_TIMEOUT_MS = 60_000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val otpFields = listOf(
            binding.etOtp1, binding.etOtp2, binding.etOtp3,
            binding.etOtp4, binding.etOtp5, binding.etOtp6
        )

        setupOtpInputs(otpFields)
        startCountdown()

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.tvResend.setOnClickListener {
            if (binding.tvResend.isEnabled) {
                // TODO: gọi API gửi lại OTP
                otpFields.forEach { it.setText("") }
                otpFields[0].requestFocus()
                hideError()
                startCountdown()
            }
        }

        binding.btnConfirmOtp.setOnClickListener {
            val otp = otpFields.joinToString("") { it.text.toString() }
            if (otp.length < 6) {
                showError("Vui lòng nhập đủ 6 chữ số")
                return@setOnClickListener
            }
            verifyOtp(otp)
        }
    }

    // ─── OTP box logic ───────────────────────────────────────────────

    private fun setupOtpInputs(fields: List<EditText>) {
        fields.forEachIndexed { index, editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    hideError()
                    updateBoxAppearance(fields)
                    if (!s.isNullOrEmpty()) {
                        // Nhảy sang ô tiếp
                        if (index < fields.size - 1) {
                            fields[index + 1].requestFocus()
                        } else {
                            // Đã nhập đủ 6 số → ẩn bàn phím + tự submit
                            editText.clearFocus()
                            autoSubmitIfComplete(fields)
                        }
                    }
                }
            })

            // Backspace → quay về ô trước
            editText.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL
                    && event.action == KeyEvent.ACTION_DOWN
                    && editText.text.isEmpty()
                    && index > 0
                ) {
                    fields[index - 1].apply {
                        setText("")
                        requestFocus()
                    }
                    true
                } else false
            }
        }
    }

    private fun updateBoxAppearance(fields: List<EditText>) {
        val focused = fields.indexOfFirst { it.isFocused }
        fields.forEachIndexed { i, et ->
            et.background = when {
                i == focused -> ContextCompat.getDrawable(requireContext(), R.drawable.bg_otp_box_active)
                et.text.isNotEmpty() -> ContextCompat.getDrawable(requireContext(), R.drawable.bg_otp_box)
                else -> ContextCompat.getDrawable(requireContext(), R.drawable.bg_otp_box_empty)
            }
        }
    }

    private fun autoSubmitIfComplete(fields: List<EditText>) {
        val otp = fields.joinToString("") { it.text.toString() }
        if (otp.length == 6) {
            verifyOtp(otp)
        }
    }

    // ─── Timer ───────────────────────────────────────────────────────

    private fun startCountdown() {
        countDownTimer?.cancel()
        binding.tvResend.isEnabled = false
        binding.tvResend.alpha = 0.5f
        binding.progressTimer.max = (OTP_TIMEOUT_MS / 1000).toInt()
        binding.progressTimer.progress = (OTP_TIMEOUT_MS / 1000).toInt()

        countDownTimer = object : CountDownTimer(OTP_TIMEOUT_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.tvResend.text = "Gửi lại (${seconds}s)"
                binding.progressTimer.progress = seconds.toInt()
            }

            override fun onFinish() {
                binding.tvResend.text = "Gửi lại"
                binding.tvResend.isEnabled = true
                binding.tvResend.alpha = 1f
                binding.progressTimer.progress = 0
            }
        }.start()
    }

    // ─── API call ────────────────────────────────────────────────────

    private fun verifyOtp(otp: String) {
        // TODO: gọi ViewModel / API xác thực OTP
        // Khi thành công:
        // findNavController().navigate(R.id.action_otpFragment_to_homeFragment)
    }

    // ─── Error ───────────────────────────────────────────────────────

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
