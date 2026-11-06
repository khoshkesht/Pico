package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StageTwoTest {
    private val firstRaw get() = File(requireNotNull(System.getProperty("stageContentFile"))).readText()
    private val secondRaw get() = File(requireNotNull(System.getProperty("stageTwoContentFile"))).readText()
    private fun course() = LearningContent.parseStages(listOf(secondRaw, firstRaw))
    private class Repository(var data: LearningData = LearningData()) : LearningRepository {
        override fun read() = LearningCodec.decode(LearningCodec.encode(data))
        override fun save(data: LearningData) { this.data = LearningCodec.decode(LearningCodec.encode(data)) }
        override fun reset() { data = LearningData() }
    }
    private fun answer(m: LearningViewModel, correct: Boolean = true) {
        val activity = m.engine.current(m.activeRun()!!)
        val ids = if (correct) activity.correctIds else setOf(activity.choices.first { it.id !in activity.correctIds }.id)
        ids.forEach(m::select); m.submit()
    }
    private fun finishChapter(m: LearningViewModel, chapter: Chapter) {
        m.startChapter(chapter)
        repeat(chapter.lessonIds.size) { answer(m); m.next() }
        m.startExam(chapter); answer(m); m.next()
    }

    @Test fun `second chapter uses existing interaction types no new assets and short feedback`() {
        val c = course()
        val stage = c.chapters[1]
        val activities = c.activities.filter { it.chapterId == "prompt" }
        assertEquals(14, c.activities.size)
        assertEquals(6, stage.lessonIds.size)
        assertEquals(1, stage.examIds.size)
        assertEquals(7, activities.size)
        assertEquals(InteractionType.entries.toSet(), activities.map { it.type }.toSet())
        assertTrue(activities.all { it.contentBlocks.isEmpty() && it.choices.all { option -> option.feedback.length in 1..110 } })
        assertFalse(Regex("(^|\\s)(دو|۲|2)(\\s|$)").containsMatchIn(activities.last().prompt))
        assertEquals(setOf(2, 3), activities.filter { it.type == InteractionType.MULTIPLE_CHOICE }.map { it.correctIds.size }.toSet())
    }

    @Test fun `second chapter unlocks only after correct first chapter exam and next is selected`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        m.startChapter(c.chapters[1]); assertNull(m.activeRun())
        m.startChapter(c.chapters[0])
        repeat(6) { answer(m); m.next() }
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[1].status)
        m.startExam(c.chapters[0]); answer(m, false); m.next()
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[1].status)
        m.repeatRun(); answer(m); m.next()
        assertEquals(ChapterStatus.READY, m.chapterProgress()[1].status)
        assertEquals("prompt", m.nextChapter()!!.id)
    }

    @Test fun `adding chapter preserves actual first chapter score progress and unfinished choices`() {
        val repo = Repository()
        val oldContent = LearningContent.parse(firstRaw)
        var m = LearningViewModel(repo, oldContent)
        m.startChapter(oldContent.chapters.first()); answer(m, false); m.retry(); answer(m); m.next()
        m.select("a")
        val oldScore = m.totalPoints()
        val oldCompleted = m.state.value.data.completedLessons
        m = LearningViewModel(repo, course()); m.continueLearning()
        assertEquals(oldScore, m.totalPoints())
        assertEquals(oldCompleted, m.state.value.data.completedLessons)
        assertEquals(1, m.activeRun()!!.index)
        assertEquals(setOf("a"), m.activeRun()!!.selectedIds)
        assertEquals(40, m.points(oldContent.activities.first().id))
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[1].status)
    }

    @Test fun `adding chapter preserves first chapter completion and opens second`() {
        val repo = Repository(); val old = LearningContent.parse(firstRaw)
        val before = LearningViewModel(repo, old); finishChapter(before, old.chapters.first())
        val after = LearningViewModel(repo, course())
        assertEquals(350, after.totalPoints())
        assertEquals(ChapterStatus.COMPLETED, after.chapterProgress()[0].status)
        assertEquals(ChapterStatus.READY, after.chapterProgress()[1].status)
    }

    @Test fun `second chapter scores independently resumes and requires correct exam`() {
        val c = course(); val repo = Repository(); var m = LearningViewModel(repo, c)
        finishChapter(m, c.chapters[0]); m.startChapter(c.chapters[1])
        val id = c.chapters[1].lessonIds.first()
        answer(m, false); m.next(); assertEquals(0, m.activeRun()!!.index)
        m.retry(); m = LearningViewModel(repo, c); m.continueLearning()
        assertEquals(40, m.points(id)); assertEquals(350, m.totalPoints())
        repeat(6) { answer(m); m.next() }
        m.startExam(c.chapters[1]); answer(m, false)
        assertNull(m.engine.feedback(m.activeRun()!!))
        m.next(); assertEquals(ChapterStatus.IN_PROGRESS, m.chapterProgress()[1].status)
        m.repeatRun(); answer(m); m.next()
        assertEquals(ChapterStatus.COMPLETED, m.chapterProgress()[1].status)
        assertEquals(680, m.totalPoints())
        assertEquals(330, m.chapterPoints(c.chapters[1]))
        assertEquals(350, m.chapterPoints(c.chapters[0]))
    }

    @Test fun `daily bank stays inside completed chapters and does not reroll same day`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        finishChapter(m, c.chapters[0])
        val today = m.today()!!
        assertEquals("capabilities", c.byId.getValue(today.activityId).chapterId)
        finishChapter(m, c.chapters[1])
        assertEquals(today, m.today())
        assertEquals(6, c.daily.size)
    }
}
