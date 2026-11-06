package ir.hooshamooz.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.os.SystemClock
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.*
import ir.hooshamooz.app.data.PicoGreetings
import ir.hooshamooz.engine.*
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(model: LearningViewModel, state: AppState) {
    val chapters = model.chapterProgress()
    val completed = chapters.count { it.status == ChapterStatus.COMPLETED }
    val fraction = chapters.sumOf { it.fraction.toDouble() }.toFloat() / chapters.size
    val resume = model.continueRun()
    val chapter = resume?.let { run -> model.content.chapters.firstOrNull { it.id == model.engine.current(run).chapterId } } ?: model.nextChapter()
    val today = model.today()
    val challenge = model.todayChallenge()
    val motion = LocalMotionEnabled.current
    val filledProgress = remember { Animatable(0f) }
    var loadNumber by remember { mutableIntStateOf(0) }
    var resumed by remember { mutableStateOf(false) }
    var lastInteraction by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var touching by remember { mutableStateOf(false) }
    var idle by remember { mutableStateOf(false) }
    var showPicoIntroduction by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        loadNumber++
        resumed = true
        idle = false
        lastInteraction = SystemClock.elapsedRealtime()
        onPauseOrDispose {
            resumed = false
            touching = false
            idle = false
        }
    }
    LaunchedEffect(lastInteraction, resumed, touching, showPicoIntroduction) {
        if (resumed && !touching && !showPicoIntroduction) {
            delay(10_000)
            idle = true
        }
    }
    LaunchedEffect(loadNumber, fraction, motion) {
        filledProgress.snapTo(0f)
        if (motion) filledProgress.animateTo(fraction, tween(950, easing = FastOutSlowInEasing))
        else filledProgress.snapTo(fraction)
    }
    Box(Modifier.fillMaxSize().pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                lastInteraction = SystemClock.elapsedRealtime()
                idle = false
                touching = event.changes.any { it.pressed }
            }
        }
    }.onPreviewKeyEvent {
        lastInteraction = SystemClock.elapsedRealtime()
        idle = false
        false
    }) {
    ScreenColumn {
        Box(Modifier.fillMaxWidth().heightIn(min = 238.dp)) {
            BreathingPlanet(Modifier.align(Alignment.TopEnd).size(170.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(PicoGreetings.titles[state.homeTitleIndex], style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    TypingPicoMessage(PicoGreetings.lines[state.greetingIndex], loadNumber,
                        Modifier.background(Panel.copy(alpha = .85f), RoundedCornerShape(18.dp)).padding(14.dp))
                }
                Box(Modifier.width(154.dp).height(224.dp).clickable(role = Role.Button,
                    onClickLabel = "آشنایی با پیکو", onClick = {
                        idle = false
                        showPicoIntroduction = true
                    })) {
                    HomePico(idle, state.homeMood, Modifier.fillMaxSize())
                }
            }
        }
        SpaceCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("پیشرفت کل", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${completed.fa()} فصل از ${chapters.size.fa()} فصل", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text("${(fraction * 100).roundToInt().fa()}٪", color = Teal, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { filledProgress.value }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Teal, trackColor = Violet.copy(alpha = .18f))
            Text("امتیاز یادگیری: ${model.totalPoints().fa()}", color = Muted)
        }
        SpaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (resume != null) "ادامهٔ یادگیری" else if (completed == chapters.size) "مأموریت کامل شد!" else "آغاز مأموریت", color = Teal, style = MaterialTheme.typography.labelLarge)
                    Text(chapter?.title ?: "همهٔ مرحله‌ها رو گذروندی!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(when {
                        resume?.mode == RunMode.EXAM -> "آزمون پایانی · سؤال ${(resume.index + 1).fa()} از ${resume.activityIds.size.fa()}"
                        resume != null -> "فعالیت ${(resume.index + 1).fa()} از ${resume.activityIds.size.fa()}"
                        chapter != null && chapter.lessonIds.all { it in state.data.completedLessons } -> "آمادهٔ آزمون پایانی هستی"
                        chapter != null -> chapter.subtitle
                        else -> "می‌تونی مرحله‌های قبلی رو مرور کنی."
                    }, color = Muted)
                }
                Planet(Modifier.size(80.dp))
            }
            if (chapter != null) PrimaryButton(if (resume != null) "ادامه بده ←" else "بزن بریم ←", model::continueLearning)
            else PrimaryButton("مرور مرحله‌ها", { model.navigate(Screen.PATH) })
        }
        SpaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("؟", Modifier.background(Orange, RoundedCornerShape(14.dp)).padding(horizontal = 16.dp, vertical = 6.dp), color = Space, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("چالش امروز", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Text(when { today == null && completed > 0 -> "فعلاً چالش تازه‌ای نداری"; today == null -> "پس از پایان اولین فصل باز می‌شود"; today.completed -> "خرابکاری امروز اصلاح شد!"; else -> challenge?.title ?: "اشتباه پیکو را پیدا کن" }, color = Muted)
                    if (challenge != null && today?.completed == false) {
                        val chapterTitle = model.content.chapters.first { it.id == challenge.chapterId }.title
                        Text("$chapterTitle · حدود ${challenge.estimatedMinutes.fa()} دقیقه · دو قدم", color = Teal, style = MaterialTheme.typography.bodySmall)
                    }
                    if (today == null && completed > 0) Text("مرور هر چالش، ۷ روز پس از تکمیلش آماده می‌شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(onClick = model::startDaily, enabled = today != null, modifier = Modifier.fillMaxWidth()) {
                Text(if (today?.completed == true) "دیدن نتیجهٔ چالش" else "شروع چالش")
            }
        }
        HomeLinks(onOpenPath = { model.navigate(Screen.PATH) })
    }
    }
    if (showPicoIntroduction) PicoIntroductionDialog(onDismiss = {
        showPicoIntroduction = false
        idle = false
        lastInteraction = SystemClock.elapsedRealtime()
    })
}
