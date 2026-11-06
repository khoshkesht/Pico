package ir.hooshamooz.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.Screen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExitIcon() {
    Canvas(Modifier.size(30.dp)) {
        scale(size.width / 32f, size.height / 32f, Offset.Zero) {
            val door = Path().apply {
                moveTo(20f, 6f); lineTo(9f, 6f); quadraticTo(6f, 6f, 6f, 9f)
                lineTo(6f, 23f); quadraticTo(6f, 26f, 9f, 26f); lineTo(20f, 26f)
            }
            drawPath(door, Muted, style = Stroke(2f, cap = StrokeCap.Round))
            drawLine(Orange, Offset(15f, 16f), Offset(28f, 16f), 2.2f, StrokeCap.Round)
            val arrow = Path().apply { moveTo(23f, 11f); lineTo(28f, 16f); lineTo(23f, 21f) }
            drawPath(arrow, Orange, style = Stroke(2.2f, cap = StrokeCap.Round))
        }
    }
}

/** Local vector geometry: crisp at every density, with no Unicode glyph dependency. */
@Composable
fun NavigationIcon(screen: Screen, selected: Boolean = false, modifier: Modifier = Modifier.size(30.dp)) {
    val line = if (selected) Violet else Muted
    val accent = if (selected) Teal else Muted.copy(alpha = .8f)
    Canvas(modifier) {
        scale(size.width / 32f, size.height / 32f, Offset.Zero) {
            val stroke = Stroke(width = 2f, cap = StrokeCap.Round)
            when (screen) {
                Screen.HOME -> {
                    val roof = Path().apply { moveTo(3f, 14f); lineTo(16f, 3f); lineTo(29f, 14f) }
                    drawPath(roof, line, style = stroke)
                    drawRoundRect(line.copy(alpha = .16f), Offset(7f, 13f), Size(18f, 15f), CornerRadius(3f))
                    val house = Path().apply { moveTo(6f, 14f); lineTo(6f, 27f); quadraticTo(6f, 29f, 8f, 29f); lineTo(24f, 29f); quadraticTo(26f, 29f, 26f, 27f); lineTo(26f, 14f) }
                    drawPath(house, line, style = stroke)
                    drawRoundRect(accent, Offset(13f, 20f), Size(6f, 9f), CornerRadius(1.5f))
                    drawCircle(if (selected) Orange else line, 2f, Offset(16f, 14f))
                    drawLine(line, Offset(23f, 7f), Offset(23f, 4f), 2f, StrokeCap.Round)
                }
                Screen.PATH -> {
                    val route = Path().apply { moveTo(7f, 26f); cubicTo(27f, 27f, 6f, 7f, 25f, 6f) }
                    drawPath(route, line, style = stroke)
                    listOf(Offset(7f, 26f), Offset(16f, 16f), Offset(25f, 6f)).forEachIndexed { index, point ->
                        drawCircle(Panel, 4.2f, point)
                        drawCircle(if (index == 1) accent else line, 3.8f, point, style = Stroke(2f))
                        drawCircle(if (index == 1) accent else line.copy(alpha = .5f), 1.4f, point)
                    }
                    drawCircle(if (selected) Orange else line, 1.1f, Offset(6f, 8f))
                    drawCircle(line.copy(alpha = .5f), .8f, Offset(27f, 25f))
                }
                else -> {
                    val gear = Path()
                    repeat(32) { index ->
                        val radius = if (index % 4 == 0 || index % 4 == 3) 12.5f else 9.7f
                        val angle = index * PI / 16 - PI / 2
                        val x = 16 + radius * cos(angle).toFloat()
                        val y = 16 + radius * sin(angle).toFloat()
                        if (index == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
                    }
                    gear.close()
                    drawPath(gear, line.copy(alpha = .16f))
                    drawPath(gear, line, style = Stroke(1.8f))
                    drawCircle(accent, 4.5f, Offset(16f, 16f), style = stroke)
                    drawCircle(accent.copy(alpha = .3f), 2f, Offset(16f, 16f))
                }
            }
        }
    }
}
