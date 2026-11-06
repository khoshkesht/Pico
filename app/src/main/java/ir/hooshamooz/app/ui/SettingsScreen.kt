package ir.hooshamooz.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.*
import ir.hooshamooz.app.R
import android.widget.Toast

@Composable
fun SettingsScreen(model: LearningViewModel, state: AppState) {
    var confirming by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    ScreenColumn("تنظیمات") {
        SpaceCard {
            Text("اندازهٔ متن", fontWeight = FontWeight.Bold)
            Text("متن‌ها را راحت‌تر بخوان؛ اندازهٔ متن سیستم هم رعایت می‌شود.", color = Muted)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1f to "معمولی", 1.15f to "بزرگ", 1.3f to "بزرگ‌تر").forEach { (scale, title) ->
                    FilterChip(selected = state.data.textScale == scale, onClick = { model.setTextScale(scale) }, label = { Text(title) })
                }
            }
        }
        SpaceCard {
            // The destructive action still has its confirmation; the card has no extra copy.
            OutlinedButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) { Text("بازنشانی پیشرفت") }
        }
        SpaceCard {
            Text("دربارهٔ این اپ", fontWeight = FontWeight.Bold)
            Text("پیکو با کمک هوش مصنوعی و با هدف آموزش مفاهیم اولیهٔ هوش مصنوعی (AI) به نوجوانان ساخته شده و استفاده از آن رایگان است.", color = Muted)
        }
        SpaceCard(Modifier.clickable(onClickLabel = "وب‌سایت قلق استدیو") {
            runCatching { uriHandler.openUri("http://gheleghstudio.ir/") }.onFailure {
                Toast.makeText(context, "مرورگری برای بازکردن لینک پیدا نشد.", Toast.LENGTH_SHORT).show()
            }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Image(painterResource(R.drawable.studio_logo), "لوگوی قلق استدیو", Modifier.size(72.dp), contentScale = ContentScale.Fit)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("دربارهٔ پیکو", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text("قلق استدیو", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text("gheleghstudio.ir ↗", color = Violet, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
    if (confirming) AlertDialog(onDismissRequest = { confirming = false }, title = { Text("پیشرفت پاک شود؟") },
        text = { Text("پیشرفت فصل‌ها، نتیجهٔ آزمون‌ها و سابقهٔ چالش‌ها از این گوشی پاک می‌شوند.") },
        confirmButton = { TextButton(onClick = { confirming = false; model.reset() }) { Text("پاک کردن پیشرفت") } },
        dismissButton = { TextButton(onClick = { confirming = false }) { Text("انصراف") } })
}
