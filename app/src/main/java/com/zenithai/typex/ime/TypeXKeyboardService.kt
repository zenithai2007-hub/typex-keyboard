package com.zenithai.typex.ime

import android.content.res.ColorStateList
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.zenithai.typex.R
import com.zenithai.typex.TypeXPrefs
import com.zenithai.typex.ai.TypeXAi

/**
 * TYPE X keyboard service.
 *
 * Architecture (the anti-hang design for 1M users):
 * - Typing path is 100% on-device and synchronous: touch -> InputConnection,
 *   zero network, zero waiting.
 * - AI runs on a throwaway background thread and NEVER blocks typing.
 */
class TypeXKeyboardService : InputMethodService() {

    private lateinit var keyboardView: KeyboardView
    private lateinit var aiBar: LinearLayout
    private lateinit var aiInput: TextView
    private lateinit var aiRunBtn: Button
    private lateinit var aiProgress: ProgressBar

    private var layoutMode = LayoutMode.LETTERS
    private var lang = Lang.EN
    private var shiftState = 0 // 0=off, 1=shift(one-shot), 2=caps lock
    private var isPassword = false
    private var lastSpaceTime = 0L

    // AI compose mode: keys edit the instruction buffer instead of the app.
    private var aiMode = false
    private val aiBuffer = StringBuilder()
    private var aiRunning = false

    private val mainHandler = Handler(Looper.getMainLooper())

