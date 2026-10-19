package com.zenithai.typex

import android.content.Context

/** Simple SharedPreferences wrapper for TYPE X settings. */
object TypeXPrefs {
    private const val FILE = "typex_prefs"

    const val THEME_AUTO = "auto"
    const val THEME_TYPEX = "typex"
    const val THEME_DARK = "dark"

    const val TONE_PROFESSIONAL = "professional"
    const val TONE_FRIENDLY = "friendly"
    const val TONE_SIMPLE = "simple"

    const val LANG_FOLLOW = "follow"
    const val LANG_EN = "en"
    const val LANG_ROMAN = "roman"
    const val LANG_URDU = "urdu"

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun themeMode(ctx: Context): String =
        prefs(ctx).getString("theme_mode", THEME_AUTO) ?: THEME_AUTO

    fun setThemeMode(ctx: Context, v: String) {
        prefs(ctx).edit().putString("theme_mode", v).apply()
    }

    fun aiTone(ctx: Context): String =
        prefs(ctx).getString("ai_tone", TONE_PROFESSIONAL) ?: TONE_PROFESSIONAL

    fun setAiTone(ctx: Context, v: String) {
        prefs(ctx).edit().putString("ai_tone", v).apply()
    }

    fun aiLanguage(ctx: Context): String =
        prefs(ctx).getString("ai_lang", LANG_FOLLOW) ?: LANG_FOLLOW

    fun setAiLanguage(ctx: Context, v: String) {
        prefs(ctx).edit().putString("ai_lang", v).apply()
    }
}
