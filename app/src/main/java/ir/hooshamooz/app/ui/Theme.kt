package ir.hooshamooz.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.*
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val Space = Color(0xFF12142D)
val Panel = Color(0xFF232047)
val Violet = Color(0xFFA78BFA)
val Teal = Color(0xFF4DD9D0)
val Orange = Color(0xFFFFA448)
val Muted = Color(0xFFBEB7DE)
val Ink = Color(0xFFF5F3FF)
val LocalMotionEnabled = staticCompositionLocalOf { true }

@Composable
fun SpaceTheme(textScale: Float = 1f, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val context = LocalContext.current
    fun motionAllowed() = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    var motionEnabled by remember { mutableStateOf(motionAllowed()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { motionEnabled = motionAllowed() }
    CompositionLocalProvider(
        LocalMotionEnabled provides motionEnabled,
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDensity provides Density(density.density, density.fontScale * textScale),
    ) {
        MaterialTheme(colorScheme = darkColorScheme(
            primary = Violet, onPrimary = Space, secondary = Teal, onSecondary = Space,
            background = Space, onBackground = Ink, surface = Panel, onSurface = Ink,
            surfaceVariant = Color(0xFF302B58), onSurfaceVariant = Muted,
            outline = Color(0xFF625988), error = Color(0xFFFFB4AB),
        ), content = content)
    }
}

fun Int.fa(): String = toString().map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")
