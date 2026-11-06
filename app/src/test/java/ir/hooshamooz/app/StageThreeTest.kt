package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.DataInputStream
import java.nio.ByteBuffer

class StageThreeTest {
    private fun raw(property: String) = File(requireNotNull(System.getProperty(property))).readText()
    private fun course() = LearningContent.parseStages(listOf(raw("stageThreeContentFile"), raw("stageContentFile"), raw("stageTwoContentFile")))
    private fun oldCourse() = LearningContent.parseStages(listOf(raw("stageContentFile"), raw("stageTwoContentFile")))
    private class Repository(var data: LearningData = LearningData()) : LearningRepository {
        override fun read() = LearningCodec.decode(LearningCodec.encode(data))
        override fun save(data: LearningData) { this.data = LearningCodec.decode(LearningCodec.encode(data)) }
        override fun reset() { data = LearningData() }
    }
    private fun answer(model: LearningViewModel, correct: Boolean = true) {
        val activity = model.engine.current(model.activeRun()!!)
        (if (correct) activity.correctIds else setOf(activity.choices.first { it.id !in activity.correctIds }.id)).forEach(model::select)
        model.submit()
    }
    private fun finish(model: LearningViewModel, chapter: Chapter) {
        model.startChapter(chapter)
        repeat(chapter.lessonIds.size) { answer(model); model.next() }
        model.startExam(chapter); answer(model); model.next()
    }

    @Test fun `five lessons one exam one packaged illustration and only existing interaction types`() {
        val c = course(); val stage = c.chapters[2]; val activities = c.activities.filter { it.chapterId == stage.id }
        assertEquals(20, c.activities.size)
        assertEquals(5, stage.lessonIds.size)
        assertEquals(1, stage.examIds.size)
        assertEquals(InteractionType.entries.toSet(), activities.map { it.type }.toSet())
        assertTrue(activities.all { it.choices.all { choice -> choice.feedback.length in 1..110 } })
        assertTrue(activities.all { a -> a.contentBlocks.none { it is ContentBlock.VoiceMessage } })
        assertEquals("ORBITAL_LINKS", c.chapterBackgrounds[stage.id])
        assertEquals(setOf("stage03-activity03"), c.illustrations.keys)
        val picture = c.illustrations.getValue("stage03-activity03").single()
        val projectRoot = File(requireNotNull(System.getProperty("stageThreeContentFile"))).parentFile.parentFile.parentFile
        val header = ByteArray(24)
        DataInputStream(File(projectRoot, "app/src/main/assets/${picture.path}").inputStream()).use { it.readFully(header) }
        assertArrayEquals(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10), header.take(8).toByteArray())
        assertTrue(ByteBuffer.wrap(header).getInt(16) >= 800 && ByteBuffer.wrap(header).getInt(20) >= 600)
        assertTrue(picture.description.isNotBlank())
        assertFalse(activities.any { Regex("(^|\\s)(دو|۲|2)(\\s|$)").containsMatchIn(it.prompt) })
    }

    @Test fun `context stays locked until second chapter exam succeeds`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        finish(m, c.chapters[0])
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[2].status)
        val oldRun = m.activeRun()
        m.startChapter(c.chapters[2]); assertEquals(oldRun, m.activeRun())
        m.startChapter(c.chapters[1]); repeat(6) { answer(m); m.next() }
        m.startExam(c.chapters[1]); answer(m, false); m.next()
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[2].status)
        m.repeatRun(); answer(m); m.next()
        assertEquals(ChapterStatus.READY, m.chapterProgress()[2].status)
        assertEquals("context", m.nextChapter()!!.id)
    }

    @Test fun `adding third chapter preserves second chapter deductions and unfinished response`() {
        val repo = Repository(); val old = oldCourse(); var m = LearningViewModel(repo, old)
        finish(m, old.chapters[0]); m.startChapter(old.chapters[1])
        answer(m, false); m.retry(); m.select("b")
        val score = m.totalPoints()
        m = LearningViewModel(repo, course()); m.continueLearning()
        assertEquals(score, m.totalPoints())
        assertEquals(40, m.points("stage02-activity01"))
        assertEquals(setOf("b"), m.activeRun()!!.selectedIds)
        assertEquals(ChapterStatus.COMPLETED, m.chapterProgress()[0].status)
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[2].status)
    }

    @Test fun `upgrade after completing two stages keeps 700 points and opens context`() {
        val repo = Repository(); val old = oldCourse(); val before = LearningViewModel(repo, old)
        finish(before, old.chapters[0]); finish(before, old.chapters[1])
        val after = LearningViewModel(repo, course())
        assertEquals(700, after.totalPoints())
        assertEquals(setOf("capabilities", "prompt"), after.state.value.data.completedExams)
        assertEquals(ChapterStatus.READY, after.chapterProgress()[2].status)
    }

    @Test fun `context retry survives relaunch and full score is 300 not 350`() {
        val repo = Repository(); val c = course(); var m = LearningViewModel(repo, c)
        finish(m, c.chapters[0]); finish(m, c.chapters[1]); m.startChapter(c.chapters[2])
        answer(m, false); m.next(); assertEquals(0, m.activeRun()!!.index)
        m.retry(); m = LearningViewModel(repo, c); m.continueLearning()
        assertEquals(40, m.points("stage03-activity01"))
        repeat(5) { answer(m); m.next() }
        m.startExam(c.chapters[2]); answer(m, false)
        assertNull(m.engine.feedback(m.activeRun()!!)); m.next()
        assertEquals(ChapterStatus.IN_PROGRESS, m.chapterProgress()[2].status)
        m.repeatRun(); answer(m); m.next()
        assertEquals(280, m.chapterPoints(c.chapters[2]))
        assertEquals(980, m.totalPoints())
        assertEquals(ChapterStatus.COMPLETED, m.chapterProgress()[2].status)
    }

    @Test fun `all three stages award exactly 1000 points and daily assignment remains stable`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        finish(m, c.chapters[0]); val today = m.today()
        finish(m, c.chapters[1]); finish(m, c.chapters[2])
        assertEquals(1000, m.totalPoints())
        assertEquals(300, m.chapterPoints(c.chapters[2]))
        assertEquals(3, m.chapterProgress().count { it.status == ChapterStatus.COMPLETED })
        assertEquals(today, m.today())
        assertEquals(9, c.daily.size)
        assertNull(m.nextChapter())
    }
}
