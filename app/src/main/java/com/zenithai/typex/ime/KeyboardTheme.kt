package com.zenithai.typex.ime

import com.zenithai.typex.TypeXPrefs

/**
 * Per-app keypad themes. Same layout, different skin —
 * the "har social media ka alag keypad" feature.
 */
data class KeyboardTheme(
    val name: String,
    val background: Int,
    val keyBackground: Int,
    val keyText: Int,
    val specialKeyBackground: Int,
    val specialKeyText: Int,
    val accent: Int,
    val aiBarBackground: Int,
    val aiBarText: Int,
    val previewBackground: Int,
    val previewText: Int
)

object KeyboardThemes {

    val TYPEX = KeyboardTheme(
        name = "TYPE X",
        background = 0xFF0B0714.toInt(),
        keyBackground = 0xFF1E1433.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF2A1F4D.toInt(),
        specialKeyText = 0xFFD9CFFF.toInt(),
        accent = 0xFFFF7A18.toInt(),
        aiBarBackground = 0xFF160B2E.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFF7C3AED.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val WHATSAPP = KeyboardTheme(
        name = "WhatsApp",
        background = 0xFF0B141A.toInt(),
        keyBackground = 0xFF1F2C34.toInt(),
        keyText = 0xFFE9EDEF.toInt(),
        specialKeyBackground = 0xFF2A3942.toInt(),
        specialKeyText = 0xFFAEBAC1.toInt(),
        accent = 0xFF25D366.toInt(),
        aiBarBackground = 0xFF111B21.toInt(),
        aiBarText = 0xFFE9EDEF.toInt(),
        previewBackground = 0xFF25D366.toInt(),
        previewText = 0xFF0B141A.toInt()
    )

    val INSTAGRAM = KeyboardTheme(
        name = "Instagram",
        background = 0xFF000000.toInt(),
        keyBackground = 0xFF262626.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF363636.toInt(),
        specialKeyText = 0xFFE1306C.toInt(),
        accent = 0xFFE1306C.toInt(),
        aiBarBackground = 0xFF121212.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFF833AB4.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val FACEBOOK = KeyboardTheme(
        name = "Facebook",
        background = 0xFF18191A.toInt(),
        keyBackground = 0xFF3A3B3C.toInt(),
        keyText = 0xFFE4E6EB.toInt(),
        specialKeyBackground = 0xFF4E4F50.toInt(),
        specialKeyText = 0xFFB0B3B8.toInt(),
        accent = 0xFF1877F2.toInt(),
        aiBarBackground = 0xFF242526.toInt(),
        aiBarText = 0xFFE4E6EB.toInt(),
        previewBackground = 0xFF1877F2.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val MESSENGER = KeyboardTheme(
        name = "Messenger",
        background = 0xFF000000.toInt(),
        keyBackground = 0xFF1E1E1E.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF2E2E2E.toInt(),
        specialKeyText = 0xFF0084FF.toInt(),
        accent = 0xFF0084FF.toInt(),
        aiBarBackground = 0xFF111111.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFF0084FF.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val TIKTOK = KeyboardTheme(
        name = "TikTok",
        background = 0xFF000000.toInt(),
        keyBackground = 0xFF1A1A1A.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF2A2A2A.toInt(),
        specialKeyText = 0xFFFE2C55.toInt(),
        accent = 0xFFFE2C55.toInt(),
        aiBarBackground = 0xFF0D0D0D.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFFFE2C55.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val SNAPCHAT = KeyboardTheme(
        name = "Snapchat",
        background = 0xFF1A1A1A.toInt(),
        keyBackground = 0xFF2A2A2A.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF3A3A3A.toInt(),
        specialKeyText = 0xFFFFFC00.toInt(),
        accent = 0xFFFFFC00.toInt(),
        aiBarBackground = 0xFF222222.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFFFFFC00.toInt(),
        previewText = 0xFF000000.toInt()
    )

    val YOUTUBE = KeyboardTheme(
        name = "YouTube",
        background = 0xFF0F0F0F.toInt(),
        keyBackground = 0xFF272727.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF3D3D3D.toInt(),
        specialKeyText = 0xFFFF0000.toInt(),
        accent = 0xFFFF0000.toInt(),
        aiBarBackground = 0xFF1A1A1A.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFFFF0000.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    val DARK = KeyboardTheme(
        name = "Dark",
        background = 0xFF121212.toInt(),
        keyBackground = 0xFF2A2A2A.toInt(),
        keyText = 0xFFFFFFFF.toInt(),
        specialKeyBackground = 0xFF3A3A3A.toInt(),
        specialKeyText = 0xFFCCCCCC.toInt(),
        accent = 0xFF7C3AED.toInt(),
        aiBarBackground = 0xFF1E1E1E.toInt(),
        aiBarText = 0xFFFFFFFF.toInt(),
        previewBackground = 0xFF7C3AED.toInt(),
        previewText = 0xFFFFFFFF.toInt()
    )

    /** Resolve the theme for the app currently being typed in. */
    fun forPackage(pkg: String?, mode: String): KeyboardTheme {
        if (mode == TypeXPrefs.THEME_TYPEX) return TYPEX
        if (mode == TypeXPrefs.THEME_DARK) return DARK
        if (pkg == null) return TYPEX
        return when {
            pkg.startsWith("com.whatsapp") -> WHATSAPP
            pkg == "com.instagram.android" -> INSTAGRAM
            pkg == "com.facebook.katana" -> FACEBOOK
            pkg == "com.facebook.orca" -> MESSENGER
            pkg == "com.zhiliaoapp.musically" || pkg == "com.ss.android.ugc.trill" -> TIKTOK
            pkg == "com.snapchat.android" -> SNAPCHAT
            pkg == "com.google.android.youtube" -> YOUTUBE
            else -> TYPEX
        }
    }
}
