package ir.hooshamooz.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.graphics.BitmapFactory
import ir.hooshamooz.app.data.ActivityIllustration
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.engine.ContentBlock

@Composable
fun LocalIllustration(illustration: ActivityIllustration) {
    val context = LocalContext.current
    val image = remember(illustration.path) {
        context.assets.open(illustration.path).use { stream ->
            requireNotNull(BitmapFactory.decodeStream(stream)).asImageBitmap()
        }
    }
    SpaceCard {
        Text(illustration.title, color = Violet, style = MaterialTheme.typography.labelLarge)
        Image(image, contentDescription = illustration.description,
            modifier = Modifier.fillMaxWidth().aspectRatio(image.width.toFloat() / image.height), contentScale = ContentScale.Fit)
    }
}

/** Static offline illustrations: no image-generation cost or additional downloads. */
@Composable
fun ActivityMedia(block: ContentBlock) {
    when (block) {
        is ContentBlock.Text -> SpaceCard {
            Text(block.title, color = Violet, style = MaterialTheme.typography.labelLarge)
            Text(block.text, style = MaterialTheme.typography.bodyLarge)
        }
        is ContentBlock.Cups -> SpaceCard {
            Text("نوشیدنی خنک", Modifier.align(Alignment.CenterHorizontally), fontWeight = FontWeight.Bold)
            Canvas(Modifier.fillMaxWidth().height(184.dp).semantics { contentDescription = block.description }) {
                val w = size.width; val h = size.height
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF40346D), Panel)), Offset(0f, h * .12f), Size(w, h * .88f), androidx.compose.ui.geometry.CornerRadius(22f))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF76609B), Color(0xFF393253))), Offset(w * .02f, h * .65f), Size(w * .96f, h * .13f))
                drawRect(Color(0xFF45365F), Offset(w * .08f, h * .78f), Size(w * .06f, h * .22f))
                drawRect(Color(0xFF45365F), Offset(w * .86f, h * .78f), Size(w * .06f, h * .22f))
                // Exactly four opaque paper cups, then two transparent cups; contents unspecified.
                repeat(6) { i ->
                    val cx = w * (.10f + i * .16f); val top = h * .28f
                    val rw = w * .057f; val cupH = h * .37f
                    val path = Path().apply { moveTo(cx - rw, top); lineTo(cx + rw, top); lineTo(cx + rw * .72f, top + cupH); lineTo(cx - rw * .72f, top + cupH); close() }
                    if (i < 4) {
                        drawPath(path, Brush.horizontalGradient(listOf(Color(0xFFE8DFF8), Color(0xFFB7A6D5))))
                        drawLine(Violet.copy(alpha = .45f), Offset(cx - rw * .87f, top + cupH * .5f), Offset(cx + rw * .87f, top + cupH * .5f), cupH * .06f)
                    } else {
                        drawPath(path, Teal.copy(alpha = .07f))
                        drawPath(path, Color(0xFFD9F4F4).copy(alpha = .85f), style = Stroke(1.5.dp.toPx()))
                        drawLine(Color.White.copy(alpha = .55f), Offset(cx - rw * .58f, top + cupH * .18f), Offset(cx - rw * .40f, top + cupH * .78f), 1.5.dp.toPx())
                    }
                    drawOval(if (i < 4) Color(0xFF8A7AAB) else Color(0xFFB7DCDF).copy(alpha = .35f), Offset(cx - rw, top - h * .025f), Size(rw * 2, h * .05f))
                }
            }
            Text("فقط اطلاعات قابل‌مشاهدهٔ تصویر را در نظر بگیر.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        is ContentBlock.VoiceMessage -> SpaceCard {
            Text("نمونهٔ پیام صوتی · متن همراه", color = Violet, style = MaterialTheme.typography.labelLarge)
            Canvas(Modifier.fillMaxWidth().height(34.dp).semantics { contentDescription = block.description }) {
                repeat(35) { i ->
                    val height = size.height * (.2f + ((i * 7) % 11) / 14f)
                    val x = size.width * (i + .5f) / 35
                    drawLine(Violet.copy(alpha = .6f), Offset(x, (size.height - height) / 2), Offset(x, (size.height + height) / 2), 3.dp.toPx())
                }
            }
            Text(block.transcript, style = MaterialTheme.typography.bodyLarge)
            Text("این یک نمونهٔ ثابت است و پخش صدا ندارد.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
