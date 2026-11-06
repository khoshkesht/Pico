package ir.hooshamooz.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.LearningViewModel
import ir.hooshamooz.app.AppState
import ir.hooshamooz.engine.ChapterStatus
import ir.hooshamooz.engine.LearningProgress

@Composable
fun PathScreen(model: LearningViewModel, state: AppState) {
    ScreenColumn("مسیر یادگیری") {
        Text("هشت ایستگاه، برای شناخت بهتر هوش مصنوعی", color = Muted)
        LearningProgress.chapters(model.content.chapters, state.data.completedLessons, state.data.completedExams).forEachIndexed { index, progress ->
            if (index > 0) Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                val path = Path().apply { moveTo(size.width * .5f, 0f); cubicTo(size.width * .65f, size.height * .3f, size.width * .35f, size.height * .7f, size.width * .5f, size.height) }
                drawPath(path, Violet.copy(alpha = .35f), style = Stroke(2.dp.toPx()))
            }
            SpaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Planet(Modifier.size(66.dp), if (progress.status == ChapterStatus.COMPLETED) Teal else Violet, index % 2 == 0)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("فصل ${(index + 1).fa()} · ${progress.chapter.title}", fontWeight = FontWeight.Bold)
                        Text(when (progress.status) {
                            ChapterStatus.LOCKED -> if (progress.total == 0) "قفل · محتوا در حال آماده‌سازی" else "قفل · ابتدا فصل قبل را تمام کن"
                            ChapterStatus.READY -> "آمادهٔ شروع"
                            ChapterStatus.IN_PROGRESS -> "در حال انجام · ${((progress.fraction * 100).toInt()).fa()}٪"
                            ChapterStatus.COMPLETED -> "تکمیل‌شده ✓"
                        }, color = if (progress.status == ChapterStatus.COMPLETED) Teal else Muted)
                    }
                }
                if (progress.status != ChapterStatus.LOCKED) OutlinedButton(onClick = { model.startChapter(progress.chapter, replay = progress.status == ChapterStatus.COMPLETED) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (progress.status == ChapterStatus.COMPLETED) "مرور فصل" else "ورود به فصل")
                }
            }
        }
    }
}
