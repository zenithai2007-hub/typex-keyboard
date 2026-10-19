package com.zenithai.typex.ai

/**
 * TYPE X on-device AI — 100% offline, rule-based generation.
 * Runs on a background thread; typing NEVER waits on it.
 *
 * v1 tasks: professional reply, friendly rewrite, shorten, expand,
 * translate (EN <-> Roman Urdu <-> Urdu script), email draft, caption,
 * persuasive message, smart polish.
 */
object TypeXAi {

    enum class Task {
        PROFESSIONAL_REPLY, FRIENDLY_REWRITE, SHORTEN, EXPAND,
        TRANSLATE, EMAIL, CAPTION, PERSUASIVE, POLISH
    }

    enum class OutLang { EN, ROMAN, URDU }

    // ------------------------------------------------------------------ entry

    fun generate(instruction: String, contextText: String, outLang: OutLang, tone: String): String {
        val task = detectTask(instruction)
        val lang = detectLang(instruction) ?: outLang
        val topic = extractTopic(instruction)
        val subject = contextText.trim().ifEmpty { topic }.trim()
        return when (task) {
            Task.PROFESSIONAL_REPLY -> professionalReply(subject.ifEmpty { topic }, lang)
            Task.FRIENDLY_REWRITE -> friendlyRewrite(subject.ifEmpty { topic }, lang)
            Task.SHORTEN -> shorten(subject.ifEmpty { topic })
            Task.EXPAND -> expand(subject.ifEmpty { topic }, lang)
            Task.TRANSLATE -> translate(subject.ifEmpty { topic }, instruction, lang)
            Task.EMAIL -> emailDraft(topic.ifEmpty { subject }, lang)
            Task.CAPTION -> caption(topic.ifEmpty { subject }, lang)
            Task.PERSUASIVE -> persuasive(topic.ifEmpty { subject }, lang)
            Task.POLISH -> polish(subject.ifEmpty { topic }, tone)
        }
    }

    // --------------------------------------------------------------- detection

    fun detectTask(instruction: String): Task {
        val t = " $instruction ".lowercase()
        fun has(vararg words: String) = words.any { t.contains(it) }
        if (has("email")) return Task.EMAIL
        if (has("caption", "hashtag")) return Task.CAPTION
        if (has("translate", "translation", "tarjuma", "turjuma")) return Task.TRANSLATE
        if (has("shorten", "shorter", "make it short", "chota", "chhota", "mukhtasir", "concise"))
            return Task.SHORTEN
        if (has("expand", "longer", "elaborate", "tafsil", "tafseel", "lamba", "detail"))
            return Task.EXPAND
        if (has("persuasive", "persuade", "convince", "qayel", "qayil", "convincing"))
            return Task.PERSUASIVE
        if (has("professional", "formal", "profesional", "proffesional")) return Task.PROFESSIONAL_REPLY
        if (has("friendly", "dostana", "warm", "casual")) return Task.FRIENDLY_REWRITE
        if (has("rewrite", "rephrase", "improve", "polish", "behtar", "dobara"))
            return Task.FRIENDLY_REWRITE
        if (has("reply", "jawab", "respond", "response", "answer")) return Task.PROFESSIONAL_REPLY
        return Task.POLISH
    }

    fun detectLang(instruction: String): OutLang? {
        val t = instruction.lowercase()
        if (t.contains("roman urdu") || t.contains("roman mein") || t.contains("in roman")) return OutLang.ROMAN
        if (t.contains("urdu")) return OutLang.URDU
        if (t.contains("english") || t.contains("angrezi") || t.contains("in english")) return OutLang.EN
        return null
    }

    private val TOPIC_STOPWORDS = setOf(
        "write", "draft", "compose", "make", "create", "generate", "give", "me", "a", "an", "the",
        "professional", "formal", "friendly", "dostana", "warm", "casual", "simple", "short", "shorter",
        "shorten", "longer", "expand", "elaborate", "persuasive", "convincing", "reply", "response",
        "respond", "answer", "message", "email", "mail", "caption", "translate", "translation",
        "to", "for", "about", "regarding", "on", "this", "that", "it", "in", "into", "of", "my",
        "likho", "likh", "banao", "banado", "bana", "do", "kar", "karo", "karna", "mein", "men", "ko",
        "ke", "ki", "ka", "liye", "se", "par", "par", "jawab", "tarjuma", "chota", "chhota", "lamba",
        "please", "plz", "kindly", "urdu", "english", "roman", "angrezi"
    )

