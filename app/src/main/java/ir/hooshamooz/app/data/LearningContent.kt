package ir.hooshamooz.app.data

import android.content.Context
import ir.hooshamooz.engine.*
import org.json.JSONArray
import org.json.JSONObject

data class ActivityIllustration(val path: String, val title: String, val description: String)

data class LearningContent(
    val version: String,
    val chapters: List<Chapter>,
    val activities: List<Activity>,
    val daily: List<Activity> = emptyList(),
    val compatibleVersions: Set<String> = emptySet(),
    val illustrations: Map<String, List<ActivityIllustration>> = emptyMap(),
    val chapterBackgrounds: Map<String, String> = emptyMap(),
    val dailyChallenges: List<DailyChallenge> = emptyList(),
    val dailyActivities: List<Activity> = emptyList(),
) {
    val byId = (activities + dailyActivities).associateBy { it.id }
    val dailyById = dailyChallenges.associateBy { it.id }
    fun challengeFor(runIds: List<String>) = dailyChallenges.firstOrNull { it.activityIds == runIds }
    init {
        require(byId.size == activities.size + dailyActivities.size)
        require(chapters.map { it.id }.distinct().size == chapters.size)
        chapters.forEach { chapter ->
            require((chapter.lessonIds + chapter.examIds).distinct().size == chapter.lessonIds.size + chapter.examIds.size)
            (chapter.lessonIds + chapter.examIds).forEach { require(byId[it]?.chapterId == chapter.id) }
        }
        require(daily.map { it.id }.distinct().size == daily.size && daily.all { it.id in byId })
        require(dailyById.size == dailyChallenges.size)
        require(dailyChallenges.all { challenge -> challenge.activityIds.size == 2 &&
            challenge.activityIds.all { byId[it]?.chapterId == challenge.chapterId && it in dailyActivities.map(Activity::id) } })
    }
    companion object {
        private val chapterOrder = listOf("capabilities" to "توانایی‌های AI", "prompt" to "Prompt", "context" to "Context", "iteration" to "Iteration", "hallucination" to "Hallucination", "verification" to "Verification", "privacy" to "Privacy", "responsibility" to "استفادهٔ مسئولانه")

        fun load(context: Context): LearningContent {
            val files = context.assets.list("content").orEmpty().filter { Regex("stage-\\d{2}\\.json").matches(it) }.sorted()
            return parseStages(files.map { name -> context.assets.open("content/$name").bufferedReader().use { it.readText() } },
                context.assets.open("content/daily-challenges.json").bufferedReader().use { it.readText() })
        }
        fun parse(raw: String) = parseStages(listOf(raw))

        /** Additive chapter files extend the catalog; the interaction engine is unchanged. */
        fun parseStages(rawStages: List<String>, rawDaily: String? = null): LearningContent {
            require(rawStages.isNotEmpty())
            val roots = rawStages.map(::JSONObject).sortedBy { it.getJSONObject("stage").getInt("number") }
            val catalog = mutableMapOf<String, Chapter>()
            val allActivities = mutableListOf<Activity>()
            val allDailyIds = mutableListOf<String>()
            val illustrations = mutableMapOf<String, List<ActivityIllustration>>()
            val backgrounds = mutableMapOf<String, String>()
            roots.forEachIndexed { index, root ->
                require(root.getString("schemaVersion") == "1.0.0" && root.getString("status") == "approved")
                val stage = root.getJSONObject("stage")
                require(index in chapterOrder.indices && stage.getInt("number") == index + 1 && stage.getString("id") == chapterOrder[index].first)
                val assets = root.getJSONArray("assets").objects().associateBy { it.getString("id") }
                val rawActivities = root.getJSONArray("activities").objects()
                rawActivities.forEach { item ->
                    val images = item.getJSONArray("contentBlocks").objects().mapNotNull { block ->
                        if (block.getString("type") != "IMAGE") null else {
                            val asset = assets.getValue(block.getString("assetId"))
                            if (asset.getString("renderer") != "LOCAL_IMAGE") null else {
                                val path = asset.getString("localPath")
                                require(Regex("images/[a-z0-9_]+\\.png").matches(path))
                                ActivityIllustration(path, block.getString("title"), asset.getString("altText"))
                            }
                        }
                    }
                    if (images.isNotEmpty()) illustrations[item.getString("id")] = images
                }
                val background = stage.optString("activityBackground", "METEORS")
                require(background in setOf("METEORS", "ORBITAL_LINKS", "SIGNAL_WAVES"))
                backgrounds[stage.getString("id")] = background
                val activities = rawActivities.map { item -> parseActivity(item, assets) }
                val lessons = stage.getJSONArray("lessonActivityIds").strings()
                val exams = stage.getJSONArray("examActivityIds").strings()
                require(lessons.isNotEmpty() && exams.isNotEmpty())
                val byId = rawActivities.associateBy { it.getString("id") }
                require(byId.size == rawActivities.size && (lessons + exams).toSet() == byId.keys)
                lessons.forEach { require(byId.getValue(it).getString("role") == "LESSON") }
                exams.forEach { require(byId.getValue(it).getString("role") == "EXAM") }
                catalog[stage.getString("id")] = Chapter(stage.getString("id"), stage.getString("title"),
                    stage.optString("subtitle", "با پیکو مرز توانایی ابزارها رو کشف کن"), lessons, exams)
                allActivities += activities
                allDailyIds += stage.getJSONArray("dailyActivityIds").strings().also { ids -> require(ids.all { it in byId }) }
            }
            // Exact prefix versions allow adding a chapter without clearing existing results.
            val prefixVersions = roots.indices.map { last ->
                if (last == 0) "stage1:${roots.first().getInt("contentVersion")}"
                else "course:" + roots.take(last + 1).joinToString("|") {
                    "${it.getJSONObject("stage").getString("id")}@${it.getInt("contentVersion")}"
                }
            }
            val chapters = chapterOrder.map { (id, title) -> catalog[id] ?: Chapter(id, title, "مأموریت بعدی", emptyList(), emptyList()) }
            val activityBank = allActivities.associateBy { it.id }
            if (rawDaily == null) return LearningContent(prefixVersions.last(), chapters, allActivities,
                allDailyIds.map(activityBank::getValue), prefixVersions.dropLast(1).toSet(), illustrations, backgrounds)
            val dailyRoot = JSONObject(rawDaily)
            require(dailyRoot.getString("schemaVersion") == "1.0.0")
            require(dailyRoot.getInt("reviewIntervalDays") == DailyChallengeSchedule.REVIEW_DAYS.toInt())
            val dailyActivities = mutableListOf<Activity>()
            val challenges = dailyRoot.getJSONArray("challenges").objects().filter { it.getString("chapterId") in catalog }.map { item ->
                val steps = item.getJSONArray("activities").objects().map { parseActivity(it, emptyMap()) }
                require(steps.size == 2 && steps.all { it.chapterId == item.getString("chapterId") && it.basePoints == 0 && it.wrongPenalty == 0 })
                dailyActivities += steps
                DailyChallenge(item.getString("id"), item.getString("chapterId"), item.getString("title"),
                    item.getInt("estimatedMinutes"), steps.map { it.id }, item.getString("closingDialogue"), item.getString("summary"))
            }
            require(challenges.groupBy { it.chapterId }.keys == catalog.keys)
            require(challenges.groupBy { it.chapterId }.values.all { it.size == 3 })
            val baseVersion = prefixVersions.last()
            val version = "$baseVersion|daily@${dailyRoot.getInt("contentVersion")}"
            // Catalog upgrades must preserve lesson progress from both old and daily-enabled prefixes.
            val compatible = prefixVersions.toSet() + prefixVersions.dropLast(1).map { "$it|daily@${dailyRoot.getInt("contentVersion")}" }
            return LearningContent(version, chapters, allActivities, challenges.map { c -> dailyActivities.first { it.id == c.activityIds.first() } },
                compatible, illustrations, backgrounds, challenges, dailyActivities)
        }

        private fun parseActivity(item: JSONObject, assets: Map<String, JSONObject>): Activity {
            val character = item.getJSONObject("character")
            val score = item.getJSONObject("scoring")
            val blocks = item.getJSONArray("contentBlocks").objects().mapNotNull { block ->
                when (block.getString("type")) {
                    "TEXT" -> ContentBlock.Text(block.getString("title"), block.getString("text"))
                    "IMAGE" -> {
                        val asset = assets.getValue(block.getString("assetId"))
                        when (asset.getString("renderer")) {
                            "LOCAL_IMAGE" -> null // App-only presentation; no new interaction or engine capability.
                            "CUPS_BOOTH" -> ContentBlock.Cups(asset.getString("altText"))
                            else -> error("Unsupported image renderer")
                        }
                    }
                    "AUDIO_MESSAGE_PREVIEW" -> {
                        require(!block.getBoolean("playbackEnabled"))
                        val asset = assets.getValue(block.getString("assetId"))
                        require(asset.getString("renderer") == "VOICE_MESSAGE_PREVIEW")
                        ContentBlock.VoiceMessage(block.getString("transcript"), asset.getString("altText"))
                    }
                    else -> error("Unsupported content block")
                }
            }
            return Activity(item.getString("id"), item.getString("chapterId"), InteractionType.valueOf(item.getString("interactionType")),
                item.optString("situation", "").takeUnless { it == "null" }.orEmpty(), character.getString("dialogue"), PicoMood.valueOf(character.getString("mood")),
                item.getJSONArray("choices").objects().map { Choice(it.getString("id"), it.getString("text"), it.getString("feedback").also { text -> require(text.isNotBlank()) }) },
                item.getJSONObject("answer").also { require(it.getString("strategy") == "EXACT_SET") }.getJSONArray("correctChoiceIds").strings().toSet(),
                item.getJSONObject("feedback").getString("general"), title = item.getString("title"), prompt = item.getString("prompt"), contentBlocks = blocks,
                basePoints = score.getInt("basePoints"), wrongPenalty = score.getInt("wrongSubmissionPenalty"))
        }
    }
}

internal fun JSONArray.strings() = (0 until length()).map { getString(it) }
internal fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
