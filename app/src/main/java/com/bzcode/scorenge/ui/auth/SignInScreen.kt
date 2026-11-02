package com.bzcode.scorenge.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bzcode.scorenge.R
import com.bzcode.scorenge.ui.theme.BrandOrange
import com.bzcode.scorenge.ui.theme.BrandOrangeDeep
import com.bzcode.scorenge.ui.theme.BrandOrangeLight
import com.bzcode.scorenge.ui.theme.BrandOrangePanel
import com.bzcode.scorenge.ui.theme.ButtonCream
import com.bzcode.scorenge.ui.theme.CreamPale
import com.bzcode.scorenge.ui.theme.CreamPaleBottom
import com.bzcode.scorenge.ui.theme.CreamPaleTop
import com.bzcode.scorenge.ui.theme.Fredoka
import com.bzcode.scorenge.ui.theme.ScorengeTheme

private val WaveHeight = 40.dp
private val ErrorInk = Color(0xFF9A2A0A)

@Composable
fun SignInRoute(viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SignInScreen(state = state, onSignIn = { viewModel.signInWithGoogle(context) })
}

/**
 * Sign-in, after the Stitch "Alternate Login (Reference Style)" design: a cream top with the brand
 * pill and board-game artwork, then an orange panel with a wavy top edge holding the headline and
 * the single Google sign-in button.
 */
@Composable
fun SignInScreen(state: SignInUiState, onSignIn: () -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val topGradient = if (dark) {
        listOf(Color(0xFF2A1D15), Color(0xFF1C1612), Color(0xFF241912))
    } else {
        listOf(CreamPaleTop, CreamPale, CreamPaleBottom)
    }
    val glow = if (dark) Color(0x1FFFB067) else Color(0x80FFE5B4)
    val accentText = if (dark) BrandOrangeLight else BrandOrangeDeep

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(topGradient))
            .drawBehind {
                // Two soft warm glows, as in the design's background.
                drawCircle(
                    Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset.Zero, radius = 160.dp.toPx()),
                    radius = 160.dp.toPx(),
                    center = Offset.Zero,
                )
                val right = Offset(size.width, size.height / 3)
                drawCircle(
                    Brush.radialGradient(listOf(glow, Color.Transparent), center = right, radius = 140.dp.toPx()),
                    radius = 140.dp.toPx(),
                    center = right,
                )
            },
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp),
        ) {
            BrandPill(dark = dark, textColor = accentText)
            SignInArtwork(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            )
            Text(
                text = "THE GAME NIGHT SCORE TRACKER",
                color = accentText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SignInPanel(state = state, onSignIn = onSignIn)
    }
}

@Composable
private fun BrandPill(dark: Boolean, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clearAndSetSemantics {
                contentDescription = "Scorenge"
                heading()
            }
            .background(if (dark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.75f), CircleShape)
            .border(1.dp, Color(0xFFFDE68A).copy(alpha = if (dark) 0.25f else 0.6f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(BrandOrange, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "SCORENGE",
            color = textColor,
            fontFamily = Fredoka,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
private fun SignInPanel(state: SignInUiState, onSignIn: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                // Brand orange at the wave, deepening before the small text so white passes 4.5:1.
                Brush.verticalGradient(0f to BrandOrange, 0.35f to BrandOrangePanel, 1f to BrandOrangePanel),
                WaveTopShape(WaveHeight),
            )
            .padding(top = WaveHeight + 16.dp)
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, bottom = 24.dp),
    ) {
        Text(
            text = "Track every round,\nrivalry & win",
            color = Color.White,
            fontFamily = Fredoka,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 33.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Quick scores, live standings and match history for your game night crew.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onSignIn,
            enabled = !state.inProgress,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonCream,
                contentColor = BrandOrangeDeep,
                disabledContainerColor = ButtonCream.copy(alpha = 0.85f),
                disabledContentColor = BrandOrangeDeep,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            if (state.inProgress) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = BrandOrangeDeep,
                    modifier = Modifier.size(22.dp),
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text("Continue with Google", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        state.errorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = ErrorInk,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CreamPale, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Your scores stay on this phone and are backed up to your account.",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(heightDp = 844, widthDp = 412)
@Composable
private fun SignInScreenPreview() {
    ScorengeTheme(darkTheme = false) { SignInScreen(state = SignInUiState(), onSignIn = {}) }
}

@Preview(heightDp = 844, widthDp = 412)
@Composable
private fun SignInScreenDarkPreview() {
    ScorengeTheme(darkTheme = true) {
        SignInScreen(state = SignInUiState(errorMessage = "Sign-in failed. Please try again."), onSignIn = {})
    }
}