    fun extractTopic(instruction: String): String {
        val words = instruction.lowercase()
            .replace(Regex("[^a-z ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it !in TOPIC_STOPWORDS }
        return words.joinToString(" ").trim()
    }

    // ---------------------------------------------------------------- builders

    private fun topicPartEn(topic: String) = if (topic.isBlank()) "" else " regarding $topic"
    private fun topicPartRoman(topic: String) = if (topic.isBlank()) "" else " $topic ke hawale se"
    private fun topicPartUrdu(topic: String) = if (topic.isBlank()) "" else " $topic کے حوالے سے"

    private fun professionalReply(subject: String, lang: OutLang): String {
        val pick = (subject.hashCode() and Int.MAX_VALUE) % 3
        return when (lang) {
            OutLang.ROMAN -> when (pick) {
                0 -> "Salam!${topicPartRoman(subject)} aap ke message ka shukriya. Mein aap ki madad ke liye hazir hoon — kya aap thori mazeed tafseel bata sakte hain taake mein behtar rehnumai kar sakoon?"
                1 -> "Hello!${topicPartRoman(subject)} rabta karne ka shukriya. Mein ne aap ki baat note kar li hai aur jald tafseeli jawab doon ga."
                else -> "Salam!${topicPartRoman(subject)} aap ka message mil gaya hai. Mein is par ghor kar raha hoon aur aap se jald rabta karoon ga. Shukriya!"
            }
            OutLang.URDU -> when (pick) {
                0 -> "السلام علیکم!${topicPartUrdu(subject)} آپ کے پیغام کا شکریہ۔ میں آپ کی مدد کے لیے حاضر ہوں۔"
                1 -> "سلام!${topicPartUrdu(subject)} رابطہ کرنے کا شکریہ۔ میں نے آپ کی بات نوٹ کر لی ہے۔"
                else -> "السلام علیکم!${topicPartUrdu(subject)} آپ کا پیغام مل گیا ہے، جلد جواب دوں گا۔ شکریہ!"
            }
            else -> when (pick) {
                0 -> "Hi, thank you for reaching out${topicPartEn(subject)}. I'd be happy to help — could you please share a few more details so I can assist you better?"
                1 -> "Hello, thanks for your message${topicPartEn(subject)}. I appreciate you getting in touch. Let me look into this and get back to you shortly."
                else -> "Hi there, thanks for contacting me${topicPartEn(subject)}. I've noted your message and will respond with details very soon."
            }
        }
    }

    private fun friendlyRewrite(text: String, lang: OutLang): String {
        if (text.isBlank()) return polish(text, "friendly")
        val clean = normalizeSentences(text)
        return when (lang) {
            OutLang.ROMAN -> "Hey! $clean".replaceFirstChar { it.uppercase() }
            OutLang.URDU -> clean
            else -> clean
        }
    }

    private fun shorten(text: String): String {
        if (text.isBlank()) return text
        val firstSentence = text.split(Regex("[.!?؟]+")).firstOrNull()?.trim().orEmpty()
        val base = firstSentence.ifEmpty { text }
        val words = base.split(Regex("\\s+"))
        return if (words.size > 16) words.take(15).joinToString(" ") + "…" else base
    }

    private val EXPAND_EN = listOf(
        "This could really make a difference if done the right way.",
        "Getting this right matters more than getting it fast.",
        "A little extra effort here will pay off in the long run."
    )
    private val EXPAND_ROMAN = listOf(
        "Agar ye sahi tareeqe se ho jaye to bohat farq par sakta hai.",
        "Is kaam mein thori tawajju se behtareen natija mil sakta hai."
    )
    private val EXPAND_URDU = listOf(
        "اگر یہ صحیح طریقے سے ہو جائے تو بہت فرق پڑ سکتا ہے۔",
        "اس کام میں تھوڑی توجہ سے بہترین نتیجہ مل سکتا ہے۔"
    )

    private fun expand(text: String, lang: OutLang): String {
        if (text.isBlank()) return text
        val base = text.trim().trimEnd('.', '!', '؟', '?') + "."
        val bank = when (lang) {
            OutLang.ROMAN -> EXPAND_ROMAN
            OutLang.URDU -> EXPAND_URDU
            else -> EXPAND_EN
        }
        return "$base ${bank[(text.hashCode() and Int.MAX_VALUE) % bank.size]}"
    }

    private fun emailDraft(topic: String, lang: OutLang): String {
        val title = topic.ifBlank { "your request" }
            .replaceFirstChar { it.uppercase() }
        return when (lang) {
            OutLang.ROMAN -> "Subject: $title\n\nSalam,\n\nUmeed hai aap khairiyat se honge. Mein $title ke hawale se rabta kar raha hoon. Baraye meherbani is par ghor farmayein aur apni qeemti raye se aagah karein.\n\nShukriya,\n"
            OutLang.URDU -> "موضوع: $title\n\nالسلام علیکم،\n\nامید ہے آپ خیریت سے ہوں گے۔ میں $title کے حوالے سے رابطہ کر رہا ہوں۔ براہ کرم اس پر غور فرمائیں۔\n\nشکریہ\n"
            else -> "Subject: $title\n\nHi,\n\nI hope you're doing well. I'm writing regarding $title. " +
                "I'd appreciate your thoughts on this when you get a chance.\n\nBest regards\n"
        }
    }

    private fun caption(topic: String, lang: OutLang): String {
        val t = topic.ifBlank { "today" }.replaceFirstChar { it.uppercase() }
        return when (lang) {
            OutLang.ROMAN -> "$t — full focus mode.\nChoti koshish, bara natija.\n#TypeX #Mehnat #Kamyabi"
            OutLang.URDU -> "$t — مکمل توجہ کے ساتھ۔\nچھوٹی کوشش، بڑا نتیجہ۔\n#TypeX"
            else -> "$t — locked in.\nLess typing, more getting things done.\n#TypeX #Productivity #AI"
        }
    }

    private fun persuasive(topic: String, lang: OutLang): String {
        val t = topic.ifBlank { "this" }
        return when (lang) {
            OutLang.ROMAN -> "Agar aap $t mein waqai behtari chahte hain to abhi qadam uthayein. " +
                "Sochnay mein waqt zaya na karein — sahi faisla aaj hi karein aur farq khud dekhein."
            OutLang.URDU -> "اگر آپ $t میں واقعی بہتری چاہتے ہیں تو ابھی قدم اٹھائیں۔ سوچنے میں وقت ضائع نہ کریں۔"
            else -> "If you genuinely want better results with $t, now is the time to act. " +
                "Don't overthink it — make the decision today and see the difference for yourself."
        }
    }

    private fun polish(text: String, tone: String): String {
        if (text.isBlank()) return text
        var clean = normalizeSentences(text)
        if (tone == "friendly" && !clean.startsWith("Hey", ignoreCase = true) &&
            !clean.startsWith("Hi", ignoreCase = true)
        ) {
            clean = "Hey! $clean"
        }
        return clean
    }

    private fun normalizeSentences(text: String): String {
        var clean = text.trim().replace(Regex("\\s+"), " ")
        // Capitalize first letter of each sentence.
        clean = clean.split(Regex("(?<=[.!?؟])\\s+")).joinToString(" ") { s ->
            s.replaceFirstChar { it.uppercase() }
        }.replaceFirstChar { it.uppercase() }
        if (clean.isNotEmpty() && clean.last() !in ".!?؟") clean += "."
        return clean
    }

    // --------------------------------------------------------------- translate

    private fun hasUrduScript(s: String) =
        s.any { it.code in 0x0600..0x06FF }

    /** EN -> (roman, urdu script). */
    private val DICT: Map<String, Pair<String, String>> = mapOf(
        "hello" to ("salam" to "سلام"),
        "hi" to ("salam" to "سلام"),
        "thanks" to ("shukriya" to "شکریہ"),
        "thank" to ("shukriya" to "شکریہ"),
        "please" to ("meherbani" to "مہربانی"),
        "yes" to ("haan" to "ہاں"),
        "no" to ("nahi" to "نہیں"),
        "good" to ("acha" to "اچھا"),
        "bad" to ("bura" to "برا"),
        "morning" to ("subah" to "صبح"),
        "night" to ("raat" to "رات"),
        "day" to ("din" to "دن"),
        "today" to ("aaj" to "آج"),
        "tomorrow" to ("kal" to "کل"),
        "friend" to ("dost" to "دوست"),
        "friends" to ("dost" to "دوست"),
        "love" to ("mohabbat" to "محبت"),
        "work" to ("kaam" to "کام"),
        "time" to ("waqt" to "وقت"),
        "new" to ("naya" to "نیا"),
        "message" to ("paigham" to "پیغام"),
        "reply" to ("jawab" to "جواب"),
        "help" to ("madad" to "مدد"),
        "home" to ("ghar" to "گھر"),
        "car" to ("gaari" to "گاڑی"),
        "money" to ("paise" to "پیسے"),
        "business" to ("karobar" to "کاروبار"),
        "happy" to ("khush" to "خوش"),
        "sad" to ("udaas" to "اداس"),
        "big" to ("bara" to "بڑا"),
        "small" to ("chota" to "چھوٹا"),
        "water" to ("pani" to "پانی"),
        "food" to ("khana" to "کھانا"),
        "man" to ("aadmi" to "آدمی"),
        "woman" to ("aurat" to "عورت"),
        "boy" to ("larka" to "لڑکا"),
        "girl" to ("larki" to "لڑکی"),
        "book" to ("kitaab" to "کتاب"),
        "school" to ("school" to "اسکول"),
        "office" to ("daftar" to "دفتر"),
        "phone" to ("phone" to "فون"),
        "number" to ("number" to "نمبر"),
        "name" to ("naam" to "نام"),
        "life" to ("zindagi" to "زندگی"),
        "world" to ("duniya" to "دنیا"),
        "people" to ("log" to "لوگ"),
        "person" to ("shakhs" to "شخص"),
        "thing" to ("cheez" to "چیز"),
        "house" to ("ghar" to "گھر"),
        "door" to ("darwaza" to "دروازہ"),
        "road" to ("sarak" to "سڑک"),
        "market" to ("bazaar" to "بازار"),
        "shop" to ("dukaan" to "دکان"),
        "price" to ("qeemat" to "قیمت"),
        "cheap" to ("sasta" to "سستا"),
        "expensive" to ("mehenga" to "مہنگا"),
        "fast" to ("tez" to "تیز"),
        "slow" to ("aahista" to "آہستہ"),
        "beautiful" to ("khoobsurat" to "خوبصورت"),
        "best" to ("behtareen" to "بہترین")
    )

    private val ROMAN_TO_EN: Map<String, String> by lazy {
        DICT.entries.associate { (en, pair) -> pair.first to en }
    }
    private val URDU_TO_EN: Map<String, String> by lazy {
        DICT.entries.associate { (en, pair) -> pair.second to en }
    }

    private fun translate(text: String, instruction: String, fallbackLang: OutLang): String {
        if (text.isBlank()) return text
        val t = instruction.lowercase()
        return when {
            hasUrduScript(text) -> translateWords(text, URDU_TO_EN)
            t.contains("roman") -> translateWords(text, DICT.mapValues { it.value.first })
            t.contains("urdu") || fallbackLang == OutLang.URDU ->
                translateWords(text, DICT.mapValues { it.value.second })
            else -> translateWords(text, DICT.mapValues { it.value.first })
        }
    }

    private fun translateWords(text: String, dict: Map<String, String>): String {
        val out = StringBuilder()
        val word = StringBuilder()
        fun flush() {
            if (word.isEmpty()) return
            val w = word.toString()
            val lower = w.lowercase()
            var rep = dict[lower] ?: w
            if (w.isNotEmpty() && w[0].isUpperCase()) {
                rep = rep.replaceFirstChar { it.uppercase() }
            }
            out.append(rep)
            word.clear()
        }
        for (ch in text) {
            if (ch.isLetter()) word.append(ch) else { flush(); out.append(ch) }
        }
        flush()
        return out.toString()
    }
}