    // ------------------------------------------------------------ view setup

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.keyboard_input, null) as LinearLayout
        keyboardView = root.findViewById(R.id.keyboard_view)
        aiBar = root.findViewById(R.id.ai_bar)
        aiInput = root.findViewById(R.id.ai_input)
        aiRunBtn = root.findViewById(R.id.ai_run)
        aiProgress = root.findViewById(R.id.ai_progress)

        keyboardView.keyListener = { code, label -> handleKey(code, label) }
        aiInput.setOnClickListener { toggleAiMode() }
        aiRunBtn.setOnClickListener {
            if (aiMode) runAi() else { toggleAiMode() }
        }
        refreshLayout()
        return root
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        val theme = KeyboardThemes.forPackage(info.packageName, TypeXPrefs.themeMode(this))
        keyboardView.theme = theme
        applyAiBarTheme(theme)

        isPassword = isPasswordField(info)
        // Password fields: AI bar hidden, plain fast typing only.
        aiBar.visibility = if (isPassword) View.GONE else View.VISIBLE
        if (isPassword && aiMode) exitAiMode()

        updateEnterKey(info)
        shiftState = if (shouldAutoCaps()) 1 else 0
        refreshLayout()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (aiMode) exitAiMode()
    }

    /** We render our own AI bar, so the system extract view is never needed. */
    override fun onEvaluateFullscreenMode(): Boolean = false

    private fun applyAiBarTheme(theme: KeyboardTheme) {
        aiBar.setBackgroundColor(theme.aiBarBackground)
        aiInput.setTextColor(theme.aiBarText)
        aiInput.setHintTextColor(theme.specialKeyText)
        aiRunBtn.backgroundTintList = ColorStateList.valueOf(theme.accent)
        aiRunBtn.setTextColor(theme.previewText)
        aiProgress.indeterminateTintList = ColorStateList.valueOf(theme.accent)
    }

    // ----------------------------------------------------------------- input

    private fun refreshLayout() {
        keyboardView.shiftState = shiftState
        keyboardView.rows = KeyboardLayouts.rows(layoutMode, lang, shiftState)
    }

    private fun handleKey(code: Int, label: String) {
        if (aiMode && code != KeyCodes.LANG && code != KeyCodes.MODE_CHANGE &&
            code != KeyCodes.SYMBOLS_PAGE
        ) {
            handleAiModeKey(code, label)
            return
        }
        val ic = currentInputConnection ?: return
        when (code) {
            KeyCodes.DELETE -> {
                ic.deleteSurroundingText(1, 0)
                lastSpaceTime = 0
            }
            KeyCodes.SHIFT -> {
                shiftState = if (shiftState == 0) 1 else 0
                refreshLayout()
            }
            KeyCodes.CAPS_LOCK -> {
                shiftState = 2
                refreshLayout()
            }
            KeyCodes.MODE_CHANGE -> {
                layoutMode =
                    if (layoutMode == LayoutMode.LETTERS) LayoutMode.SYMBOLS else LayoutMode.LETTERS
                refreshLayout()
            }
            KeyCodes.SYMBOLS_PAGE -> {
                layoutMode =
                    if (layoutMode == LayoutMode.SYMBOLS) LayoutMode.SYMBOLS2 else LayoutMode.SYMBOLS
                refreshLayout()
            }
            KeyCodes.LANG -> {
                lang = when (lang) {
                    Lang.EN -> Lang.ROMAN
                    Lang.ROMAN -> Lang.URDU
                    Lang.URDU -> Lang.EN
                }
                refreshLayout()
            }
            KeyCodes.ENTER -> handleEnter(ic)
            KeyCodes.SPACE -> handleSpace(ic)
            0 -> { /* spacer key, ignore */ }
            else -> {
                ic.commitText(label, 1)
                lastSpaceTime = 0
                if (shiftState == 1) {
                    shiftState = 0
                    refreshLayout()
                }
            }
        }
    }

    private fun handleAiModeKey(code: Int, label: String) {
        when (code) {
            KeyCodes.DELETE -> {
                if (aiBuffer.isNotEmpty()) {
                    aiBuffer.deleteCharAt(aiBuffer.length - 1)
                    updateAiInput()
                }
            }
            KeyCodes.SHIFT -> {
                shiftState = if (shiftState == 0) 1 else 0
                refreshLayout()
            }
            KeyCodes.CAPS_LOCK -> {
                shiftState = 2
                refreshLayout()
            }
            KeyCodes.ENTER -> runAi()
            KeyCodes.SPACE -> {
                aiBuffer.append(' ')
                updateAiInput()
                if (shiftState == 1) {
                    shiftState = 0
                    refreshLayout()
                }
            }
            0 -> { /* spacer, ignore */ }
            else -> {
                aiBuffer.append(label)
                updateAiInput()
                if (shiftState == 1) {
                    shiftState = 0
                    refreshLayout()
                }
            }
        }
    }

    private fun handleSpace(ic: InputConnection) {
        val now = SystemClock.uptimeMillis()
        if (now - lastSpaceTime < 400) {
            // double-space -> period + space
            ic.deleteSurroundingText(1, 0)
            ic.commitText(". ", 1)
            lastSpaceTime = 0
            if (shiftState == 0) {
                shiftState = 1
                refreshLayout()
            }
        } else {
            ic.commitText(" ", 1)
            lastSpaceTime = now
        }
    }

    private fun handleEnter(ic: InputConnection) {
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
            ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
    }

    // ---------------------------------------------------------------- AI mode

    private fun toggleAiMode() {
        if (isPassword) return
        aiMode = !aiMode
        if (aiMode) {
            aiInput.hint = "Instruction likhein… phir RUN"
            aiInput.setBackgroundResource(R.drawable.ai_input_bg_active)
            aiRunBtn.text = "RUN"
        } else {
            exitAiMode()
        }
    }

    private fun exitAiMode() {
        aiMode = false
        aiBuffer.clear()
        aiInput.text = ""
        aiInput.hint = "Tell Type X what to do…"
        aiInput.setBackgroundResource(R.drawable.ai_input_bg)
    }

    private fun updateAiInput() {
        aiInput.text = aiBuffer.toString()
    }

    private fun resolveOutLang(): TypeXAi.OutLang {
        return when (TypeXPrefs.aiLanguage(this)) {
            TypeXPrefs.LANG_EN -> TypeXAi.OutLang.EN
            TypeXPrefs.LANG_ROMAN -> TypeXAi.OutLang.ROMAN
            TypeXPrefs.LANG_URDU -> TypeXAi.OutLang.URDU
            else -> when (lang) {
                Lang.EN -> TypeXAi.OutLang.EN
                Lang.ROMAN -> TypeXAi.OutLang.ROMAN
                Lang.URDU -> TypeXAi.OutLang.URDU
            }
        }
    }

    private fun runAi() {
        if (aiRunning) return
        val ic = currentInputConnection ?: return
        val instruction = aiBuffer.toString().trim()
        val selected = try {
            ic.getSelectedText(0)?.toString() ?: ""
        } catch (e: Exception) { "" }
        val before = try {
            ic.getTextBeforeCursor(400, 0)?.toString() ?: ""
        } catch (e: Exception) { "" }
        val contextText = selected.ifEmpty { before }
        if (instruction.isEmpty() && contextText.isBlank()) {
            aiInput.hint = "Pehle instruction likhein…"
            return
        }
        aiRunning = true
        aiProgress.visibility = View.VISIBLE
        aiRunBtn.isEnabled = false
        val outLang = resolveOutLang()
        val tone = TypeXPrefs.aiTone(this)

        // Background thread — typing stays instant while AI thinks.
        Thread {
            val result = try {
                TypeXAi.generate(instruction, contextText, outLang, tone)
            } catch (e: Exception) {
                ""
            }
            mainHandler.post {
                aiRunning = false
                aiProgress.visibility = View.GONE
                aiRunBtn.isEnabled = true
                if (result.isNotEmpty()) {
                    ic.beginBatchEdit()
                    ic.commitText(result, 1) // replaces selection automatically
                    ic.endBatchEdit()
                }
                exitAiMode()
            }
        }.start()
    }

    // ---------------------------------------------------------------- helpers

    private fun isPasswordField(info: EditorInfo): Boolean {
        val variation = info.inputType and InputType.TYPE_MASK_VARIATION
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD
    }

    private fun shouldAutoCaps(): Boolean {
        if (isPassword) return false
        return try {
            val before = currentInputConnection?.getTextBeforeCursor(4, 0)?.toString() ?: ""
            before.isEmpty() || before.endsWith(". ") || before.endsWith("\n")
        } catch (e: Exception) {
            false
        }
    }

    private fun updateEnterKey(info: EditorInfo) {
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        keyboardView.enterLabel = when (action) {
            EditorInfo.IME_ACTION_GO -> "Go"
            EditorInfo.IME_ACTION_SEARCH -> "Search"
            EditorInfo.IME_ACTION_SEND -> "Send"
            EditorInfo.IME_ACTION_NEXT -> "Next"
            EditorInfo.IME_ACTION_DONE -> "Done"
            EditorInfo.IME_ACTION_PREVIOUS -> "Prev"
            else -> null
        }
    }
}
