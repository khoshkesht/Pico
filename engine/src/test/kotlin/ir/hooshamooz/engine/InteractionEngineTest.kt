package ir.hooshamooz.engine

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class InteractionEngineTest {
    private fun activity(id: String = "a", type: InteractionType = InteractionType.SINGLE_CHOICE) = Activity(
        id, "chapter", type, "Situation", "Dialogue", PicoMood.CONFIDENT,
        listOf(Choice("a", "A", "Feedback A"), Choice("b", "B", "Feedback B")),
        if (type == InteractionType.MULTIPLE_CHOICE) setOf("a", "b") else setOf("a"), "Explanation", "Hint",
    )
    private val engine = InteractionEngine(listOf(activity(), activity("b", InteractionType.MULTIPLE_CHOICE), activity("c", InteractionType.COMPARE_RESPONSES)))
    private fun answer(state: RunState, id: String) = engine.submit(engine.select(state, id))

    @Test fun `lesson retries keep first answer and can be resumed between attempts`() {
        var state = engine.start("lesson", RunMode.LESSON, listOf("a"))
        state = answer(state, "b")
        assertFalse(engine.feedback(state)!!.correct)
        state = engine.retry(state)
        assertTrue(engine.canResume(state))
        assertNull(engine.feedback(state))
        state = answer(state, "a")
        assertTrue(engine.feedback(state)!!.correct)
        assertEquals(0, engine.firstAttemptScore(state))
        assertEquals(2, state.results.single().attempts.size)
        assertTrue(engine.canResume(engine.next(state)))
    }

    @Test fun `exam locks first answer and delays feedback until entire run finishes`() {
        var state = answer(engine.start("exam", RunMode.EXAM, listOf("a", "c")), "b")
        assertNull(engine.feedback(state))
        assertThrows(IllegalStateException::class.java) { engine.retry(state) }
        val submitted = state
        assertThrows(IllegalStateException::class.java) { engine.select(submitted, "a") }
        state = engine.next(state)
        assertNull(engine.feedback(state, "a"))
        state = engine.next(answer(state, "a"))
        assertTrue(state.finished)
        assertFalse(engine.feedback(state, "a")!!.correct)
        assertTrue(engine.feedback(state, "c")!!.correct)
        assertEquals(1, engine.firstAttemptScore(state))
    }

    @Test fun `multi select uses exact set including no extra choices`() {
        val state = engine.start("multi", RunMode.LESSON, listOf("b"))
        assertFalse(engine.feedback(answer(state, "a"))!!.correct)
        var both = engine.select(engine.select(state, "b"), "a")
        assertTrue(engine.feedback(engine.submit(both))!!.correct)
        both = engine.select(both, "b")
        assertEquals(setOf("a"), both.selectedIds)
    }

    @Test fun `unknown options empty submissions and malformed snapshots are rejected`() {
        val state = engine.start("lesson", RunMode.LESSON, listOf("a"))
        assertThrows(IllegalArgumentException::class.java) { engine.select(state, "unknown") }
        assertThrows(IllegalStateException::class.java) { engine.submit(state) }
        assertFalse(engine.canResume(state.copy(index = 5)))
        assertFalse(engine.canResume(state.copy(finished = true)))
        assertFalse(engine.canResume(state.copy(activityIds = listOf("removed"))))
        assertFalse(engine.canResume(state.copy(selectedIds = setOf("a", "b"))))
        assertTrue(engine.canResume(engine.select(state, "a")))
    }

    @Test fun `daily reuses today's assignment and prioritizes never completed entries`() {
        val day = LocalDate.of(2026, 11, 6)
        val bank = listOf(activity("one"), activity("two"))
        assertNull(DailyChallengeSelector.choose(day, bank, emptySet(), emptyList()))
        val assigned = DailyChallengeSelector.choose(day, bank, setOf("chapter"), emptyList())!!
        val history = listOf(assigned.copy(completed = true))
        assertEquals(history.single(), DailyChallengeSelector.choose(day, bank.reversed(), setOf("chapter"), history))
        val tomorrow = DailyChallengeSelector.choose(day.plusDays(1), bank, setOf("chapter"), history)!!
        assertNotEquals(assigned.activityId, tomorrow.activityId)
        assertEquals(tomorrow, DailyChallengeSelector.choose(day.plusDays(1), bank.reversed(), setOf("chapter"), history))
    }

    @Test fun `chapters unlock sequentially and unfinished content remains locked`() {
        val chapters = listOf(Chapter("one", "One", "", listOf("a"), listOf("x")), Chapter("two", "Two", "", listOf("b"), listOf("y")), Chapter("three", "Three", "", emptyList(), emptyList()))
        val before = LearningProgress.chapters(chapters, setOf("a"), emptySet())
        assertEquals(ChapterStatus.IN_PROGRESS, before[0].status)
        assertEquals(.5f, before[0].fraction)
        assertEquals(ChapterStatus.LOCKED, before[1].status)
        val after = LearningProgress.chapters(chapters, setOf("a"), setOf("one"))
        assertEquals(ChapterStatus.COMPLETED, after[0].status)
        assertEquals(ChapterStatus.READY, after[1].status)
        assertEquals(ChapterStatus.LOCKED, after[2].status)
    }

    @Test fun `wrong training answer blocks next and content version blocks stale resume`() {
        val state = answer(engine.start("lesson", RunMode.LESSON, listOf("a")), "b")
        assertThrows(IllegalStateException::class.java) { engine.next(state) }
        assertFalse(engine.canResume(state.copy(contentVersion = "old")))
        assertFalse(engine.canResume(state.copy(finished = true)))
    }

    @Test fun `activity points floor at zero including extreme penalty counts`() {
        val a = activity()
        assertEquals(50, a.points(0))
        assertEquals(40, a.points(1))
        assertEquals(0, a.points(5))
        assertEquals(0, a.points(Int.MAX_VALUE))
    }
}
