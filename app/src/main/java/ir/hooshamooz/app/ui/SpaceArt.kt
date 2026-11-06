package ir.hooshamooz.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StarField(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Space, Color(0xFF191735), Space)))
        repeat(46) { i ->
            val x = ((i * 137 + 23) % 997) / 997f * size.width
            val y = ((i * 223 + 73) % 991) / 991f * size.height
            drawCircle(Ink.copy(alpha = if (i % 5 == 0) .55f else .16f), if (i % 5 == 0) 2.2f else 1.2f, Offset(x, y))
        }
    }
}

@Composable
fun MeteorField(modifier: Modifier = Modifier) {
    if (!LocalMotionEnabled.current) return
    val transition = rememberInfiniteTransition(label = "meteor-shower")
    val phase = transition.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "meteor-phase")
    Canvas(modifier) {
        // Staggered lifetimes keep 2–3 meteors moving; no long static intervals.
        // Animation state is read only in the draw phase, not in layout.
        repeat(5) { index ->
            val local = (phase.value + index / 5f) % 1f
            if (local < .55f) {
                val t = local / .55f
                val head = Offset(size.width * (1.15f - t * 1.3f), size.height * (.04f + index * .18f) + t * 100.dp.toPx())
                val tail = head + Offset(76.dp.toPx(), -32.dp.toPx())
                val alpha = (kotlin.math.sin(t * kotlin.math.PI).toFloat() * .60f).coerceIn(0f, .60f)
                drawLine(Brush.linearGradient(listOf(Teal.copy(alpha = alpha * .2f), Color.Transparent), head, tail), head, tail, 5.dp.toPx())
                drawLine(Brush.linearGradient(listOf(Ink.copy(alpha = alpha), Violet.copy(alpha = alpha * .4f), Color.Transparent), head, tail), head, tail, 1.6.dp.toPx())
                drawCircle(Ink.copy(alpha = alpha), 1.8.dp.toPx(), head)
            }
        }
        repeat(14) { index ->
            val x = (((index * 71 % 101) / 101f - phase.value * .12f + 1f) % 1f) * size.width
            val y = (((index * 43 % 97) / 97f + phase.value * .06f) % 1f) * size.height
            drawCircle(Violet.copy(alpha = .3f), .8.dp.toPx(), Offset(x, y))
        }
    }
}

@Composable
fun BreathingPlanet(modifier: Modifier = Modifier) {
    if (!LocalMotionEnabled.current) { Planet(modifier); return }
    val transition = rememberInfiniteTransition(label = "home-planet")
    val phase = transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "planet-breath")
    val distance = with(LocalDensity.current) { 4.dp.toPx() }
    Planet(modifier.graphicsLayer {
        val wave = sin(phase.value)
        translationY = wave * distance
        rotationZ = wave * 5f
        scaleX = 1f + wave * .035f; scaleY = scaleX
        alpha = .92f + wave * .08f
    })
}

@Composable
fun OrbitalLinks(modifier: Modifier = Modifier) {
    if (!LocalMotionEnabled.current) return
    val transition = rememberInfiniteTransition(label = "context-orbits")
    val phase = transition.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "orbit-phase")
    Canvas(modifier) {
        repeat(3) { index ->
            val center = Offset(size.width * if (index % 2 == 0) .25f else .77f, size.height * (.15f + index * .34f))
            val rx = size.width * .46f; val ry = size.height * .10f
            rotate(18f - index * 14f, center) {
                drawOval(Violet.copy(alpha = .18f), Offset(center.x - rx, center.y - ry), androidx.compose.ui.geometry.Size(rx * 2, ry * 2), style = Stroke(1.dp.toPx()))
                val points = (0..3).map { node ->
                    val angle = phase.value * if (index % 2 == 0) 1f else -1f
                    val a = angle + node * (PI / 2).toFloat()
                    Offset(center.x + cos(a) * rx, center.y + sin(a) * ry)
                }
                points.forEachIndexed { node, point ->
                    drawLine(Teal.copy(alpha = .10f), point, points[(node + 1) % points.size], 1.dp.toPx())
                    val radius = 10.dp.toPx()
                    drawCircle(Brush.radialGradient(listOf(Teal.copy(alpha = .25f), Color.Transparent), point, radius), radius, point)
                    drawCircle(if (node % 2 == 0) Teal.copy(alpha = .6f) else Violet.copy(alpha = .7f), 2.5.dp.toPx(), point)
                }
            }
        }
    }
}

@Composable
fun SignalWaves(modifier: Modifier = Modifier) {
    if (!LocalMotionEnabled.current) return
    val transition = rememberInfiniteTransition(label = "signal-waves")
    val phase = transition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "signal-phase")
    Canvas(modifier) {
        repeat(3) { index ->
            val color = if (index % 2 == 0) Teal else Violet
            val baseline = size.height * (.18f + index * .31f)
            val amplitude = 24.dp.toPx()
            fun point(t: Float) = Offset(t * size.width,
                baseline + sin(t * (2 * PI).toFloat() + phase.value + index) * amplitude)
            val path = Path().apply {
                val start = point(0f); moveTo(start.x, start.y)
                for (step in 1..60) { val p = point(step / 60f); lineTo(p.x, p.y) }
            }
            drawPath(path, color.copy(alpha = .045f), style = Stroke(9.dp.toPx()))
            drawPath(path, color.copy(alpha = .19f), style = Stroke(1.dp.toPx()))
            repeat(2) { pulse ->
                val t = (phase.value / (2 * PI).toFloat() + pulse * .5f + index * .23f) % 1f
                val center = point(t)
                val fade = sin(t * PI).toFloat().coerceIn(0f, 1f)
                val radius = 15.dp.toPx()
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .28f * fade), Color.Transparent), center, radius), radius, center)
                drawCircle(color.copy(alpha = .65f * fade), 2.dp.toPx(), center)
            }
        }
    }
}

@Composable
fun Planet(modifier: Modifier = Modifier, color: Color = Violet, ring: Boolean = true) {
    Canvas(modifier) {
        val radius = size.minDimension * .30f
        val center = center
        drawCircle(Brush.radialGradient(listOf(color.copy(alpha = .20f), Color.Transparent), center, radius * 1.6f), radius * 1.6f, center)
        if (ring) drawOval(Orange.copy(alpha = .65f), Offset(center.x - radius * 1.6f, center.y - radius * .38f),
            androidx.compose.ui.geometry.Size(radius * 3.2f, radius * .76f), style = Stroke(radius * .12f))
        drawCircle(Brush.radialGradient(listOf(color, Color(0xFF433477)), Offset(center.x - radius * .4f, center.y - radius * .5f), radius * 1.8f), radius, center)
        repeat(5) { i ->
            val angle = i * 2 * PI / 5
            drawCircle(Space.copy(alpha = .16f), radius * (.12f + i * .013f),
                Offset(center.x + cos(angle).toFloat() * radius * .55f, center.y + sin(angle).toFloat() * radius * .5f))
        }
    }
}

@Composable
fun MiniOrbit(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Planet(Modifier.size(32.dp), Orange, false)
        Planet(Modifier.size(40.dp), Violet)
        Planet(Modifier.size(30.dp), Teal, false)
    }
}
