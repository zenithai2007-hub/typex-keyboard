package com.zenithai.typex

import android.os.Bundle
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity

/** TYPE X settings: keypad theme, AI tone, AI language, about. */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val rgTheme = findViewById<RadioGroup>(R.id.rg_theme)
        val rgTone = findViewById<RadioGroup>(R.id.rg_tone)
        val rgLang = findViewById<RadioGroup>(R.id.rg_lang)

        when (TypeXPrefs.themeMode(this)) {
            TypeXPrefs.THEME_TYPEX -> rgTheme.check(R.id.rb_theme_typex)
            TypeXPrefs.THEME_DARK -> rgTheme.check(R.id.rb_theme_dark)
            else -> rgTheme.check(R.id.rb_theme_auto)
        }
        when (TypeXPrefs.aiTone(this)) {
            TypeXPrefs.TONE_FRIENDLY -> rgTone.check(R.id.rb_tone_friendly)
            TypeXPrefs.TONE_SIMPLE -> rgTone.check(R.id.rb_tone_simple)
            else -> rgTone.check(R.id.rb_tone_pro)
        }
        when (TypeXPrefs.aiLanguage(this)) {
            TypeXPrefs.LANG_EN -> rgLang.check(R.id.rb_lang_en)
            TypeXPrefs.LANG_ROMAN -> rgLang.check(R.id.rb_lang_roman)
            TypeXPrefs.LANG_URDU -> rgLang.check(R.id.rb_lang_ur)
            else -> rgLang.check(R.id.rb_lang_follow)
        }

        rgTheme.setOnCheckedChangeListener { _, id ->
            TypeXPrefs.setThemeMode(
                this, when (id) {
                    R.id.rb_theme_typex -> TypeXPrefs.THEME_TYPEX
                    R.id.rb_theme_dark -> TypeXPrefs.THEME_DARK
                    else -> TypeXPrefs.THEME_AUTO
                }
            )
        }
        rgTone.setOnCheckedChangeListener { _, id ->
            TypeXPrefs.setAiTone(
                this, when (id) {
                    R.id.rb_tone_friendly -> TypeXPrefs.TONE_FRIENDLY
                    R.id.rb_tone_simple -> TypeXPrefs.TONE_SIMPLE
                    else -> TypeXPrefs.TONE_PROFESSIONAL
                }
            )
        }
        rgLang.setOnCheckedChangeListener { _, id ->
            TypeXPrefs.setAiLanguage(
                this, when (id) {
                    R.id.rb_lang_en -> TypeXPrefs.LANG_EN
                    R.id.rb_lang_roman -> TypeXPrefs.LANG_ROMAN
                    R.id.rb_lang_ur -> TypeXPrefs.LANG_URDU
                    else -> TypeXPrefs.LANG_FOLLOW
                }
            )
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
