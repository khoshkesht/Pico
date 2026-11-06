package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.time.*

class DailyChallengesTest {
    private val properties = listOf("stageContentFile", "stageTwoContentFile", "stageThreeContentFile", "stageFourContentFile",
        "stageFiveContentFile", "stageSixContentFile", "stageSevenContentFile", "stageEightContentFile")
    private fun raw(name: String) = File(requireNotNull(System.getProperty(name))).readText()
    private fun content() = LearningContent.parseStages(properties.map(::raw), raw("dailyContentFile"))
    private class Repository(var data: LearningData = LearningData()) : LearningRepository {
        override fun read() = LearningCodec.decode(LearningCodec.encode(data))
        override fun save(data: LearningData) { this.data = LearningCodec.decode(LearningCodec.encode(data)) }
        override fun reset() { data = LearningData() }
    }
    private class DayClock(var day: LocalDate = LocalDate.of(2026, 11, 6)) : Clock() {
        override fun getZone(): ZoneId = ZoneId.of("Asia/Tehran")
        override fun instant(): Instant = day.atStartOfDay(zone).toInstant()
        override fun withZone(zone: ZoneId): Clock = fixed(instant(), zone)
    }
    private fun answer(m: LearningViewModel, correct: Boolean = true) {
        val activity = m.engine.current(m.activeRun()!!)
        (if (correct) activity.correctIds else setOf(activity.choices.first { it.id !in activity.correctIds }.id)).forEach(m::select)
        m.submit()
    }
    private fun finishChapter(m: LearningViewModel, chapter: Chapter) {
        m.startChapter(chapter)
        repeat(chapter.lessonIds.size) { answer(m); m.next() }
        m.startExam(chapter); answer(m); m.next()
    }
    private fun finishDaily(m: LearningViewModel) {
        m.startDaily(); repeat(2) { answer(m); m.next() }
    }

    @Test fun `24 independent two step challenges load with short Pico dialogue and no new interaction or media types`() {
        val c = content()
        assertEquals(54, c.activities.size)
        assertEquals(24, c.dailyChallenges.size)
        assertEquals(48, c.dailyActivities.size)
        assertEquals(102, c.byId.size)
        assertTrue(c.dailyChallenges.groupBy { it.chapterId }.values.all { it.size == 3 })
        assertTrue(c.dailyChallenges.all { it.estimatedMinutes == 2 && it.summary.isNotBlank() && it.closingDialogue.length <= 81 })
        assertTrue(c.dailyActivities.all { it.type == InteractionType.SINGLE_CHOICE && it.basePoints == 0 && it.wrongPenalty == 0 })
        assertTrue(c.dailyActivities.all { it.dialogue.length in 1..81 && it.situation.isNotBlank() && it.explanation.isNotBlank() })
        assertTrue(c.dailyActivities.all { it.contentBlocks.all { b -> b is ContentBlock.Text } })
        assertTrue(c.dailyActivities.none { d -> c.activities.any { it.id == d.id || it.prompt == d.prompt && it.choices == d.choices } })
        c.dailyChallenges.forEach { challenge ->
            assertEquals(listOf(PicoMood.CONFIDENT, PicoMood.CONFUSED), challenge.activityIds.map { c.byId.getValue(it).mood })
            assertTrue(challenge.activityIds.all { c.byId.getValue(it).chapterId == challenge.chapterId })
        }
    }

    @Test fun `daily challenge unlocks only after passing an exam and today's assignment survives more chapter completions`() {
        val c = content(); val m = LearningViewModel(Repository(), c, DayClock())
        assertNull(m.today())
        m.startChapter(c.chapters[0]); repeat(6) { answer(m); m.next() }
        assertNull(m.today())
        m.startExam(c.chapters[0]); answer(m, false); m.next()
        assertNull(m.today())
        m.repeatRun(); answer(m); m.next()
        val today = m.today()!!
        assertEquals(c.chapters[0].id, c.dailyById.getValue(today.activityId).chapterId)
        finishChapter(m, c.chapters[1])
        assertEquals(today, m.today())
        repeat(10) { m.navigate(Screen.HOME); m.refreshDay(); assertEquals(today, m.today()) }
    }

    @Test fun `both steps need correct answers and partial repair resumes without changing lesson points`() {
        val c = content(); val repo = Repository(); val clock = DayClock()
        var m = LearningViewModel(repo, c, clock)
        finishChapter(m, c.chapters[0]); val points = m.totalPoints()
        m.startDaily(); assertEquals(2, m.activeRun()!!.activityIds.size)
        answer(m, false); m.next()
        assertEquals(0, m.activeRun()!!.index); assertFalse(m.today()!!.completed)
        m.retry(); answer(m); m.next()
        assertFalse(m.today()!!.completed)
        m.engine.current(m.activeRun()!!).correctIds.forEach(m::select)
        val selection = m.activeRun()!!.selectedIds
        m = LearningViewModel(repo, c, clock); m.startDaily()
        assertEquals(1, m.activeRun()!!.index); assertEquals(selection, m.activeRun()!!.selectedIds)
        m.submit(); m.next()
        assertTrue(m.today()!!.completed)
        assertEquals(clock.day, repo.data.dailyCompletedOn[clock.day])
        assertEquals(points, m.totalPoints())
        assertFalse(c.dailyActivities.any { it.id in repo.data.masteredActivities || it.id in repo.data.wrongSubmissions })
        val finished = m.activeRun()
        m.repeatRun(); assertEquals(finished, m.activeRun())
        m.navigate(Screen.HOME); m.startDaily()
        assertEquals(Screen.RESULT, m.state.value.screen); assertEquals(finished, m.activeRun())
        clock.day = clock.day.plusDays(1); m.refreshDay()
        assertNotEquals(finished!!.activityIds, m.todayChallenge()!!.activityIds)
    }

