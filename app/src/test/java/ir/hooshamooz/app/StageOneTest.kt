package ir.hooshamooz.app

import ir.hooshamooz.app.data.*
import ir.hooshamooz.engine.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StageOneTest {
    private val raw get() = File(requireNotNull(System.getProperty("stageContentFile"))).readText()
    private fun content() = LearningContent.parse(raw)
    private class MemoryRepository(var data: LearningData = LearningData()) : LearningRepository {
        override fun read() = LearningCodec.decode(LearningCodec.encode(data))
        override fun save(data: LearningData) { this.data = LearningCodec.decode(LearningCodec.encode(data)) }
        override fun reset() { data = LearningData() }
    }
    private fun answer(model: LearningViewModel, correct: Boolean = true) {
        val a = model.engine.current(model.activeRun()!!)
        (if (correct) a.correctIds else setOf(a.choices.first { it.id !in a.correctIds }.id)).forEach(model::select)
        model.submit()
    }
    private fun finishLessons(model: LearningViewModel) {
        model.continueLearning()
        repeat(6) { answer(model); model.next() }
    }

    @Test fun `approved source loads all seven activities and complete feedback without answer count hints`() {
        val c = content()
        assertEquals(7, c.activities.size)
        assertEquals(6, c.chapters.first().lessonIds.size)
        assertEquals(1, c.chapters.first().examIds.size)
        assertEquals(2, c.activities.count { it.contentBlocks.isNotEmpty() && it.contentBlocks.any { b -> b !is ContentBlock.Text } })
        assertTrue(c.activities.all { it.choices.all { choice -> choice.feedback.isNotBlank() } && it.explanation.isNotBlank() })
        assertTrue(c.activities.filter { it.type == InteractionType.MULTIPLE_CHOICE }.none {
            Regex("(^|\\s)(دو|۲|2)(\\s|$)").containsMatchIn(it.prompt)
        })
        assertFalse(raw.contains("خنگول"))
        assertEquals(setOf("a", "c"), c.activities.first().correctIds)
        assertEquals(setOf("a", "d"), c.activities.last().correctIds)
        assertEquals(setOf(PicoMood.CONFUSED, PicoMood.CONFIDENT, PicoMood.EMBARRASSED), c.activities.map { it.mood }.toSet())
        assertTrue(c.activities.all { it.choices.all { choice -> choice.feedback.length <= 110 } })
    }

    @Test fun `wrong submissions deduct once each and cannot skip even at zero points`() {
        val repo = MemoryRepository()
        var m = LearningViewModel(repo, content())
        m.continueLearning()
        val id = m.activeRun()!!.activityIds.first()
        repeat(7) {
            answer(m, false)
            assertEquals((50 - (it + 1) * 10).coerceAtLeast(0), m.points(id))
            m.submit() // Rapid duplicate taps must not charge another penalty.
            assertEquals(it + 1, m.state.value.data.wrongSubmissions[id])
            m.next()
            assertEquals(0, m.activeRun()!!.index)
            m.retry()
        }
        m = LearningViewModel(repo, content())
        m.continueLearning()
        assertEquals(0, m.points(id))
        assertFalse(id in m.state.value.data.completedLessons)
        answer(m)
        m.next()
        assertEquals(0, m.totalPoints())
        assertTrue(id in m.state.value.data.completedLessons)
        assertEquals(1, m.activeRun()!!.index)
    }

    @Test fun `exam reveals nothing before end and wrong result cannot complete chapter`() {
        val repo = MemoryRepository()
        var m = LearningViewModel(repo, content())
        finishLessons(m)
        m.startExam(content().chapters.first())
        answer(m, false)
        val id = m.activeRun()!!.activityIds.single()
        assertNull(m.engine.feedback(m.activeRun()!!))
        assertEquals(50, m.points(id))
        assertEquals(300, m.totalPoints())
        // Relaunch between submission and result must charge the error only once.
        m = LearningViewModel(repo, content()); m.continueLearning(); m.next()
        assertEquals(40, m.points(id))
        assertTrue(m.state.value.data.completedExams.isEmpty())
        assertEquals(ChapterStatus.IN_PROGRESS, m.chapterProgress().first().status)
        assertNotNull(m.engine.feedback(m.activeRun()!!))
        m.next() // Duplicate result navigation must not charge the exam twice.
        assertEquals(40, m.points(id))
        m.repeatRun(); answer(m); m.next()
        assertEquals(340, m.totalPoints())
        assertEquals(ChapterStatus.COMPLETED, m.chapterProgress().first().status)
        assertEquals(2, m.state.value.data.examHistory.size)
        // Reviewing cannot farm points or restore deductions.
        m.repeatRun(); answer(m); m.next()
        assertEquals(340, m.totalPoints())
    }

    @Test fun `all correct grants 350 points and repeat does not add a second award`() {
        val repo = MemoryRepository()
        val m = LearningViewModel(repo, content())
        finishLessons(m); m.startExam(content().chapters.first()); answer(m); m.next()
        assertEquals(350, m.totalPoints())
        assertEquals(7, repo.data.masteredActivities.size)
        m.repeatRun(); answer(m); m.next()
        assertEquals(350, m.totalPoints())
    }

    @Test fun `legacy demo completion cannot unlock final chapter or carry demo points`() {
        val repo = MemoryRepository(LearningData(completedLessons = setOf("demo-capabilities-1"), completedExams = setOf("capabilities"), textScale = 1.15f))
        val m = LearningViewModel(repo, content())
        assertTrue(m.state.value.data.completedLessons.isEmpty())
        assertTrue(m.state.value.data.completedExams.isEmpty())
        assertEquals(0, m.totalPoints())
        assertEquals(1.15f, m.state.value.data.textScale)
        assertEquals(content().version, repo.data.contentVersion)
    }

    @Test fun `partially selected multiple choice resumes without reset or leaked cardinality`() {
        val repo = MemoryRepository()
        var m = LearningViewModel(repo, content()); m.continueLearning(); m.select("a")
        m = LearningViewModel(repo, content()); m.continueLearning()
        assertEquals(setOf("a"), m.activeRun()!!.selectedIds)
        m.select("b"); m.select("c"); m.select("d") // No max=correct-count restriction.
        m.submit(); assertFalse(m.engine.feedback(m.activeRun()!!)!!.correct)
        assertEquals(40, m.points(m.activeRun()!!.activityIds.first()))
    }

    @Test fun `fifteen home lines randomize on fresh entry and stay stable during state updates`() {
        assertEquals(15, PicoGreetings.lines.size)
        assertEquals(15, PicoGreetings.lines.distinct().size)
        val random = kotlin.random.Random(7)
        var previous = 0
        repeat(100) {
            val next = PicoGreetings.next(previous, random)
            assertTrue(next in PicoGreetings.lines.indices)
            assertNotEquals(previous, next)
            previous = next
        }
        val m = LearningViewModel(MemoryRepository(), content())
        val original = m.state.value.greetingIndex
        val originalMood = m.state.value.homeMood
        val originalTitle = m.state.value.homeTitleIndex
        assertEquals(6, PicoGreetings.titles.distinct().size)
        m.navigate(Screen.HOME)
        assertEquals(original, m.state.value.greetingIndex)
        assertEquals(originalMood, m.state.value.homeMood)
        assertEquals(originalTitle, m.state.value.homeTitleIndex)
        m.navigate(Screen.PATH); m.navigate(Screen.HOME)
        assertNotEquals(original, m.state.value.greetingIndex)
        assertNotEquals(originalMood, m.state.value.homeMood)
        assertNotEquals(originalTitle, m.state.value.homeTitleIndex)
        assertTrue(m.state.value.homeTitleIndex in PicoGreetings.titles.indices)
        assertTrue(m.state.value.homeMood in setOf(PicoMood.CONFUSED, PicoMood.CONFIDENT, PicoMood.EMBARRASSED))
    }

    @Test fun `editorial update preserves existing points and partially answered session`() {
        val c = content()
        val repo = MemoryRepository()
        var m = LearningViewModel(repo, c)
        m.continueLearning(); answer(m, false); m.retry(); m.select("a")
        val oldPoints = m.points(c.activities.first().id)
        m = LearningViewModel(repo, LearningContent.parse(raw))
        m.continueLearning()
        assertEquals(oldPoints, m.points(c.activities.first().id))
        assertEquals(setOf("a"), m.activeRun()!!.selectedIds)
    }
}
