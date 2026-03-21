package com.transgo.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transgo.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ForgotPasswordScreen(
    onSendOtp: (String) -> Unit,
    onBack: () -> Unit,
    onGoLogin: () -> Unit,
) {
    var tab     by remember { mutableStateOf(0) }
    var value   by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        isError = value.isBlank() || (tab == 1 && !value.contains("@"))
        return !isError
    }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            BackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1AFF7A1F))
                    .border(1.dp, Color(0x33FF7A1F), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔑", fontSize = 38.sp)
            }
            Spacer(Modifier.height(18.dp))

            Text(
                "Quên mật khẩu?",
                style = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Nhập số điện thoại hoặc email đã đăng ký —\nchúng tôi sẽ gửi mã xác nhận cho bạn.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                ),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            AuthCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0FFFFFFF))
                        .padding(4.dp)
                ) {
                    listOf("Số điện thoại", "Email").forEachIndexed { idx, label ->
                        val selected = tab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (selected) Brush.horizontalGradient(listOf(Accent, Accent2))
                                    else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { tab = idx; value = "" }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else Color(0x66FFFFFF)
                                )
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                TransGoInput(
                    value = value,
                    onValueChange = { value = it; isError = false },
                    label = if (tab == 0) "Số điện thoại" else "Email",
                    placeholder = if (tab == 0) "0901 234 567" else "email@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (tab == 0) KeyboardType.Phone else KeyboardType.Email
                    ),
                    leadingIcon = { Text(if (tab == 0) "📱" else "✉️", fontSize = 14.sp) },
                    isError = isError,
                    errorMessage = if (tab == 0) "Vui lòng nhập số điện thoại hợp lệ" else "Email không hợp lệ"
                )
                Spacer(Modifier.height(20.dp))

                PrimaryButton(
                    text = "Gửi mã xác nhận  →",
                    onClick = { if (validate()) onSendOtp(value) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Mã OTP có hiệu lực trong 5 phút",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = TextStyle(fontSize = 11.sp, color = Color(0x40FFFFFF))
                )
            }

            Spacer(Modifier.height(20.dp))
            Row {
                Text("Nhớ mật khẩu rồi? ", style = TextStyle(fontSize = 13.sp, color = Color(0x59FFFFFF)))
                Text(
                    "Đăng nhập",
                    style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Accent),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoLogin
                    )
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun OtpScreen(
    maskedContact: String = "0901 234 5**",
    onVerified: () -> Unit,
    onBack: () -> Unit,
    onResend: () -> Unit = {},
) {
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    var countdown by remember { mutableStateOf(120) }
    var isError   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    val minutes = countdown / 60
    val seconds = countdown % 60
    val timerText = "%02d:%02d".format(minutes, seconds)

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            BackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1A00C6A2))
                    .border(1.dp, Color(0x3300C6A2), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 38.sp)
            }
            Spacer(Modifier.height(18.dp))

            Text(
                "Nhập mã OTP",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Mã xác nhận đã gửi đến\n$maskedContact",
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            )
            Spacer(Modifier.height(28.dp))

            AuthCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(6) { idx ->
                        val filled = otpValues[idx].isNotEmpty()
                        val isActive = !filled && (idx == 0 || otpValues[idx - 1].isNotEmpty())
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    when {
                                        isError  -> Color(0x1AFF4B7B)
                                        filled   -> Color(0x0FFF7A1F)
                                        isActive -> Color(0x0FFFFFFF)
                                        else     -> Color(0x0AFFFFFF)
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        isError  -> ErrorRed
                                        isActive -> Accent
                                        filled   -> Accent
                                        else     -> Color(0x1AFFFFFF)
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = otpValues[idx],
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }
                    }
                }

                if (isError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Mã OTP không đúng. Vui lòng thử lại.",
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontSize = 11.sp, color = ErrorRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Gửi lại mã sau ",
                        style = TextStyle(fontSize = 12.sp, color = Color(0x59FFFFFF))
                    )
                    Text(
                        timerText,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (countdown > 0) Accent else Gray
                        )
                    )
                }

                Spacer(Modifier.height(20.dp))

                PrimaryButton(
                    text = "Xác nhận  ✓",
                    onClick = {
                        if (otpValues.all { it.isNotEmpty() }) onVerified()
                        else isError = true
                    }
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Không nhận được? ", style = TextStyle(fontSize = 12.sp, color = Color(0x4DFFFFFF)))
                    Text(
                        "Gửi lại",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (countdown == 0) Accent else Gray
                        ),
                        modifier = Modifier.clickable(
                            enabled = countdown == 0,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onResend(); countdown = 120 }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ResetPasswordScreen(
    onResetSuccess: () -> Unit,
    onBack: () -> Unit,
) {
    var password     by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }
    var passError    by remember { mutableStateOf(false) }
    var confirmError by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        passError    = password.length < 8
        confirmError = confirmPass != password
        return !passError && !confirmError
    }

    data class Hint(val text: String, val fulfilled: Boolean)
    val hints = listOf(
        Hint("Ít nhất 8 ký tự",             password.length >= 8),
        Hint("Có chữ hoa và chữ thường",     password.any { it.isUpperCase() } && password.any { it.isLowerCase() }),
        Hint("Có ký tự đặc biệt (!@#\$...)", password.any { !it.isLetterOrDigit() })
    )

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(56.dp))
            BackButton(onClick = onBack)
            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0x1A1A3A6B))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔐", fontSize = 38.sp)
            }
            Spacer(Modifier.height(18.dp))

            Text(
                "Đặt lại mật khẩu",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Tạo mật khẩu mới cho tài khoản của bạn",
                style = TextStyle(fontSize = 12.sp, color = Gray, textAlign = TextAlign.Center)
            )
            Spacer(Modifier.height(28.dp))

            AuthCard {
                PasswordInput(
                    value = password,
                    onValueChange = { password = it; passError = false },
                    label = "Mật khẩu mới",
                    placeholder = "Nhập mật khẩu mới",
                    isError = passError,
                    errorMessage = "Mật khẩu phải có ít nhất 8 ký tự"
                )
                if (password.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    StrengthBar(password = password)
                }
                Spacer(Modifier.height(16.dp))

                PasswordInput(
                    value = confirmPass,
                    onValueChange = { confirmPass = it; confirmError = false },
                    label = "Xác nhận mật khẩu",
                    placeholder = "Nhập lại mật khẩu",
                    isError = confirmError,
                    errorMessage = "Mật khẩu không khớp"
                )
                Spacer(Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x0AFFFFFF))
                        .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "YÊU CẦU MẬT KHẨU",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0x59FFFFFF),
                                letterSpacing = 1.sp
                            )
                        )
                        hints.forEach { hint ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (hint.fulfilled) "✓" else "○",
                                    style = TextStyle(
                                        fontSize = 12.sp,
                                        color = if (hint.fulfilled) Teal else Color(0x33FFFFFF),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    hint.text,
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = if (hint.fulfilled) Teal else Color(0x59FFFFFF)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                PrimaryButton(
                    text = "Cập nhật mật khẩu  ✓",
                    onClick = { if (validate()) onResetSuccess() }
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
