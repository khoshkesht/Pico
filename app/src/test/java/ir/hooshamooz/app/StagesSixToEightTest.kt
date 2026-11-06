package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.DataInputStream
import java.nio.ByteBuffer

class StagesSixToEightTest {
    private val properties = listOf("stageContentFile", "stageTwoContentFile", "stageThreeContentFile", "stageFourContentFile", "stageFiveContentFile", "stageSixContentFile", "stageSevenContentFile", "stageEightContentFile")
    private fun course(count: Int = 8) = LearningContent.parseStages(properties.take(count).reversed().map { File(requireNotNull(System.getProperty(it))).readText() })
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

    @Test fun `three separate chapters use existing interactions short feedback and shared new backdrop`() {
        val c = course()
        assertEquals(54, c.activities.size)
        assertEquals(listOf("verification", "privacy", "responsibility"), c.chapters.drop(5).map { it.id })
        assertEquals(listOf(6, 5, 6), c.chapters.drop(5).map { it.lessonIds.size })
        c.chapters.drop(5).forEach { chapter ->
            val activities = c.activities.filter { it.chapterId == chapter.id }
            assertEquals(1, chapter.examIds.size)
            assertEquals(InteractionType.entries.toSet(), activities.map { it.type }.toSet())
            assertTrue(activities.all { it.choices.all { choice -> choice.feedback.length in 1..64 } })
            assertTrue(activities.all { it.contentBlocks.none { block -> block is ContentBlock.VoiceMessage } })
            assertEquals("SIGNAL_WAVES", c.chapterBackgrounds[chapter.id])
            assertTrue(activities.all { it.basePoints == 50 && it.wrongPenalty == 10 })
        }
    }

    @Test fun `each chapter uses its own distinct relevant packaged image`() {
        val c = course()
        val images = c.chapters.drop(5).map { chapter -> chapter.lessonIds.flatMap { c.illustrations[it].orEmpty() }.single() }
        assertEquals(listOf("images/verification_library.png", "images/privacy_form.png", "images/responsibility_team.png"), images.map { it.path })
        assertEquals(3, images.map { it.path }.toSet().size)
        val source = File(requireNotNull(System.getProperty("stageSixContentFile")))
        val root = requireNotNull(source.parentFile?.parentFile?.parentFile)
        val files = images.map { File(root, "app/src/main/assets/${it.path}") }
        files.forEach { file ->
            val header = ByteArray(24)
            DataInputStream(file.inputStream()).use { it.readFully(header) }
            assertArrayEquals(byteArrayOf(137.toByte(), 80, 78, 71, 13, 10, 26, 10), header.take(8).toByteArray())
            assertTrue(ByteBuffer.wrap(header).getInt(16) >= 800 && ByteBuffer.wrap(header).getInt(20) >= 600)
        }
        assertEquals(3, files.map { java.security.MessageDigest.getInstance("SHA-256").digest(it.readBytes()).toList() }.toSet().size)
    }

