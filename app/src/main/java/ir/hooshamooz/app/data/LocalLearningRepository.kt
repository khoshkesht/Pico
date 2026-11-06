package ir.hooshamooz.app.data

import android.content.Context
import ir.hooshamooz.engine.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class ExamRecord(val chapterId: String, val score: Int, val total: Int, val timestamp: Long)
data class LearningData(
    val completedLessons: Set<String> = emptySet(),
    val completedExams: Set<String> = emptySet(),
    val runs: Map<String, RunState> = emptyMap(),
    val lastRunId: String? = null,
    val dailyHistory: List<DailyEntry> = emptyList(),
    val examHistory: List<ExamRecord> = emptyList(),
    val textScale: Float = 1f,
    val contentVersion: String = "legacy-demo",
    val wrongSubmissions: Map<String, Int> = emptyMap(),
    val masteredActivities: Set<String> = emptySet(),
    val dailyCompletedOn: Map<LocalDate, LocalDate> = emptyMap(),
)

interface LearningRepository {
    fun read(): LearningData
    fun save(data: LearningData)
    fun reset()
}

/** Versioned private local storage. The public repository boundary can later use Room. */
class LocalLearningRepository(context: Context) : LearningRepository {
    private val prefs = context.getSharedPreferences("learning-v1", Context.MODE_PRIVATE)
    override fun read(): LearningData = runCatching {
        val raw = prefs.getString("state", null) ?: return LearningData()
        LearningCodec.decode(raw)
    }.getOrDefault(LearningData())
    override fun save(data: LearningData) {
        val editor = prefs.edit()
        val previous = prefs.getString("state", null)
        if (previous != null && !prefs.contains("pre-content-migration") &&
            runCatching { JSONObject(previous).optString("contentVersion", "legacy-demo") != data.contentVersion }.getOrDefault(false)) {
            editor.putString("pre-content-migration", previous)
        }
        editor.putString("state", LearningCodec.encode(data)).apply()
    }
    override fun reset() { prefs.edit().clear().apply() }
}

internal object LearningCodec {
    private fun JSONArray.strings() = (0 until length()).map { getString(it) }
    private fun <T> JSONArray.items(read: (JSONObject) -> T) = (0 until length()).map { read(getJSONObject(it)) }
    fun encode(data: LearningData): String = JSONObject().apply {
        put("version", 2)
        put("contentVersion", data.contentVersion)
        put("wrongSubmissions", JSONObject(data.wrongSubmissions))
        put("mastered", JSONArray(data.masteredActivities.toList()))
        put("lessons", JSONArray(data.completedLessons.toList()))
        put("exams", JSONArray(data.completedExams.toList()))
        put("lastRun", data.lastRunId)
        put("textScale", data.textScale)
        put("runs", JSONArray(data.runs.values.map { state -> JSONObject().apply {
            put("id", state.runId); put("mode", state.mode.name); put("ids", JSONArray(state.activityIds))
            put("contentVersion", state.contentVersion)
            put("index", state.index); put("selected", JSONArray(state.selectedIds.toList()))
            put("submitted", state.submitted); put("finished", state.finished)
            put("results", JSONArray(state.results.map { result -> JSONObject().apply {
                put("id", result.activityId)
                put("attempts", JSONArray(result.attempts.map { attempt -> JSONObject().apply {
                    put("selected", JSONArray(attempt.selectedIds.toList())); put("correct", attempt.correct)
                } }))
            } }))
        } }))
        put("daily", JSONArray(data.dailyHistory.map { JSONObject().apply {
            put("day", it.day.toString()); put("id", it.activityId); put("done", it.completed)
        } }))
        put("dailyCompletedOn", JSONObject().apply {
            data.dailyCompletedOn.forEach { (assigned, completed) -> put(assigned.toString(), completed.toString()) }
        })
        put("history", JSONArray(data.examHistory.map { JSONObject().apply {
            put("chapter", it.chapterId); put("score", it.score); put("total", it.total); put("time", it.timestamp)
        } }))
    }.toString()

    fun decode(raw: String): LearningData {
        val json = JSONObject(raw)
        require(json.getInt("version") in 1..2)
        return LearningData(
            completedLessons = json.getJSONArray("lessons").strings().toSet(),
            contentVersion = json.optString("contentVersion", "legacy-demo"),
            wrongSubmissions = json.optJSONObject("wrongSubmissions")?.let { counts -> counts.keys().asSequence().associateWith { counts.getInt(it).coerceIn(0, 1_000_000) } }.orEmpty(),
            masteredActivities = json.optJSONArray("mastered")?.strings()?.toSet().orEmpty(),
            completedExams = json.getJSONArray("exams").strings().toSet(),
            lastRunId = if (json.isNull("lastRun")) null else json.getString("lastRun"),
            textScale = json.optDouble("textScale", 1.0).toFloat().let { if (it.isFinite()) it.coerceIn(1f, 1.3f) else 1f },
            runs = json.getJSONArray("runs").items { state -> RunState(
                runId = state.getString("id"), mode = RunMode.valueOf(state.getString("mode")),
                contentVersion = state.optString("contentVersion", "1"),
                activityIds = state.getJSONArray("ids").strings(), index = state.getInt("index"),
                selectedIds = state.getJSONArray("selected").strings().toSet(),
                submitted = state.getBoolean("submitted"), finished = state.getBoolean("finished"),
                results = state.getJSONArray("results").items { result -> ActivityResult(
                    result.getString("id"), result.getJSONArray("attempts").items { attempt ->
                        Attempt(attempt.getJSONArray("selected").strings().toSet(), attempt.getBoolean("correct"))
                    }) }
            ) }.associateBy { it.runId },
            dailyHistory = json.getJSONArray("daily").items {
                DailyEntry(LocalDate.parse(it.getString("day")), it.getString("id"), it.getBoolean("done"))
            },
            dailyCompletedOn = json.optJSONObject("dailyCompletedOn")?.let { dates ->
                dates.keys().asSequence().associate { LocalDate.parse(it) to LocalDate.parse(dates.getString(it)) }
            }.orEmpty(),
            examHistory = json.getJSONArray("history").items {
                ExamRecord(it.getString("chapter"), it.getInt("score"), it.getInt("total"), it.getLong("time"))
            },
        )
    }
}
