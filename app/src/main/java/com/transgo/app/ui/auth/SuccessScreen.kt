package com.transgo.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transgo.app.ui.theme.*

@Composable
fun SuccessScreen(
    title: String = "Thành công! 🎉",
    subtitle: String = "Mật khẩu của bạn đã được cập nhật.\nĐăng nhập để tiếp tục hành trình!",
    maskedContact: String? = "0901 234 5**",
    ctaText: String = "Về trang đăng nhập  →",
    onCtaClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    var visible by remember { mutableStateOf(false) }
    val entryScale by animateFloatAsState(
        targetValue  = if (visible) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "entry"
    )
    LaunchedEffect(Unit) { visible = true }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(entryScale)
                    .size(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(ringScale)
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0x1A00C6A2))
                        .border(1.dp, Color(0x3300C6A2), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(Teal, Color(0xFF007A64)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✓", style = TextStyle(fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold))
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                title,
                style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                subtitle,
                style = TextStyle(fontSize = 14.sp, color = Gray, textAlign = TextAlign.Center, lineHeight = 22.sp),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            if (maskedContact != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x0DFFFFFF))
                        .border(1.dp, Color(0x14FFFFFF), RoundedCornerShape(20.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x1A00C6A2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👤", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tài khoản", style = TextStyle(fontSize = 10.sp, color = Color(0x66FFFFFF)))
                        Text(maskedContact, style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            PrimaryButton(text = ctaText, onClick = onCtaClick, modifier = Modifier.fillMaxWidth())
        }
    }
}
