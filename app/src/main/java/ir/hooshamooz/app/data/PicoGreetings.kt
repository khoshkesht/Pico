package ir.hooshamooz.app.data

import kotlin.random.Random
import ir.hooshamooz.engine.PicoMood

object PicoGreetings {
    val titles = listOf(
        "پیکو اینجاست!",
        "پیکو آماده‌ست!",
        "پیکو وارد می‌شود!",
        "با پیکو بزن بریم!",
        "فرمانده پیکو رسید!",
        "پیکو پشت فرمونه!",
    )
    fun nextTitle(previous: Int? = null, random: Random = Random.Default): Int {
        if (previous == null || previous !in titles.indices) return random.nextInt(titles.size)
        val next = random.nextInt(titles.size - 1)
        return if (next >= previous) next + 1 else next
    }
    private val moods = listOf(PicoMood.CONFUSED, PicoMood.CONFIDENT, PicoMood.EMBARRASSED)
    fun nextMood(previous: PicoMood? = null, random: Random = Random.Default): PicoMood =
        moods.filterNot { it == previous }.random(random)
    val lines = listOf(
        "من همه‌چی رو بلدم؛ فقط هنوز نمی‌دونم همه‌چی چیه!",
        "امروز قراره بترکونیم! امیدوارم منظورم سفینه نباشه…",
        "نقشه دست منه. چرا زمین داره دور خودش می‌چرخه؟",
        "آماده‌ام! فقط دکمهٔ شروع خودم رو پیدا نمی‌کنم.",
        "جواب رو می‌دونم! سؤال چی بود؟",
        "اعتمادبه‌نفسم صددرصده؛ دقتم هنوز داره لود می‌شه.",
        "یه کشف بزرگ کردم: بعضی وقتا اشتباه می‌کنم!",
        "کمربندتو ببند؛ من راه رو بلدم… تقریباً.",
        "امروز هیچ سوتی‌ای نمی‌دم. اینم اولین پیش‌بینی اشتباهم!",
        "بزن بریم! این بار اول فکر می‌کنم، بعد با اطمینان اشتباه می‌کنم.",
        "مغزم روشنه؛ چراغ چکشم همین‌طور!",
        "من گم نمی‌شم؛ فقط مسیرهای خیلی جدید کشف می‌کنم.",
        "هوش مصنوعی‌ام، ولی دنبال عینکم می‌گردم. عینک ندارم!",
        "یه سؤال بده؛ یه جواب مطمئنِ مشکوک تحویل بگیر!",
        "خبر خوب: آماده‌ام. خبر عجیب: هنوز نمی‌دونم برای چی!",
    )
    fun next(previous: Int? = null, random: Random = Random.Default): Int {
        if (previous == null || previous !in lines.indices) return random.nextInt(lines.size)
        val next = random.nextInt(lines.size - 1)
        return if (next >= previous) next + 1 else next
    }
}
