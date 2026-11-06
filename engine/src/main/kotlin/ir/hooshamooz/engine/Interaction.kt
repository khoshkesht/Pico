package ir.hooshamooz.engine

enum class InteractionType { SINGLE_CHOICE, MULTIPLE_CHOICE, COMPARE_RESPONSES }
enum class PicoMood { NORMAL, CONFUSED, CONFIDENT, SURPRISED, EMBARRASSED, HAPPY }
enum class RunMode { LESSON, EXAM, DAILY }

data class Choice(val id: String, val text: String, val feedback: String)
sealed interface ContentBlock {
    data class Text(val title: String, val text: String) : ContentBlock
    data class Cups(val description: String) : ContentBlock
    data class VoiceMessage(val transcript: String, val description: String) : ContentBlock
}

/** Internal runtime contract, not the future chapter-file format. */
data class Activity(
    val id: String,
    val chapterId: String,
    val type: InteractionType,
    val situation: String,
    val dialogue: String,
    val mood: PicoMood,
    val choices: List<Choice>,
    val correctIds: Set<String>,
    val explanation: String,
    val hint: String? = null,
    val title: String = "",
    val prompt: String = "",
    val contentBlocks: List<ContentBlock> = emptyList(),
    val basePoints: Int = 50,
    val wrongPenalty: Int = 10,
) {
    init {
        require(id.isNotBlank() && chapterId.isNotBlank())
        require(choices.size >= 2 && choices.map { it.id }.toSet().size == choices.size)
        require(choices.all { it.id.isNotBlank() })
        require(correctIds.isNotEmpty() && correctIds.all { id -> choices.any { it.id == id } })
        require(type == InteractionType.MULTIPLE_CHOICE || correctIds.size == 1)
        require(type != InteractionType.COMPARE_RESPONSES || choices.size == 2)
        require(basePoints >= 0 && wrongPenalty >= 0)
    }
    fun points(wrongSubmissions: Int): Int = (basePoints.toLong() - wrongPenalty.toLong() * wrongSubmissions.coerceAtLeast(0)).coerceAtLeast(0).toInt()
}

data class Attempt(val selectedIds: Set<String>, val correct: Boolean)
data class Feedback(val correct: Boolean, val choiceFeedback: List<String>, val explanation: String)
data class ActivityResult(val activityId: String, val attempts: List<Attempt>) {
    val firstAttempt: Attempt get() = attempts.first()
}
data class RunState(
    val runId: String,
    val mode: RunMode,
    val activityIds: List<String>,
    val index: Int = 0,
    val selectedIds: Set<String> = emptySet(),
    val results: List<ActivityResult> = emptyList(),
    val submitted: Boolean = false,
    val finished: Boolean = false,
    val contentVersion: String = "1",
)

/** Pure state machine: no Android, network, UI or persistence dependencies. */
class InteractionEngine(activities: List<Activity>, private val contentVersion: String = "1") {
    private val bank = activities.associateBy { it.id }
    init { require(bank.size == activities.size) { "Duplicate activity IDs" } }

    fun start(runId: String, mode: RunMode, ids: List<String>): RunState {
        require(runId.isNotBlank() && ids.isNotEmpty() && ids.distinct().size == ids.size)
        require(ids.all { it in bank })
        return RunState(runId, mode, ids.toList(), contentVersion = contentVersion)
    }

    fun current(state: RunState): Activity = bank.getValue(state.activityIds[state.index])

    fun select(state: RunState, choiceId: String): RunState {
        check(!state.finished && !state.submitted)
        val activity = current(state)
        require(activity.choices.any { it.id == choiceId })
        val ids = if (activity.type == InteractionType.MULTIPLE_CHOICE) {
            if (choiceId in state.selectedIds) state.selectedIds - choiceId else state.selectedIds + choiceId
        } else setOf(choiceId)
        return state.copy(selectedIds = ids)
    }

