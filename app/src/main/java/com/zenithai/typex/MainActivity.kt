package com.zenithai.typex

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView

/**
 * Setup wizard: 2 steps — enable TYPE X in system settings,
 * then switch to it. Status is detected live on every resume.
 */
class MainActivity : Activity() {

    private lateinit var step1Status: TextView
    private lateinit var step2Status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        step1Status = findViewById(R.id.step1_status)
        step2Status = findViewById(R.id.step2_status)

        findViewById<Button>(R.id.btn_enable).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        findViewById<Button>(R.id.btn_switch).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        findViewById<Button>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val serviceId = "$packageName/.ime.TypeXKeyboardService"
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_INPUT_METHODS
        )?.contains(serviceId) == true
        val current = Settings.Secure.getString(
            contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD
        ) == serviceId

        markStep(step1Status, enabled)
        markStep(step2Status, current)
    }

    private fun markStep(view: TextView, done: Boolean) {
        if (done) {
            view.text = "✓"
            view.background = getDrawable(R.drawable.status_circle_done)
            view.setTextColor(getColor(android.R.color.white))
        } else {
            view.text = if (view.id == R.id.step1_status) "1" else "2"
            view.background = getDrawable(R.drawable.status_circle)
            view.setTextColor(getColor(R.color.typex_text))
        }
    }
}
