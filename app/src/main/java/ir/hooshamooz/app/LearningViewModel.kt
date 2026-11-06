package ir.hooshamooz.app

import androidx.lifecycle.ViewModel
import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.Clock

enum class Screen { HOME, PATH, SETTINGS, ACTIVITY, RESULT }
data class AppState(val screen: Screen = Screen.HOME, val data: LearningData = LearningData(), val activeRunId: String? = null, val day: LocalDate = LocalDate.now(), val greetingIndex: Int = PicoGreetings.next(), val homeMood: PicoMood = PicoGreetings.nextMood(), val homeTitleIndex: Int = PicoGreetings.nextTitle())

class LearningViewModel(private val repository: LearningRepository, val content: LearningContent,
    private val clock: Clock = Clock.systemDefaultZone()) : ViewModel() {
    val engine = InteractionEngine(content.byId.values.toList(), content.version)
    private fun resumable(run: RunState) = engine.canResume(run) &&
        (run.mode != RunMode.DAILY || content.dailyChallenges.isEmpty() || content.challengeFor(run.activityIds) != null)
    private val mutable = MutableStateFlow(AppState(data = repository.read().let { data ->
        when {
            data.contentVersion == content.version -> data.copy(runs = data.runs.filterValues(::resumable))
            data.contentVersion in content.compatibleVersions -> {
                val runs = data.runs.mapValues { (_, run) -> run.copy(contentVersion = content.version) }.filterValues(::resumable)
                data.copy(contentVersion = content.version, runs = runs,
                    lastRunId = data.lastRunId?.takeIf { it in runs },
                    completedLessons = data.completedLessons.filter { it in content.byId }.toSet(),
                    masteredActivities = data.masteredActivities.filter { it in content.byId }.toSet(),
                    wrongSubmissions = data.wrongSubmissions.filterKeys { it in content.byId })
            }
            else -> LearningData(textScale = data.textScale, contentVersion = content.version)
        }
    }))
    val state = mutable.asStateFlow()
    init { save(state.value.data); refreshDay() }
    fun chapterProgress() = LearningProgress.chapters(content.chapters, state.value.data.completedLessons, state.value.data.completedExams)
    fun activeRun() = state.value.data.runs[state.value.activeRunId]
    fun continueRun() = state.value.data.let { data -> data.runs[data.lastRunId]?.takeIf { !it.finished && it.mode != RunMode.DAILY } }
    fun nextChapter() = chapterProgress().firstOrNull { it.status == ChapterStatus.READY || it.status == ChapterStatus.IN_PROGRESS }?.chapter
    fun journeyBackground(): String {
        val chapter = nextChapter() ?: chapterProgress().lastOrNull { it.status == ChapterStatus.COMPLETED }?.chapter
        return content.chapterBackgrounds[chapter?.id] ?: "METEORS"
    }
    fun today() = state.value.data.dailyHistory.lastOrNull { it.day == state.value.day &&
        (content.dailyChallenges.isEmpty() || it.activityId in content.dailyById) }
    fun todayChallenge() = today()?.let { content.dailyById[it.activityId] }
    fun dailyChallenge(run: RunState) = content.challengeFor(run.activityIds)
    fun points(id: String) = content.byId.getValue(id).points(state.value.data.wrongSubmissions[id] ?: 0)
    fun totalPoints() = state.value.data.masteredActivities.filter { it in content.byId }.sumOf(::points)
    fun chapterPoints(chapter: Chapter) = (chapter.lessonIds + chapter.examIds).filter { it in state.value.data.masteredActivities }.sumOf(::points)
    fun navigate(screen: Screen) {
        val greeting = if (screen == Screen.HOME && state.value.screen != Screen.HOME) PicoGreetings.next(state.value.greetingIndex) else state.value.greetingIndex
        val mood = if (screen == Screen.HOME && state.value.screen != Screen.HOME) PicoGreetings.nextMood(state.value.homeMood) else state.value.homeMood
        val title = if (screen == Screen.HOME && state.value.screen != Screen.HOME) PicoGreetings.nextTitle(state.value.homeTitleIndex) else state.value.homeTitleIndex
        mutable.value = state.value.copy(screen = screen, greetingIndex = greeting, homeMood = mood, homeTitleIndex = title)
        refreshDay()
    }
    private fun save(data: LearningData) {
        val trimmed = if (content.dailyChallenges.isEmpty()) data else DailyChallengeSchedule.trim(data, content.dailyById.keys)
        val newestRuns = trimmed.runs.values.filter { it.mode == RunMode.DAILY }
            .sortedByDescending { it.runId }.take(DailyChallengeSchedule.MAX_HISTORY).map { it.runId }.toSet()
        val bounded = trimmed.copy(runs = trimmed.runs.filter { (id, run) ->
            run.mode != RunMode.DAILY || id in newestRuns || id == state.value.activeRunId
        }.mapValues { (_, run) ->
            if (run.mode != RunMode.DAILY) run else run.copy(results = run.results.map { result ->
                if (result.attempts.size <= DailyChallengeSchedule.MAX_ATTEMPTS) result
                else result.copy(attempts = result.attempts.take(1) + result.attempts.takeLast(DailyChallengeSchedule.MAX_ATTEMPTS - 1))
            })
        }, examHistory = trimmed.examHistory.takeLast(128))
        repository.save(bounded); mutable.value = state.value.copy(data = bounded)
    }
    fun refreshDay() {
        val day = LocalDate.now(clock)
        mutable.value = state.value.copy(day = day)
        val data = state.value.data
        val entry = if (content.dailyChallenges.isEmpty()) {
            DailyChallengeSelector.choose(day, content.daily, data.completedExams, data.dailyHistory)
        } else DailyChallengeSchedule.choose(day, content.dailyChallenges, data.completedExams, data.dailyHistory, data.dailyCompletedOn)
        if (entry == null) return
        if (data.dailyHistory.none { it.day == day && it.activityId == entry.activityId }) save(data.copy(dailyHistory = data.dailyHistory.filterNot { it.day == day } + entry))
    }
    fun continueLearning() {
        continueRun()?.let { open(it); return }
        nextChapter()?.let(::startChapter)
    }
    fun startChapter(chapter: Chapter, replay: Boolean = false) {
        if (chapterProgress().first { it.chapter.id == chapter.id }.status == ChapterStatus.LOCKED) return
        if (!replay && chapter.lessonIds.all { it in state.value.data.completedLessons } && chapter.id !in state.value.data.completedExams) { startExam(chapter); return }
        val id = "LESSON:${chapter.id}"
        open(state.value.data.runs[id]?.takeIf { !it.finished } ?: engine.start(id, RunMode.LESSON, chapter.lessonIds))
    }
    fun startDaily() {
        refreshDay()
        val entry = today() ?: return
        val id = "DAILY:${entry.day}"
        val saved = state.value.data.runs[id]
        if (entry.completed && saved?.finished == true) {
            mutable.value = state.value.copy(screen = Screen.RESULT, activeRunId = id)
            return
        }
        if (entry.completed) return
        val ids = content.dailyById[entry.activityId]?.activityIds ?: listOf(entry.activityId)
        open(saved?.takeIf { !it.finished && it.activityIds == ids } ?: engine.start(id, RunMode.DAILY, ids))
    }
    fun startExam(chapter: Chapter) {
        if (chapter.examIds.isEmpty() || !chapter.lessonIds.all { it in state.value.data.completedLessons }) return
        val id = "EXAM:${chapter.id}"
        open(state.value.data.runs[id]?.takeIf { !it.finished } ?: engine.start(id, RunMode.EXAM, chapter.examIds))
    }
    private fun open(run: RunState) {
        save(state.value.data.copy(runs = state.value.data.runs + (run.runId to run), lastRunId = if (run.mode == RunMode.DAILY) state.value.data.lastRunId else run.runId))
        mutable.value = state.value.copy(screen = Screen.ACTIVITY, activeRunId = run.runId)
    }
    private fun updateRun(transform: (RunState) -> RunState) {
        val run = transform(activeRun() ?: return)
        save(state.value.data.copy(runs = state.value.data.runs + (run.runId to run)))
    }
    fun select(id: String) {
        val run = activeRun() ?: return
        if (!run.finished && !run.submitted) updateRun { engine.select(it, id) }
    }
    fun retry() {
        val run = activeRun() ?: return
        if (!run.finished && run.submitted && run.mode != RunMode.EXAM) updateRun { engine.retry(it) }
    }
    fun submit() {
        val previous = activeRun() ?: return
        if (previous.finished || previous.submitted || previous.selectedIds.isEmpty()) return
        val run = engine.submit(previous)
        val activity = engine.current(run)
        var data = state.value.data.copy(runs = state.value.data.runs + (run.runId to run))
        // Exam points and correctness remain hidden until the run ends.
        if (run.mode == RunMode.LESSON) data = recordAnswer(data, activity.id, run.results.last().attempts.last().correct)
        save(data)
    }
    private fun recordAnswer(data: LearningData, id: String, correct: Boolean): LearningData =
        if (correct) data.copy(masteredActivities = data.masteredActivities + id)
        else data.copy(wrongSubmissions = data.wrongSubmissions + (id to ((data.wrongSubmissions[id] ?: 0).coerceIn(0, 999_999) + 1)))
    fun next() {
        val previous = activeRun() ?: return
        if (previous.finished || !previous.submitted) return
        if (previous.mode != RunMode.EXAM && !previous.results.last().attempts.last().correct) return
        val run = engine.next(previous)
        val activity = engine.current(previous)
        var data = state.value.data.copy(runs = state.value.data.runs + (run.runId to run))
        if (run.mode == RunMode.LESSON) data = data.copy(completedLessons = data.completedLessons + activity.id)
        if (run.finished) {
            if (run.mode == RunMode.EXAM) {
                run.results.forEach { data = recordAnswer(data, it.activityId, it.firstAttempt.correct) }
                data = data.copy(completedExams = if (engine.passed(run)) data.completedExams + activity.chapterId else data.completedExams,
                    examHistory = data.examHistory + ExamRecord(activity.chapterId, run.activityIds.sumOf { content.byId.getValue(it).points(data.wrongSubmissions[it] ?: 0) }, run.activityIds.sumOf { content.byId.getValue(it).basePoints }, System.currentTimeMillis()))
            }
            if (run.mode == RunMode.DAILY) {
                val entry = data.dailyHistory.firstOrNull { "DAILY:${it.day}" == run.runId }
                if (entry != null && !entry.completed) data = data.copy(
                    dailyHistory = data.dailyHistory.map { if (it.day == entry.day) it.copy(completed = true) else it },
                    dailyCompletedOn = data.dailyCompletedOn + (entry.day to LocalDate.now(clock)))
            }
        }
        save(data) // Session, deductions and completion form one snapshot.
        if (run.finished) mutable.value = state.value.copy(screen = Screen.RESULT)
        refreshDay()
    }
    fun repeatRun() { val run = activeRun() ?: return; if (run.mode != RunMode.DAILY) open(engine.start(run.runId, run.mode, run.activityIds)) }
    fun setTextScale(scale: Float) = save(state.value.data.copy(textScale = scale.coerceIn(1f, 1.3f)))
    fun reset() { repository.reset(); mutable.value = AppState(data = LearningData(contentVersion = content.version)); repository.save(state.value.data); refreshDay() }
}
