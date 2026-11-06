package ir.hooshamooz.app.ui

import android.icu.text.BreakIterator
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.R
import ir.hooshamooz.engine.PicoMood
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos

@Composable
fun TypingPicoMessage(message: String, visit: Int, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    var visibleEnd by remember(message, visit) { mutableIntStateOf(0) }
    LaunchedEffect(message, visit, motion) {
        visibleEnd = if (motion) 0 else message.length
        if (motion) {
            // Reveal complete characters, keeping Persian combining marks together.
            val boundaries = BreakIterator.getCharacterInstance(Locale.forLanguageTag("fa"))
            boundaries.setText(message)
            var end = boundaries.next()
            while (end != BreakIterator.DONE) {
                delay(32)
                visibleEnd = end
                end = boundaries.next()
            }
        }
    }
    Box(modifier.clearAndSetSemantics { text = AnnotatedString(message) }) {
        // Reserve the final text's space so typing never moves the cards below it.
        Text(message, Modifier.alpha(0f), color = Ink)
        Text(if (motion) message.take(visibleEnd) else message, color = Ink)
    }
}

@Composable
fun HomePico(idle: Boolean, mood: PicoMood, modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    Crossfade(idle, modifier, tween(if (motion) 220 else 0), label = "home-pico-idle") { loading ->
        if (loading) {
            Column(Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = "پیکو در حال فکرکردن" },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Image(painterResource(R.drawable.pico_loading_head), contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(154.dp), contentScale = ContentScale.Fit)
                LoadingDots(Modifier.width(64.dp).height(26.dp))
            }
        } else PicoCharacter(mood, Modifier.fillMaxSize())
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val motion = LocalMotionEnabled.current
    val phase = if (motion) {
        rememberInfiniteTransition(label = "pico-loading").animateFloat(0f, 1f,
            infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "loading-dots")
    } else remember { mutableFloatStateOf(0f) }
    Canvas(modifier) {
        repeat(3) { index ->
            val wave = if (motion) ((1f - cos((phase.value - index / 3f) * 2f * PI).toFloat()) / 2f) else .6f
            drawCircle(Teal.copy(alpha = .25f + .75f * wave), 3.5.dp.toPx(),
                Offset(size.width * (index + 1) / 4f, size.height * .6f - 5.dp.toPx() * wave))
        }
    }
}