    @Test fun `after three fresh challenges there is an honest empty period and the first review is exactly seven days later`() {
        val clock = DayClock(); val start = clock.day; val c = content(); val repo = Repository()
        var m = LearningViewModel(repo, c, clock); finishChapter(m, c.chapters[0])
        val used = mutableListOf<String>()
        repeat(3) { offset ->
            clock.day = start.plusDays(offset.toLong()); m.refreshDay()
            used += m.today()!!.activityId; finishDaily(m)
        }
        assertEquals(3, used.distinct().size)
        for (offset in 3..6) {
            clock.day = start.plusDays(offset.toLong())
            m = LearningViewModel(repo, c, clock)
            assertNull(m.today()); assertNull(m.todayChallenge())
            m.startDaily(); assertNull(m.activeRun())
        }
        clock.day = start.plusDays(7); m.refreshDay()
        assertEquals(used.first(), m.today()!!.activityId)
        finishDaily(m)
        assertEquals(start.plusDays(7), repo.data.dailyCompletedOn[clock.day])
    }

    @Test fun `every review is seven days from actual completion even after many reviews or an overnight completion`() {
        val c = content(); val challenge = c.dailyChallenges.first(); val assigned = LocalDate.of(2026, 1, 1)
        val actual = assigned.plusDays(2)
        val history = (0..30).map { DailyEntry(assigned.minusDays(it * 10L), challenge.id, true) }
        val completedOn = mapOf(assigned to actual)
        assertNull(DailyChallengeSchedule.choose(actual.plusDays(6), listOf(challenge), setOf(challenge.chapterId), history, completedOn))
        assertEquals(challenge.id, DailyChallengeSchedule.choose(actual.plusDays(7), listOf(challenge), setOf(challenge.chapterId), history, completedOn)!!.activityId)
        // No arithmetic overflow at extreme calendar dates.
        assertNull(DailyChallengeSchedule.choose(LocalDate.MIN, listOf(challenge), setOf(challenge.chapterId),
            listOf(DailyEntry(LocalDate.MAX, challenge.id, true)), emptyMap()))
    }

    @Test fun `history is capped but keeps the latest completion of every challenge after years of use`() {
        val c = content(); val origin = LocalDate.of(2000, 1, 1)
        val old = c.dailyChallenges.mapIndexed { index, challenge -> DailyEntry(origin.plusDays(index.toLong()), challenge.id, true) }
        val recent = (1..5000).map { DailyEntry(origin.plusDays(30L + it), c.dailyChallenges.first().id, false) }
        val data = LearningData(dailyHistory = old + recent, dailyCompletedOn = old.associate { it.day to it.day })
        val trimmed = DailyChallengeSchedule.trim(data, c.dailyById.keys)
        assertEquals(128, trimmed.dailyHistory.size)
        assertEquals(c.dailyById.keys, trimmed.dailyHistory.filter { it.completed }.map { it.activityId }.toSet())
        assertEquals(24, trimmed.dailyCompletedOn.size)
        assertEquals(trimmed, LearningCodec.decode(LearningCodec.encode(trimmed)))
    }

    @Test fun `adding the independent bank preserves old points unfinished lessons and discards obsolete daily runs`() {
        val old = LearningContent.parseStages(properties.map(::raw)); val repo = Repository(); val clock = DayClock()
        val before = LearningViewModel(repo, old, clock)
        finishChapter(before, old.chapters[0]); before.startDaily()
        before.startChapter(old.chapters[1]); answer(before, false); before.retry(); before.select("b")
        val score = before.totalPoints()
        val after = LearningViewModel(repo, content(), clock)
        after.continueLearning()
        assertEquals(score, after.totalPoints()); assertEquals(40, after.points("stage02-activity01"))
        assertEquals(setOf("b"), after.activeRun()!!.selectedIds)
        assertEquals(setOf("capabilities"), repo.data.completedExams)
        assertTrue(repo.data.runs.values.none { it.mode == RunMode.DAILY })
        assertTrue(after.today()!!.activityId.startsWith("daily01-"))
        after.reset(); assertNull(after.today()); assertTrue(repo.data.dailyCompletedOn.isEmpty())
    }

    @Test fun `daily retry history stays bounded and still resumes and finishes correctly`() {
        val c = content(); val repo = Repository(); val clock = DayClock()
        var m = LearningViewModel(repo, c, clock)
        finishChapter(m, c.chapters[0]); m.startDaily()
        repeat(100) { answer(m, false); m.retry() }
        assertEquals(32, m.activeRun()!!.results.single().attempts.size)
        m = LearningViewModel(repo, c, clock); m.startDaily()
        assertEquals(0, m.activeRun()!!.index)
        answer(m); m.next(); answer(m); m.next()
        assertTrue(m.today()!!.completed)
        assertEquals(350, m.totalPoints())
    }
}
