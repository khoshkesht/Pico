package ir.hooshamooz.engine

import java.time.LocalDate

data class Chapter(val id: String, val title: String, val subtitle: String, val lessonIds: List<String>, val examIds: List<String>)
enum class ChapterStatus { LOCKED, READY, IN_PROGRESS, COMPLETED }
data class ChapterProgress(val chapter: Chapter, val status: ChapterStatus, val completed: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else completed.toFloat() / total
}

object LearningProgress {
    fun chapters(chapters: List<Chapter>, completed: Set<String>, exams: Set<String>): List<ChapterProgress> {
        var previousCompleted = true
        return chapters.map { chapter ->
            val available = chapter.lessonIds.isNotEmpty() && chapter.examIds.isNotEmpty()
            val total = chapter.lessonIds.size + if (available) 1 else 0
            val done = chapter.lessonIds.count { it in completed } + if (chapter.id in exams && available) 1 else 0
            val status = when {
                !previousCompleted || !available -> ChapterStatus.LOCKED
                done == total -> ChapterStatus.COMPLETED
                done > 0 -> ChapterStatus.IN_PROGRESS
                else -> ChapterStatus.READY
            }
            previousCompleted = status == ChapterStatus.COMPLETED
            ChapterProgress(chapter, status, done, total)
        }
    }
}

data class DailyEntry(val day: LocalDate, val activityId: String, val completed: Boolean = false)

object DailyChallengeSelector {
    /** Persist the returned assignment; completion never re-rolls today's challenge. */
    fun choose(day: LocalDate, bank: List<Activity>, completedChapters: Set<String>, history: List<DailyEntry>): DailyEntry? {
        val eligible = bank.filter { it.chapterId in completedChapters }.sortedBy { it.id }
        history.lastOrNull { it.day == day && entryMatches(it, eligible) }?.let { return it }
        if (eligible.isEmpty()) return null
        val done = history.filter { it.completed }.map { it.activityId }.toSet()
        val pool = eligible.filterNot { it.id in done }.ifEmpty { eligible }
        return DailyEntry(day, pool[Math.floorMod(day.toEpochDay(), pool.size.toLong()).toInt()].id)
    }

    private fun entryMatches(entry: DailyEntry, activities: List<Activity>) = activities.any { it.id == entry.activityId }
}
