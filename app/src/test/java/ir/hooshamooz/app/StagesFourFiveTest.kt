package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.DataInputStream
import java.nio.ByteBuffer

class StagesFourFiveTest {
    private val properties = listOf("stageContentFile", "stageTwoContentFile", "stageThreeContentFile", "stageFourContentFile", "stageFiveContentFile")
    private fun course(count: Int = 5) = LearningContent.parseStages(properties.take(count).reversed().map { File(requireNotNull(System.getProperty(it))).readText() })
    private class Repository(var data: LearningData = LearningData()) : LearningRepository {
        override fun read() = LearningCodec.decode(LearningCodec.encode(data))
        override fun save(data: LearningData) { this.data = LearningCodec.decode(LearningCodec.encode(data)) }
        override fun reset() { data = LearningData() }
    }
    private fun answer(m: LearningViewModel, correct: Boolean = true) {
        val a = m.engine.current(m.activeRun()!!)
        (if (correct) a.correctIds else setOf(a.choices.first { it.id !in a.correctIds }.id)).forEach(m::select)
        m.submit()
    }
    private fun lessons(m: LearningViewModel, chapter: Chapter) {
        m.startChapter(chapter); repeat(chapter.lessonIds.size) { answer(m); m.next() }
    }
    private fun finish(m: LearningViewModel, chapter: Chapter) {
        lessons(m, chapter); m.startExam(chapter); answer(m); m.next()
    }

    @Test fun `each stage has six lessons one exam short feedback and existing types only`() {
        val c = course()
        assertEquals(34, c.activities.size)
        for (chapter in c.chapters.slice(3..4)) {
            val activities = c.activities.filter { it.chapterId == chapter.id }
            assertEquals(6, chapter.lessonIds.size); assertEquals(1, chapter.examIds.size)
            assertEquals(InteractionType.entries.toSet(), activities.map { it.type }.toSet())
            assertTrue(activities.all { it.choices.all { choice -> choice.feedback.length in 1..64 } })
            assertTrue(activities.all { it.contentBlocks.none { block -> block is ContentBlock.VoiceMessage } })
            assertEquals("ORBITAL_LINKS", c.chapterBackgrounds[chapter.id])
            assertFalse(activities.any { Regex("(^|\\s)(دو|۲|2)(\\s|$)").containsMatchIn(it.prompt) })
        }
    }

    @Test fun `both new stages share exactly one new packaged image and keep original context image`() {
        val c = course()
        val fourth = c.chapters[3].lessonIds.flatMap { c.illustrations[it].orEmpty() }.single()
        val fifth = c.chapters[4].lessonIds.flatMap { c.illustrations[it].orEmpty() }.single()
        assertEquals(fourth.path, fifth.path)
        assertEquals("images/courtyard_bike.png", fourth.path)
        assertEquals(2, c.illustrations.values.flatten().map { it.path }.toSet().size)
        val source = File(requireNotNull(System.getProperty("stageFourContentFile")))
        val root = requireNotNull(source.parentFile?.parentFile?.parentFile)
        val header = ByteArray(24)
        DataInputStream(File(root, "app/src/main/assets/${fourth.path}").inputStream()).use { it.readFully(header) }
        assertArrayEquals(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10), header.take(8).toByteArray())
        assertTrue(ByteBuffer.wrap(header).getInt(16) >= 800 && ByteBuffer.wrap(header).getInt(20) >= 600)
    }

    @Test fun `fourth and fifth unlock sequentially only after successful preceding exam`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        m.startChapter(c.chapters[3]); assertNull(m.activeRun())
        c.chapters.take(3).forEach { finish(m, it) }
        assertEquals(ChapterStatus.READY, m.chapterProgress()[3].status)
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[4].status)
        lessons(m, c.chapters[3]); m.startExam(c.chapters[3]); answer(m, false)
        assertNull(m.engine.feedback(m.activeRun()!!)); m.next()
        assertEquals(ChapterStatus.IN_PROGRESS, m.chapterProgress()[3].status)
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[4].status)
        m.repeatRun(); answer(m); m.next()
        assertEquals("hallucination", m.nextChapter()!!.id)
        assertEquals(ChapterStatus.READY, m.chapterProgress()[4].status)
    }

    @Test fun `upgrade from three complete stages preserves 1000 points and daily assignment`() {
        val old = course(3); val repo = Repository(); val before = LearningViewModel(repo, old)
        old.chapters.take(3).forEach { finish(before, it) }
        val daily = before.today()
        val after = LearningViewModel(repo, course())
        assertEquals(1000, after.totalPoints())
        assertEquals(3, after.chapterProgress().count { it.status == ChapterStatus.COMPLETED })
        assertEquals(ChapterStatus.READY, after.chapterProgress()[3].status)
        assertEquals(daily, after.today())
    }

    @Test fun `direct upgrade from installed two stage version keeps incomplete answer and deductions`() {
        val old = course(2); val repo = Repository(); var m = LearningViewModel(repo, old)
        finish(m, old.chapters[0]); m.startChapter(old.chapters[1])
        answer(m, false); m.retry(); m.select("b")
        m = LearningViewModel(repo, course()); m.continueLearning()
        assertEquals(350, m.totalPoints())
        assertEquals(40, m.points("stage02-activity01"))
        assertEquals(setOf("b"), m.activeRun()!!.selectedIds)
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[3].status)
    }

    @Test fun `all five stages award 1700 points without duplicate awards and preserve daily choice`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        finish(m, c.chapters[0]); val daily = m.today()
        c.chapters.slice(1..4).forEach { finish(m, it) }
        assertEquals(1700, m.totalPoints())
        assertEquals(350, m.chapterPoints(c.chapters[3])); assertEquals(350, m.chapterPoints(c.chapters[4]))
        assertEquals(34, m.state.value.data.masteredActivities.size)
        assertEquals(15, c.daily.size); assertEquals(daily, m.today())
        m.repeatRun(); answer(m); m.next(); assertEquals(1700, m.totalPoints())
        assertNull(m.nextChapter())
    }
}
