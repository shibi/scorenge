package com.bzcode.scorenge.ui.auth

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bzcode.scorenge.R
import com.bzcode.scorenge.ui.theme.AmberCitrus
import com.bzcode.scorenge.ui.theme.BrandOrange
import com.bzcode.scorenge.ui.theme.CreamPale

private val AmberInk = Color(0xFF451A03)
private val AmberChip = Color(0xFFFBBF24)
private val AmberLight = Color(0xFFFDE68A)
private val CoralOrange = Color(0xFFFF5E36)

/**
 * Decorative board-game hero for the sign-in screen: a big orange die with a crown pip, a small
 * ivory die, a "+15" score chip, and a meeple and playing card that bob gently. Drawn in Compose
 * from the Stitch "Alternate Login (Reference Style)" design (256 x 256 units), scaled to fit.
 * Hidden from accessibility services, and hidden entirely when there is too little room.
 */
@Composable
fun SignInArtwork(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth * 0.78f, maxHeight, 288.dp)
        if (side < 96.dp) return@BoxWithConstraints
        val u = side / 256

        val (bob, bobReverse) = floatPhases()

        Box(
            Modifier
                .size(side)
                .clearAndSetSemantics {},
        ) {
            // Peach backdrop circle with a dashed and a thin inner ring.
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFFFE7C8), Color(0xFFFFD8AC), Color(0xFFFFF1DE)),
                            start = Offset(0f, Float.POSITIVE_INFINITY),
                            end = Offset(Float.POSITIVE_INFINITY, 0f),
                        ),
                        CircleShape,
                    ),
            )
            Box(
                Modifier
                    .matchParentSize()
                    .padding(16 * u)
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawCircle(
                            color = CoralOrange.copy(alpha = 0.25f),
                            radius = size.minDimension / 2 - stroke / 2,
                            style = Stroke(
                                width = stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())),
                            ),
                        )
                    },
            )
            Box(
                Modifier
                    .matchParentSize()
                    .padding(48 * u)
                    .border(1.dp, AmberChip.copy(alpha = 0.3f), CircleShape),
            )

            // Dice stack with the score chip.
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(144 * u),
            ) {
                BigDie(
                    u,
                    Modifier
                        .align(Alignment.TopStart)
                        .rotate(-6f)
                        .size(128 * u),
                )
                SmallDie(
                    u,
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(12 * u, 12 * u)
                        .rotate(12f)
                        .size(64 * u),
                )
                ScoreChip(
                    u,
                    Modifier
                        .align(Alignment.BottomStart)
                        .offset(-16 * u, 8 * u),
                )
            }

            // Floating meeple (top left) and playing card (top right).
            Image(
                painter = painterResource(R.drawable.ic_meeple),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(24 * u, -4 * u)
                    .graphicsLayer {
                        translationY = -(7 * u).toPx() * bob
                        rotationZ = -12f + 2f * bob
                    }
                    .size(56 * u, 64 * u),
            )
            PlayingCard(
                u,
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(-20 * u, -8 * u)
                    .graphicsLayer {
                        translationY = -(7 * u).toPx() * bobReverse
                        rotationZ = 12f + 2f * bobReverse
                    }
                    .size(48 * u, 64 * u),
            )
        }
    }
}

/** Two 0..1 phases for the bobbing pieces, or zeros when the system has animations turned off. */
@Composable
private fun floatPhases(): Pair<Float, Float> {
    val context = LocalContext.current
    val animate = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    if (!animate) return 0f to 0f
    val transition = rememberInfiniteTransition(label = "float")
    val bob = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "meeple",
    )
    val bobReverse = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "card",
    )
    return bob.value to bobReverse.value
}

@Composable
private fun BigDie(u: Dp, modifier: Modifier) {
    val shape = RoundedCornerShape(24 * u)
    Box(
        modifier
            .shadow(16 * u, shape, ambientColor = Color(0xFFDC3B19), spotColor = Color(0xFFDC3B19))
            .background(
                Brush.linearGradient(listOf(Color(0xFFFF6E47), BrandOrange, Color(0xFFCF3515))),
                shape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.3f), shape)
            .padding(14 * u),
    ) {
        val pip = Modifier
            .size(20 * u)
            .background(CreamPale, CircleShape)
        Box(pip.align(Alignment.TopStart))
        Box(pip.align(Alignment.TopEnd))
        Box(pip.align(Alignment.BottomStart))
        Box(pip.align(Alignment.BottomEnd))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .size(24 * u)
                .background(AmberCitrus, CircleShape),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_crown),
                contentDescription = null,
                tint = AmberInk,
                modifier = Modifier.size(14 * u),
            )
        }
    }
}

@Composable
private fun SmallDie(u: Dp, modifier: Modifier) {
    val shape = RoundedCornerShape(16 * u)
    Box(
        modifier
            .shadow(12 * u, shape, ambientColor = Color(0xFFD84B29), spotColor = Color(0xFFD84B29))
            .background(Brush.linearGradient(listOf(Color.White, Color(0xFFFEEED1))), shape)
            .border(1.dp, Color.White.copy(alpha = 0.8f), shape)
            .padding(12 * u),
    ) {
        val pip = Modifier
            .size(10 * u)
            .background(BrandOrange, CircleShape)
        Box(pip.align(Alignment.TopStart))
        Box(pip.align(Alignment.TopEnd))
        Box(pip.align(Alignment.BottomStart))
        Box(pip.align(Alignment.BottomEnd))
    }
}

@Composable
private fun ScoreChip(u: Dp, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4 * u),
        modifier = modifier
            .shadow(4 * u, CircleShape)
            .background(AmberChip, CircleShape)
            .border(1.dp, AmberLight, CircleShape)
            .padding(horizontal = 10 * u, vertical = 4 * u),
    ) {
        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = AmberInk, modifier = Modifier.size(12 * u))
        Text("+15", color = AmberInk, fontWeight = FontWeight.ExtraBold, fontSize = (11 * u.value).sp)
    }
}

@Composable
private fun PlayingCard(u: Dp, modifier: Modifier) {
    val shape = RoundedCornerShape(12 * u)
    val cornerText = (12 * u.value).sp
    Box(
        modifier
            .shadow(10 * u, shape, ambientColor = Color(0xFFD84B29), spotColor = Color(0xFFD84B29))
            .background(Color.White, shape)
            .border(2.dp, Color(0xFFFCD34D).copy(alpha = 0.8f), shape)
            .padding(horizontal = 6 * u, vertical = 4 * u),
    ) {
        Text(
            "A",
            color = BrandOrange,
            fontWeight = FontWeight.Black,
            fontSize = cornerText,
            lineHeight = cornerText,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .rotate(45f)
                .size(16 * u)
                .background(CoralOrange, RoundedCornerShape(3 * u)),
        ) {
            Text(
                "★",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (8 * u.value).sp,
                lineHeight = (8 * u.value).sp,
                modifier = Modifier.rotate(-45f),
            )
        }
        Text(
            "A",
            color = BrandOrange,
            fontWeight = FontWeight.Black,
            fontSize = cornerText,
            lineHeight = cornerText,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/** Shape whose top edge is the S-curve wave from the design; the rest is a plain rectangle. */
class WaveTopShape(private val waveHeight: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val sx = size.width / 375f
        val sy = with(density) { waveHeight.toPx() } / 40f
        val path = Path().apply {
            moveTo(0f, 28 * sy)
            cubicTo(60 * sx, 40 * sy, 110 * sx, 6 * sy, 185 * sx, 15 * sy)
            cubicTo(260 * sx, 24 * sy, 315 * sx, 38 * sy, size.width, 18 * sy)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
