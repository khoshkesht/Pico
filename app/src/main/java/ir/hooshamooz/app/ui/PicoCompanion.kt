package ir.hooshamooz.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.R
import ir.hooshamooz.engine.PicoMood

/** Three transparent full-body assets, with matching expression and hand gesture. */
@Composable
fun PicoCharacter(mood: PicoMood, modifier: Modifier = Modifier) {
    val (asset, description) = when (mood) {
        PicoMood.CONFIDENT, PicoMood.HAPPY -> R.drawable.pico_confident to "پیکو، زیادی مطمئن؛ یک دست به کمر و یک انگشت بالا"
        PicoMood.EMBARRASSED, PicoMood.SURPRISED -> R.drawable.pico_embarrassed to "پیکو، سوتی‌داده؛ یک دست پشت سر و یک کف دست رو به بالا"
        else -> R.drawable.pico_hero to "پیکو، گیج؛ شانه‌ها بالا و دست‌ها باز"
    }
    Image(painterResource(asset), contentDescription = description, modifier = modifier, contentScale = ContentScale.Fit)
}

@Composable
fun PicoCompanion(dialogue: String, mood: PicoMood) {
    val motion = LocalMotionEnabled.current
    Box(Modifier.fillMaxWidth().padding(top = 42.dp)) {
        Row(Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Panel, Violet.copy(alpha = .16f))), RoundedCornerShape(28.dp))
            .border(1.dp, Violet.copy(alpha = .35f), RoundedCornerShape(28.dp)).padding(22.dp),
            verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).heightIn(min = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("پیکو", color = Violet, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(dialogue, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(152.dp))
        }
        Crossfade(targetState = mood, animationSpec = tween(if (motion) 180 else 0), label = "pico-expression",
            modifier = Modifier.align(Alignment.TopEnd).offset(y = (-42).dp).width(174.dp).height(248.dp)) {
            PicoCharacter(it, Modifier.fillMaxSize())
        }
    }
}
