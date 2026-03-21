package com.transgo.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transgo.app.ui.theme.*

/**
 * Màn ĐĂNG NHẬP
 *
 * @param onLoginSuccess     callback khi nhấn nút Đăng nhập
 * @param onForgotPassword   callback đến màn Quên mật khẩu
 * @param onGoRegister       callback đến màn Đăng ký
 * @param onGoogleLogin      callback đăng nhập Google
 * @param onFacebookLogin    callback đăng nhập Facebook
 */
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit,
    onGoRegister: () -> Unit,
    onGoogleLogin: () -> Unit = {},
    onFacebookLogin: () -> Unit = {},
) {
    var phone    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var remember by remember { mutableStateOf(true) }
    var phoneError by remember { mutableStateOf(false) }
    var passError  by remember { mutableStateOf(false) }

    fun validate(): Boolean {
        phoneError = phone.isBlank()
        passError  = password.length < 6
        return !phoneError && !passError
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

            // ── Logo ──────────────────────────────────────────────
            TransGoLogoRow()

            Spacer(Modifier.height(32.dp))

            // ── Card ──────────────────────────────────────────────
            AuthCard {
                Text(
                    "Chào mừng trở lại!",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Đăng nhập để tiếp tục hành trình của bạn",
                    style = TextStyle(fontSize = 12.sp, color = Gray)
                )
                Spacer(Modifier.height(24.dp))

                // SĐT / Email
                TransGoInput(
                    value = phone,
                    onValueChange = { phone = it; phoneError = false },
                    label = "Số điện thoại hoặc Email",
                    placeholder = "0901 234 567 hoặc email@...",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Text("👤", fontSize = 14.sp) },
                    isError = phoneError,
                    errorMessage = "Vui lòng nhập số điện thoại hoặc email"
                )
                Spacer(Modifier.height(16.dp))

                // Mật khẩu
                PasswordInput(
                    value = password,
                    onValueChange = { password = it; passError = false },
                    isError = passError,
                    errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                )
                Spacer(Modifier.height(16.dp))

                // Remember / Forgot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = remember,
                            onCheckedChange = { remember = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Accent,
                                uncheckedColor = Gray,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Ghi nhớ đăng nhập",
                            style = TextStyle(fontSize = 12.sp, color = Color(0x80FFFFFF))
                        )
                    }
                    Text(
                        "Quên mật khẩu?",
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        ),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onForgotPassword
                        )
                    )
                }
                Spacer(Modifier.height(20.dp))

                // CTA
                PrimaryButton(
                    text = "Đăng nhập  →",
                    onClick = {
                        if (validate()) onLoginSuccess()
                    }
                )
                Spacer(Modifier.height(20.dp))

                AuthDivider()
                Spacer(Modifier.height(16.dp))

                // Social
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SocialButton(
                        icon = "G",
                        label = "Google",
                        onClick = onGoogleLogin,
                        modifier = Modifier.weight(1f)
                    )
                    SocialButton(
                        icon = "f",
                        label = "Facebook",
                        onClick = onFacebookLogin,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Footer
            Row {
                Text(
                    "Chưa có tài khoản? ",
                    style = TextStyle(fontSize = 13.sp, color = Color(0x59FFFFFF))
                )
                Text(
                    "Đăng ký ngay",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Accent
                    ),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onGoRegister
                    )
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