    fun submit(state: RunState): RunState {
        check(!state.finished && !state.submitted && state.selectedIds.isNotEmpty())
        val activity = current(state)
        val attempt = Attempt(state.selectedIds.toSet(), state.selectedIds == activity.correctIds)
        val existing = state.results.firstOrNull { it.activityId == activity.id }
        val result = ActivityResult(activity.id, existing?.attempts.orEmpty() + attempt)
        return state.copy(
            submitted = true,
            results = state.results.filterNot { it.activityId == activity.id } + result,
        )
    }

    /** Exam answers stay hidden until the entire run has ended. */
    fun feedback(state: RunState, activityId: String = current(state).id): Feedback? {
        if (state.mode == RunMode.EXAM && !state.finished) return null
        if (!state.finished && !state.submitted) return null
        val result = state.results.firstOrNull { it.activityId == activityId } ?: return null
        val attempt = if (state.mode == RunMode.EXAM) result.firstAttempt else result.attempts.last()
        val activity = bank.getValue(activityId)
        return Feedback(attempt.correct,
            activity.choices.filter { it.id in attempt.selectedIds }.map { it.feedback }, activity.explanation)
    }

    fun retry(state: RunState): RunState {
        check(state.mode != RunMode.EXAM && state.submitted && !state.finished)
        return state.copy(submitted = false, selectedIds = emptySet())
    }

    fun next(state: RunState): RunState {
        check(state.submitted && !state.finished)
        check(state.mode == RunMode.EXAM || state.results.last().attempts.last().correct) { "A correct response is required to advance" }
        return if (state.index == state.activityIds.lastIndex) state.copy(finished = true)
        else state.copy(index = state.index + 1, selectedIds = emptySet(), submitted = false)
    }

    fun firstAttemptScore(state: RunState): Int = state.results.count { it.firstAttempt.correct }
    fun passed(state: RunState): Boolean = state.finished && state.results.size == state.activityIds.size && state.results.all { it.attempts.last().correct }

    /** Validate persisted snapshots before allowing a resume after a content update. */
    fun canResume(state: RunState): Boolean = runCatching {
        require(state.runId.isNotBlank())
        require(state.contentVersion == contentVersion)
        require(state.activityIds.isNotEmpty() && state.activityIds.distinct().size == state.activityIds.size)
        require(state.activityIds.all { it in bank } && state.index in state.activityIds.indices)
        require(state.results.map { it.activityId }.distinct().size == state.results.size)
        val hasCurrentResult = state.results.any { it.activityId == current(state).id }
        val expected = state.activityIds.take(state.index + if (hasCurrentResult) 1 else 0)
        require(state.results.map { it.activityId } == expected)
        if (state.submitted) require(hasCurrentResult)
        if (state.mode == RunMode.EXAM && !state.submitted) require(!hasCurrentResult)
        state.results.forEach { result ->
            val activity = bank.getValue(result.activityId)
            require(result.attempts.isNotEmpty())
            require(state.mode != RunMode.EXAM || result.attempts.size == 1)
            result.attempts.forEach { attempt ->
                require(validSelection(activity, attempt.selectedIds))
                require(attempt.correct == (attempt.selectedIds == activity.correctIds))
            }
            if (state.mode != RunMode.EXAM && result.activityId in state.activityIds.take(state.index)) require(result.attempts.last().correct)
        }
        require(state.selectedIds.isEmpty() || validSelection(current(state), state.selectedIds))
        if (state.submitted) require(state.selectedIds == state.results.last().attempts.last().selectedIds)
        if (state.finished) {
            require(state.submitted && state.index == state.activityIds.lastIndex)
            if (state.mode != RunMode.EXAM) require(state.results.all { it.attempts.last().correct })
        }
    }.isSuccess

    private fun validSelection(activity: Activity, ids: Set<String>) = ids.isNotEmpty() &&
        ids.all { id -> activity.choices.any { it.id == id } } &&
        (activity.type == InteractionType.MULTIPLE_CHOICE || ids.size == 1)
}
