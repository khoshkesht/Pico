package ir.hooshamooz.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.hooshamooz.app.*
import ir.hooshamooz.engine.*

@Composable
fun ActivityScreen(model: LearningViewModel, state: AppState) {
    val run = state.data.runs[state.activeRunId] ?: return
    val activity = model.engine.current(run)
    val chapterTitle = model.content.chapters.first { it.id == activity.chapterId }.title
    val challenge = model.dailyChallenge(run)
    var showHint by remember(activity.id) { mutableStateOf(false) }
    val feedback = model.engine.feedback(run)
    key(run.runId, run.index) {
    ScreenColumn(chapterTitle, { model.navigate(Screen.HOME) }) {
        if (challenge != null) {
            Text(challenge.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("چالش روزانه · حدود ${challenge.estimatedMinutes.fa()} دقیقه", color = Teal)
        }
        Text("${if (run.mode == RunMode.DAILY) "گام" else "فعالیت"} ${(run.index + 1).fa()} از ${run.activityIds.size.fa()}", color = Muted)
        LinearProgressIndicator(progress = { (run.index + 1f) / run.activityIds.size }, modifier = Modifier.fillMaxWidth(), color = Teal)
        Text(activity.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        val mood = when (feedback?.correct) { true -> PicoMood.CONFIDENT; false -> PicoMood.EMBARRASSED; null -> activity.mood }
        if (activity.situation.isNotBlank()) Text(activity.situation, style = MaterialTheme.typography.bodyLarge)
        model.content.illustrations[activity.id].orEmpty().forEach { LocalIllustration(it) }
        activity.contentBlocks.forEach { ActivityMedia(it) }
        Text(activity.prompt, style = MaterialTheme.typography.titleLarge)
        PicoCompanion(activity.dialogue, mood)
        activity.choices.forEachIndexed { index, choice ->
            val selected = choice.id in run.selectedIds
            val shape = RoundedCornerShape(18.dp)
            val duration = if (LocalMotionEnabled.current) 150 else 0
            val cardColor by animateColorAsState(if (selected) Violet.copy(alpha = .16f) else Panel, tween(duration), label = "choice-tint")
            val scale by animateFloatAsState(if (selected) 1.012f else 1f, tween(duration), label = "choice-scale")
            // Selection styling is neutral and never derives from the answer key.
            Row(Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }.background(cardColor, shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) Violet else Violet.copy(alpha = .2f), shape)
                .selectable(selected, enabled = !run.submitted, role = if (activity.type == InteractionType.MULTIPLE_CHOICE) Role.Checkbox else Role.RadioButton,
                    onClick = { model.select(choice.id) }).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (activity.type == InteractionType.MULTIPLE_CHOICE) Checkbox(selected, onCheckedChange = null, enabled = !run.submitted)
                else RadioButton(selected, onClick = null, enabled = !run.submitted)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(listOf("الف", "ب", "ج", "د").getOrElse(index) { (index + 1).fa() }, color = Muted, style = MaterialTheme.typography.labelMedium)
                    Text(choice.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        activity.hint?.let { hint ->
            if (run.mode != RunMode.EXAM && !run.submitted) {
                TextButton(onClick = { showHint = !showHint }) { Text("سرنخ") }
                if (showHint) Text(hint)
            }
        }
        if (run.mode == RunMode.LESSON) Text("امتیاز: ${model.points(activity.id).fa()} از ${activity.basePoints.fa()}", color = Muted)
        if (!run.submitted) PrimaryButton("ثبت پاسخ", model::submit, run.selectedIds.isNotEmpty())
        else {
            if (feedback != null) FeedbackCard(feedback, showExplanation = run.mode == RunMode.DAILY)
            else Text("ثبت شد! نتیجه آخر آزمون میاد.", color = Muted)
            if (feedback?.correct == false) {
                PrimaryButton("یه بار دیگه", model::retry)
            } else {
                PrimaryButton(if (run.index == run.activityIds.lastIndex) "دیدن نتیجه" else "ادامه ←", model::next)
            }
        }
    }
    }
}

@Composable
private fun FeedbackCard(feedback: Feedback, showExplanation: Boolean = false) {
    SpaceCard {
        Text(if (feedback.correct) "خودشه!" else "یه جاش می‌لنگه!", fontWeight = FontWeight.Bold)
        feedback.choiceFeedback.forEach { Text(it) }
        if (!feedback.correct || showExplanation) Text(feedback.explanation, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ResultScreen(model: LearningViewModel, state: AppState) {
    val run = state.data.runs[state.activeRunId] ?: return
    val chapter = model.content.chapters.first { it.id == model.engine.current(run).chapterId }
    val passed = model.engine.passed(run)
    val challenge = model.dailyChallenge(run)
    val courseCompleted = model.chapterProgress().all { it.status == ChapterStatus.COMPLETED }
    ScreenColumn(if (run.mode == RunMode.DAILY) "نتیجهٔ چالش" else "نتیجهٔ مأموریت", { model.navigate(Screen.HOME) }) {
        Planet(Modifier.align(Alignment.CenterHorizontally).size(100.dp))
        if (challenge != null && run.mode == RunMode.DAILY) {
            PicoCompanion(challenge.closingDialogue, PicoMood.EMBARRASSED)
            SpaceCard {
                Text("✓ خرابکاری اصلاح شد", color = Teal, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(challenge.title, fontWeight = FontWeight.Bold)
                Text(challenge.summary, style = MaterialTheme.typography.bodyLarge)
                Text("این چالش تا ۷ روز پس از تکمیل دوباره انتخاب نمی‌شود.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        } else SpaceCard {
            Text(when {
                run.mode == RunMode.EXAM && passed && courseCompleted -> "هر هشت مأموریت تموم شد!"
                run.mode == RunMode.EXAM && passed -> "مأموریت تموم شد!"
                run.mode == RunMode.EXAM -> "یه دور دیگه بزنیم؟"
                run.mode == RunMode.DAILY -> "چالش امروز هم تموم شد!"
                else -> "بریم سراغ چالش آخر؟"
            }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("امتیاز مرحله: ${model.chapterPoints(chapter).fa()} از ${(chapter.lessonIds + chapter.examIds).sumOf { model.content.byId.getValue(it).basePoints }.fa()}")
            if (run.mode == RunMode.EXAM && !passed) Text("این بار می‌گیریمش!", color = Muted)
        }
        if (run.mode == RunMode.EXAM) run.activityIds.forEach { id ->
            val activity = model.content.byId.getValue(id)
            Text(activity.title, style = MaterialTheme.typography.titleLarge)
            Text(activity.prompt)
            val selected = run.results.first { it.activityId == id }.firstAttempt.selectedIds
            Text("پاسخ ثبت‌شدهٔ تو: " + activity.choices.filter { it.id in selected }.joinToString("\n") { it.text }, color = Muted)
            model.engine.feedback(run, id)?.let { FeedbackCard(it) }
            Text("امتیاز این فعالیت: ${model.points(id).fa()} از ${activity.basePoints.fa()}", color = Muted)
        }
        if (run.mode == RunMode.LESSON) PrimaryButton("شروع آزمون فصل", { model.startExam(chapter) })
        if (run.mode == RunMode.EXAM && passed) {
            model.nextChapter()?.let { nextChapter ->
                PrimaryButton("بریم مرحلهٔ بعد", { model.startChapter(nextChapter) })
            }
        }
        if (run.mode == RunMode.EXAM && !passed) PrimaryButton("تلاش دوباره در آزمون", model::repeatRun)
        else if (run.mode != RunMode.DAILY) OutlinedButton(onClick = model::repeatRun, modifier = Modifier.fillMaxWidth()) { Text("مرور دوباره") }
        PrimaryButton("بازگشت به خانه", { model.navigate(Screen.HOME) })
    }
}
