package ir.hooshamooz.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PicoIntroductionDialog(onDismiss: () -> Unit) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .85f
    Dialog(onDismissRequest = onDismiss) {
        Surface(Modifier.widthIn(max = 480.dp).fillMaxWidth().heightIn(max = maxHeight),
            shape = RoundedCornerShape(28.dp), color = Panel,
            border = BorderStroke(1.dp, Violet.copy(alpha = .4f))) {
            Column(Modifier.background(Brush.linearGradient(listOf(Panel, Violet.copy(alpha = .12f)))).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("سلام، من پیکوام!", Modifier.weight(1f), color = Teal,
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.semantics { contentDescription = "بستن پیام معرفی پیکو" }) {
                        Canvas(Modifier.size(20.dp)) {
                            drawLine(Ink, Offset(3.dp.toPx(), 3.dp.toPx()), Offset(size.width - 3.dp.toPx(), size.height - 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                            drawLine(Ink, Offset(size.width - 3.dp.toPx(), 3.dp.toPx()), Offset(3.dp.toPx(), size.height - 3.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
                        }
                    }
                }
                Text("سلام، من پیکوام! یه بات با کلی ایده و اعتمادبه‌نفسی که بعضی وقت‌ها از اطلاعاتم جلو می‌زنه! اینجا قراره با هم بفهمیم هوش مصنوعی چه کارهایی بلده، چطور ازش کمک بگیریم و کِی بهتره جوابش رو بررسی کنیم. گاهی من کمکت می‌کنم، گاهی تو باید مچ اشتباه‌هام رو بگیری و خرابکاری‌هام رو درست کنی.",
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    color = Ink, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
