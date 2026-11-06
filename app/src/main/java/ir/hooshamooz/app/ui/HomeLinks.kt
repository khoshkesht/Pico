package ir.hooshamooz.app.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.R

private data class PromotedGame(val title: String, val description: String, val image: Int, val url: String)

private val promotedGames = listOf(
    PromotedGame("ردینو", "در هر پرونده، محیط را جست‌وجو کن، سرنخ پیدا کن",
        R.drawable.promo_radino, "https://cafebazaar.ir/app/radino.mokho.com"),
    PromotedGame("رمز و راز", "۱۰۱ مرحله چیستان، معما و تست هوش تعاملی منتظر توس",
        R.drawable.promo_ramzoraz, "https://cafebazaar.ir/app/ramzoraz.mokho.com"),
)

@Composable
fun HomeLinks(onOpenPath: () -> Unit) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    val descriptionStyle = MaterialTheme.typography.bodySmall
    val adStyle = MaterialTheme.typography.labelSmall
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val imageWidth = (maxWidth * .28f).coerceIn(80.dp, 120.dp)
        val textWidth = with(density) { (maxWidth - imageWidth - 46.dp).roundToPx().coerceAtLeast(1) }
        val adWidth = measurer.measure("ads", adStyle).size.width + with(density) { 14.dp.roundToPx() }
        // Keep the original path card's 96dp baseline. Expand all three together when
        // narrow windows or enlarged text need more space; never truncate the copy.
        val textHeight = promotedGames.maxOf { game ->
            val constraints = Constraints(maxWidth = textWidth)
            measurer.measure(game.title, titleStyle, constraints = Constraints(maxWidth = (textWidth - adWidth).coerceAtLeast(1))).size.height +
                measurer.measure(game.description, descriptionStyle, constraints = constraints).size.height
        }
        val cardHeight = maxOf(96.dp, with(density) { textHeight.toDp() } + 28.dp)
        val shape = RoundedCornerShape(24.dp)
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            SpaceCard(Modifier.height(cardHeight).clip(shape).clickable(role = Role.Button,
                onClickLabel = "بازکردن مسیر یادگیری", onClick = onOpenPath)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Text("مسیر یادگیری", Modifier.weight(1f), color = Ink, style = MaterialTheme.typography.titleMedium)
                    MiniOrbit()
                    Text(" ←", color = Violet)
                }
            }
            promotedGames.forEach { game ->
                SpaceCard(Modifier.height(cardHeight).clip(shape).clickable(role = Role.Button,
                    onClickLabel = "دانلود بازی ${game.title}", onClick = {
                        runCatching { uriHandler.openUri(game.url) }.onFailure {
                            Toast.makeText(context, "برنامه‌ای برای بازکردن لینک پیدا نشد.", Toast.LENGTH_SHORT).show()
                        }
                    }), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                    // The app's RTL layout puts the text on the right and artwork on the left.
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(game.title, Modifier.weight(1f), color = Ink, style = titleStyle)
                                Text("ads", color = Muted.copy(alpha = .65f), style = adStyle,
                                    modifier = Modifier.background(Muted.copy(alpha = .07f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                        .semantics { contentDescription = "تبلیغ" })
                            }
                            Text(game.description, color = Muted, style = descriptionStyle)
                        }
                        val artwork = painterResource(game.image)
                        val aspect = artwork.intrinsicSize.width / artwork.intrinsicSize.height
                        val imageHeight = minOf(cardHeight - 24.dp, imageWidth / aspect)
                        Box(Modifier.width(imageWidth).fillMaxHeight(), contentAlignment = Alignment.Center) {
                            Image(artwork, contentDescription = null,
                                modifier = Modifier.size(imageHeight * aspect, imageHeight).clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Fit)
                        }
                    }
                }
            }
        }
    }
}