    @Test fun `each new chapter waits for correct lessons and preceding exam`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        c.chapters.take(5).forEach { finish(m, it) }
        for (index in 5..7) {
            assertEquals(ChapterStatus.READY, m.chapterProgress()[index].status)
            if (index < 7) {
                val previous = m.activeRun()
                m.startChapter(c.chapters[index + 1]); assertEquals(previous, m.activeRun())
                assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[index + 1].status)
            }
            lessons(m, c.chapters[index]); m.startExam(c.chapters[index]); answer(m, false)
            assertNull(m.engine.feedback(m.activeRun()!!)); m.next()
            assertEquals(ChapterStatus.IN_PROGRESS, m.chapterProgress()[index].status)
            if (index < 7) assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[index + 1].status)
            m.repeatRun(); answer(m); m.next()
            assertEquals(ChapterStatus.COMPLETED, m.chapterProgress()[index].status)
        }
        assertEquals(2670, m.totalPoints())
    }

    @Test fun `upgrade from five chapters retains progress score and today's challenge`() {
        val repo = Repository(); val old = course(5); val before = LearningViewModel(repo, old)
        old.chapters.take(5).forEach { finish(before, it) }
        val daily = before.today()
        val after = LearningViewModel(repo, course())
        assertEquals(1700, after.totalPoints())
        assertEquals(5, after.chapterProgress().count { it.status == ChapterStatus.COMPLETED })
        assertEquals(ChapterStatus.READY, after.chapterProgress()[5].status)
        assertTrue(after.chapterProgress().drop(6).all { it.status == ChapterStatus.LOCKED })
        assertEquals(daily, after.today())
    }

    @Test fun `direct upgrade from two chapters retains unfinished selections and penalties`() {
        val repo = Repository(); val old = course(2); var m = LearningViewModel(repo, old)
        finish(m, old.chapters[0]); m.startChapter(old.chapters[1])
        answer(m, false); m.retry(); m.select("b")
        m = LearningViewModel(repo, course()); m.continueLearning()
        assertEquals(350, m.totalPoints())
        assertEquals(40, m.points("stage02-activity01"))
        assertEquals(setOf("b"), m.activeRun()!!.selectedIds)
        assertEquals(ChapterStatus.LOCKED, m.chapterProgress()[5].status)
    }

    @Test fun `privacy retries persist across restart and cannot award more than remaining points`() {
        val c = course(); val repo = Repository(); var m = LearningViewModel(repo, c)
        c.chapters.take(6).forEach { finish(m, it) }
        val chapter = c.chapters[6]; m.startChapter(chapter)
        repeat(6) { answer(m, false); m.retry() }
        m = LearningViewModel(repo, c); m.continueLearning()
        assertEquals(0, m.points(chapter.lessonIds.first()))
        answer(m); m.next()
        repeat(chapter.lessonIds.size - 1) { answer(m); m.next() }
        m.startExam(chapter); answer(m); m.next()
        assertEquals(250, m.chapterPoints(chapter))
        assertEquals(ChapterStatus.READY, m.chapterProgress()[7].status)
    }

    @Test fun `all eight chapters finish at 2700 points with no repeat farming or changed daily assignment`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        finish(m, c.chapters[0]); val daily = m.today()
        c.chapters.drop(1).forEach { finish(m, it) }
        assertEquals(2700, m.totalPoints())
        assertEquals(listOf(350, 300, 350), c.chapters.drop(5).map { m.chapterPoints(it) })
        assertEquals(54, m.state.value.data.masteredActivities.size)
        assertTrue(m.chapterProgress().all { it.status == ChapterStatus.COMPLETED && it.fraction == 1f })
        assertEquals(24, c.daily.size); assertEquals(daily, m.today())
        m.repeatRun(); answer(m); m.next(); assertEquals(2700, m.totalPoints())
        assertNull(m.nextChapter())
    }

    @Test fun `journey backdrop advances after passing exams and stays advanced during replay`() {
        val c = course(); val m = LearningViewModel(Repository(), c)
        assertEquals("METEORS", m.journeyBackground())
        finish(m, c.chapters[0])
        lessons(m, c.chapters[1]); m.startExam(c.chapters[1]); answer(m, false); m.next()
        assertEquals("METEORS", m.journeyBackground())
        m.repeatRun(); answer(m); m.next()
        assertEquals("ORBITAL_LINKS", m.journeyBackground())
        m.startChapter(c.chapters[0], replay = true)
        m.navigate(Screen.HOME)
        assertEquals("ORBITAL_LINKS", m.journeyBackground())
        c.chapters.drop(2).take(3).forEach { finish(m, it) }
        assertEquals("SIGNAL_WAVES", m.journeyBackground())
        c.chapters.drop(5).forEach { finish(m, it) }
        assertEquals("SIGNAL_WAVES", m.journeyBackground())
        m.reset()
        assertEquals("METEORS", m.journeyBackground())
    }
}
