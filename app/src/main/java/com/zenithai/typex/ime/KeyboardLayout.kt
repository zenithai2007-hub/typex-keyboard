package com.zenithai.typex.ime

import com.zenithai.typex.R

/** Custom key codes (negative = non-printable). */
object KeyCodes {
    const val SHIFT = -1
    const val MODE_CHANGE = -2
    const val DELETE = -5
    const val ENTER = 10
    const val SPACE = 32
    const val LANG = -100
    const val SYMBOLS_PAGE = -101
    const val CAPS_LOCK = -102
}

data class KeyDef(
    val label: String = "",
    val code: Int = 0,
    val width: Float = 1f,
    val special: Boolean = false,
    val iconRes: Int = 0
)

enum class LayoutMode { LETTERS, SYMBOLS, SYMBOLS2 }
enum class Lang { EN, ROMAN, URDU }

/** Key layouts. ROMAN uses the latin QWERTY (AI outputs Roman Urdu). */
object KeyboardLayouts {

    fun rows(mode: LayoutMode, lang: Lang, shiftState: Int): List<List<KeyDef>> {
        return when (mode) {
            LayoutMode.LETTERS -> letterRows(lang, shiftState)
            LayoutMode.SYMBOLS -> symbolRows(page2 = false)
            LayoutMode.SYMBOLS2 -> symbolRows(page2 = true)
        }
    }

    fun langKeyLabel(lang: Lang): String = when (lang) {
        Lang.EN -> "EN"
        Lang.ROMAN -> "UR"
        Lang.URDU -> "اردو"
    }

    private fun letterRows(lang: Lang, shiftState: Int): List<List<KeyDef>> {
        val upper = shiftState != 0
        val row1Src: List<String>
        val row2Src: List<String>
        val row3Src: List<String>
        if (lang == Lang.URDU) {
            // Phonetic Urdu mapped onto QWERTY positions.
            row1Src = listOf("ق", "و", "ع", "ر", "ت", "ے", "ء", "ی", "ہ", "ح")
            row2Src = listOf("ا", "س", "ڈ", "ف", "گ", "ح", "ج", "ک", "ل")
            row3Src = listOf("ز", "ش", "چ", "ط", "ب", "ن", "م")
        } else {
            val base1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
            val base2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
            val base3 = listOf("z", "x", "c", "v", "b", "n", "m")
            row1Src = if (upper) base1.map { it.uppercase() } else base1
            row2Src = if (upper) base2.map { it.uppercase() } else base2
            row3Src = if (upper) base3.map { it.uppercase() } else base3
        }
        val row1 = row1Src.map { KeyDef(it, it[0].code) }
        val row2 = row2Src.map { KeyDef(it, it[0].code) }
        val row3 = mutableListOf<KeyDef>()
        row3.add(KeyDef("", KeyCodes.SHIFT, 1.5f, special = true, iconRes = R.drawable.ic_shift))
        row3.addAll(row3Src.map { KeyDef(it, it[0].code) })
        row3.add(KeyDef("", KeyCodes.DELETE, 1.5f, special = true, iconRes = R.drawable.ic_backspace))
        val row4 = listOf(
            KeyDef("?123", KeyCodes.MODE_CHANGE, 1.5f, special = true),
            KeyDef(langKeyLabel(lang), KeyCodes.LANG, 1f, special = true),
            KeyDef("", KeyCodes.SPACE, 5f),
            KeyDef("", KeyCodes.ENTER, 2f, special = true, iconRes = R.drawable.ic_enter)
        )
        return listOf(row1, row2, row3, row4)
    }

    private fun symbolRows(page2: Boolean): List<List<KeyDef>> {
        val row1Src: List<String>
        val row2Src: List<String>
        val row3Src: List<String>
        val pageKey: KeyDef
        if (page2) {
            row1Src = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
            row2Src = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
            row3Src = listOf("$", "&", "§", "¶", "¡", "¿")
            pageKey = KeyDef("123", KeyCodes.SYMBOLS_PAGE, 1.5f, special = true)
        } else {
            row1Src = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            row2Src = listOf("-", "/", ":", ";", "(", ")", "@", "\"", "'")
            row3Src = listOf("_", "&", "%", "+", "=")
            pageKey = KeyDef("#+=", KeyCodes.SYMBOLS_PAGE, 1.5f, special = true)
        }
        val row1 = row1Src.map { KeyDef(it, it[0].code) }
        val row2 = mutableListOf<KeyDef>()
        if (!page2) row2.add(KeyDef("", 0, 0.5f)) // center the 9-key row
        row2.addAll(row2Src.map { KeyDef(it, it[0].code) })
        if (!page2) row2.add(KeyDef("", 0, 0.5f))
        val row3 = mutableListOf<KeyDef>()
        row3.add(pageKey)
        row3.addAll(row3Src.map { KeyDef(it, it[0].code) })
        while (row3.size < 8) row3.add(KeyDef("", 0, 1f))
        row3.add(KeyDef("", KeyCodes.DELETE, 1.5f, special = true, iconRes = R.drawable.ic_backspace))
        val row4 = listOf(
            KeyDef("ABC", KeyCodes.MODE_CHANGE, 1.5f, special = true),
            KeyDef("", KeyCodes.LANG, 1f, special = true, iconRes = R.drawable.ic_globe),
            KeyDef("", KeyCodes.SPACE, 5f),
            KeyDef("", KeyCodes.ENTER, 2f, special = true, iconRes = R.drawable.ic_enter)
        )
        return listOf(row1, row2, row3, row4)
    }
}
