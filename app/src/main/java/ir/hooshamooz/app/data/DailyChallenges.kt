package ir.hooshamooz.app.data

import ir.hooshamooz.engine.DailyEntry
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class DailyChallenge(
    val id: String,
    val chapterId: String,
    val title: String,
    val estimatedMinutes: Int,
    val activityIds: List<String>,
    val closingDialogue: String,
    val summary: String,
)

/** App scheduling policy; the interaction engine still runs ordinary choice activities. */
object DailyChallengeSchedule {
    const val REVIEW_DAYS = 7L
    const val MAX_HISTORY = 128
    const val MAX_ATTEMPTS = 32
    fun choose(day: LocalDate, bank: List<DailyChallenge>, completedChapters: Set<String>,
        history: List<DailyEntry>, completedOn: Map<LocalDate, LocalDate>): DailyEntry? {
        val eligible = bank.filter { it.chapterId in completedChapters }.sortedBy { it.id }
        // A persisted assignment wins even when another chapter is passed that day.
        history.lastOrNull { entry -> entry.day == day && eligible.any { it.id == entry.activityId } }?.let { return it }
        val completions = history.filter { it.completed }.groupBy { it.activityId }
        val fresh = eligible.filter { completions[it.id].isNullOrEmpty() }
        val due = eligible.filter { challenge ->
            val done = completions[challenge.id].orEmpty()
            if (done.isEmpty()) false else {
                val latest = done.maxOf { completedOn[it.day] ?: it.day }
                ChronoUnit.DAYS.between(latest, day) >= REVIEW_DAYS
            }
        }
        val pool = fresh.ifEmpty { due }
        if (pool.isEmpty()) return null
        // Choose one eligible stage, then one challenge in it, deterministically per date.
        val stages = pool.groupBy { it.chapterId }.toSortedMap().values.toList()
        val stage = stages[Math.floorMod(day.toEpochDay(), stages.size.toLong()).toInt()]
        val challenge = stage[Math.floorMod(day.toEpochDay() / stages.size, stage.size.toLong()).toInt()]
        return DailyEntry(day, challenge.id)
    }

    fun trim(data: LearningData, validIds: Set<String>): LearningData {
        val valid = data.dailyHistory.filter { it.activityId in validIds }.distinctBy { it.day }.sortedBy { it.day }
        // Keep each challenge's latest completion even after old calendar entries are pruned.
        val latest = valid.filter { it.completed }.groupBy { it.activityId }.values.map { entries ->
            entries.maxBy { data.dailyCompletedOn[it.day] ?: it.day }
        }.toSet()
        require(latest.size <= MAX_HISTORY)
        val recent = valid.filterNot { it in latest }.takeLast(MAX_HISTORY - latest.size)
        val kept = (latest + recent).sortedBy { it.day }
        val days = kept.map { it.day }.toSet()
        return data.copy(dailyHistory = kept, dailyCompletedOn = data.dailyCompletedOn.filterKeys { it in days })
    }
}
