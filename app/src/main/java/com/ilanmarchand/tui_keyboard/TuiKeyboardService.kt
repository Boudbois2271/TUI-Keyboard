// SPDX-License-Identifier: GPL-3.0-or-later

package com.ilanmarchand.tui_keyboard

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

// Colors from the launcher's theme, so the keyboard matches the terminal.
private const val BACKGROUND = 0xFF000000.toInt()
private const val FOREGROUND = 0xFFAEB6F4.toInt()
private const val ACCENT = 0xFFFF9A00.toInt()
private const val KEY_FILL = 0xFF101015.toInt()

private const val ROW_HEIGHT_DP = 46
private const val LONG_PRESS_MS = 350L
private const val REPEAT_START_MS = 400L
private const val REPEAT_EVERY_MS = 50L

class TuiKeyboardService : InputMethodService() {

    private var layer = Layer.BASE
    private var shift = false
    private var ctrlHeld = false

    // Fn is a tap to lock the layer, or a hold that lasts only while it is pressed.
    private var fnLocked = false
    private var fnDownAt = 0L

    private val handler = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var popup: PopupWindow? = null
    private var accentRow: LinearLayout? = null

    // One TextView per key position. The layers share the same shape, so a layer
    // change only rebinds these views instead of rebuilding them.
    private val keyViews = mutableListOf<List<TextView>>()

    override fun onCreateInputView(): View {
        val view = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BACKGROUND)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ROW_HEIGHT_DP * 5))
        }
        root = view
        buildKeys()
        return view
    }

    override fun onFinishInput() {
        super.onFinishInput()
        dismissPopup()
        ctrlHeld = false
    }

    // Creates the views once, with the widths of the base layer.
    private fun buildKeys() {
        val container = root ?: return
        container.removeAllViews()
        keyViews.clear()
        for (rowKeys in Layout.base) {
            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(ROW_HEIGHT_DP))
            }
            val row = rowKeys.map { key ->
                TextView(this).apply {
                    gravity = Gravity.CENTER
                    typeface = Typeface.MONOSPACE
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, key.weight)
                        .apply { setMargins(dp(1), dp(1), dp(1), dp(1)) }
                }.also { rowView.addView(it) }
            }
            container.addView(rowView)
            keyViews.add(row)
        }
        refreshKeys()
    }

    // Rebinds every view to the current layer: label, colors and touch action.
    private fun refreshKeys() {
        val rows = Layout.rows(layer)
        for ((r, rowKeys) in rows.withIndex()) {
            for ((c, key) in rowKeys.withIndex()) {
                bind(keyViews[r][c], key)
            }
        }
    }

    private fun bind(view: TextView, key: Key) {
        val active = isActive(key)
        val label = labelFor(key)
        view.text = label
        // Multi-letter labels such as Ctrl or Home need a smaller size to fit.
        view.textSize = if (label.length > 1) 12f else 15f
        view.setTextColor(if (active) BACKGROUND else FOREGROUND)
        view.setBackgroundColor(if (active) ACCENT else KEY_FILL)
        if (key.action == Action.Spacer) {
            view.visibility = View.INVISIBLE
            view.setOnTouchListener(null)
        } else {
            view.visibility = View.VISIBLE
            view.setOnTouchListener { v, event -> onKeyTouch(key, v, event) }
        }
    }

    private fun labelFor(key: Key): String = when (val action = key.action) {
        is Action.Text -> if (shift) action.shifted else action.base
        is Action.Code -> when (action.keyCode) {
            KeyEvent.KEYCODE_ESCAPE -> "Esc"
            KeyEvent.KEYCODE_TAB -> "Tab"
            KeyEvent.KEYCODE_MOVE_HOME -> "Home"
            KeyEvent.KEYCODE_MOVE_END -> "End"
            KeyEvent.KEYCODE_PAGE_UP -> "PgUp"
            KeyEvent.KEYCODE_PAGE_DOWN -> "PgDn"
            KeyEvent.KEYCODE_FORWARD_DEL -> "Del"
            KeyEvent.KEYCODE_DPAD_LEFT -> "←"
            KeyEvent.KEYCODE_DPAD_DOWN -> "↓"
            KeyEvent.KEYCODE_DPAD_UP -> "↑"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "→"
            KeyEvent.KEYCODE_ENTER -> "⏎"
            else -> ""
        }
        Action.Shift -> "⇧"
        Action.Ctrl -> "Ctrl"
        Action.Space -> "space"
        Action.Backspace -> "⌫"
        is Action.ToggleLayer -> when (action.layer) {
            Layer.FN -> "Fn"
            Layer.SYM -> "Sym"
            Layer.BASE -> "ABC"
        }
        Action.Spacer -> ""
    }

    private fun isActive(key: Key): Boolean = when (key.action) {
        Action.Shift -> shift
        Action.Ctrl -> ctrlHeld
        is Action.ToggleLayer -> layer == key.action.layer
        else -> false
    }

    private fun onKeyTouch(key: Key, view: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                view.alpha = 0.6f
                onKeyDown(key, view)
            }
            MotionEvent.ACTION_UP -> {
                view.alpha = 1f
                onKeyUp(key, view, event)
            }
            MotionEvent.ACTION_CANCEL -> {
                view.alpha = 1f
                onKeyCancel(key)
            }
        }
        return true
    }

    private var longPressed = false
    private val longPressRunnable = Runnable { onLongPress() }
    private val repeatRunnable = object : Runnable {
        override fun run() {
            sendKey(KeyEvent.KEYCODE_DEL, ctrl = false)
            handler.postDelayed(this, REPEAT_EVERY_MS)
        }
    }
    private var pendingKey: Key? = null
    private var pendingView: View? = null

    private fun onKeyDown(key: Key, view: View) {
        longPressed = false
        pendingKey = key
        pendingView = view
        when (val action = key.action) {
            is Action.Text -> {
                if (key.accents.isNotEmpty()) {
                    // Commit on release, so a long press can show the accents instead.
                    handler.postDelayed(longPressRunnable, LONG_PRESS_MS)
                } else {
                    typeText(action)
                }
            }
            Action.Backspace -> {
                sendKey(KeyEvent.KEYCODE_DEL, ctrl = false)
                handler.postDelayed(repeatRunnable, REPEAT_START_MS)
            }
            Action.Space -> commit(" ")
            is Action.Code -> sendKey(action.keyCode, ctrl = ctrlHeld)
            Action.Shift -> {
                shift = !shift
                refreshKeys()
            }
            Action.Ctrl -> {
                ctrlHeld = true
                refreshKeys()
            }
            is Action.ToggleLayer -> when (action.layer) {
                Layer.FN -> {
                    // A locked Fn is unlocked on release. An unlocked Fn opens now and
                    // closes on release if it was held, or stays open if it was tapped.
                    fnDownAt = SystemClock.uptimeMillis()
                    if (!fnLocked) setLayer(Layer.FN)
                }
                Layer.SYM -> {
                    fnLocked = false
                    setLayer(if (layer == Layer.SYM) Layer.BASE else Layer.SYM)
                }
                Layer.BASE -> setLayer(Layer.BASE)
            }
            Action.Spacer -> Unit
        }
    }

    private fun onKeyUp(key: Key, view: View, event: MotionEvent) {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        if (key.action is Action.Text && key.accents.isNotEmpty()) {
            if (longPressed) pickAccent(event.rawX, event.rawY) else typeText(key.action)
        }
        when (key.action) {
            Action.Ctrl -> {
                ctrlHeld = false
                refreshKeys()
            }
            is Action.ToggleLayer -> if (key.action.layer == Layer.FN) releaseFn()
            else -> Unit
        }
        dismissPopup()
    }

    private fun onKeyCancel(key: Key) {
        handler.removeCallbacks(longPressRunnable)
        handler.removeCallbacks(repeatRunnable)
        when (key.action) {
            Action.Ctrl -> {
                ctrlHeld = false
                refreshKeys()
            }
            is Action.ToggleLayer -> if (key.action.layer == Layer.FN) releaseFn()
            else -> Unit
        }
        dismissPopup()
    }

    // Decides what releasing Fn does: exit a locked layer, lock on a tap, or leave on a hold.
    private fun releaseFn() {
        val held = SystemClock.uptimeMillis() - fnDownAt
        when {
            fnLocked -> {
                fnLocked = false
                setLayer(Layer.BASE)
            }
            held < LONG_PRESS_MS -> fnLocked = true
            else -> setLayer(Layer.BASE)
        }
    }

    private fun setLayer(target: Layer) {
        layer = target
        refreshKeys()
    }

    // The accent under the finger when it lifts is the one that gets typed.
    private fun pickAccent(rawX: Float, rawY: Float) {
        val row = accentRow ?: return
        val loc = IntArray(2)
        for (i in 0 until row.childCount) {
            val child = row.getChildAt(i)
            child.getLocationOnScreen(loc)
            val inside = rawX >= loc[0] && rawX < loc[0] + child.width &&
                rawY >= loc[1] && rawY < loc[1] + child.height
            if (inside) {
                commit((child as TextView).text.toString())
                if (shift) {
                    shift = false
                    refreshKeys()
                }
                return
            }
        }
    }

    private fun onLongPress() {
        val key = pendingKey ?: return
        val view = pendingView ?: return
        if (key.action !is Action.Text) return
        longPressed = true
        showAccents(key.accents, view)
    }

    // Commits the typed text, or sends Ctrl+letter as a key event while Ctrl is held.
    private fun typeText(action: Action.Text) {
        val text = if (shift) action.shifted else action.base
        if (ctrlHeld && action.base.length == 1 && action.base[0].isLetter()) {
            val letter = action.base[0].lowercaseChar()
            sendKey(KeyEvent.KEYCODE_A + (letter - 'a'), ctrl = true)
        } else {
            commit(text)
        }
        if (shift) {
            shift = false
            refreshKeys()
        }
    }

    private fun commit(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    // Sends a real key event with Ctrl held, so the app gets Ctrl codes, not text.
    private fun sendKey(keyCode: Int, ctrl: Boolean) {
        val ic = currentInputConnection ?: return
        val meta = if (ctrl) KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON else 0
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, meta,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD)
        )
        ic.sendKeyEvent(
            KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, meta,
                KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD)
        )
    }

    // Long-press accents: a row of choices above the key. Release on one to type it.
    private fun showAccents(accents: String, anchor: View) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(KEY_FILL)
        }
        for (ch in accents) {
            val label = if (shift) ch.uppercaseChar() else ch
            row.addView(TextView(this).apply {
                text = label.toString()
                gravity = Gravity.CENTER
                typeface = Typeface.MONOSPACE
                textSize = 18f
                setTextColor(FOREGROUND)
                setPadding(dp(14), dp(10), dp(14), dp(10))
            })
        }
        row.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val loc = IntArray(2)
        anchor.getLocationOnScreen(loc)
        val window = PopupWindow(row, row.measuredWidth, row.measuredHeight, false).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        window.showAtLocation(anchor, Gravity.NO_GRAVITY, loc[0], loc[1] - row.measuredHeight)
        popup = window
        accentRow = row
    }

    private fun dismissPopup() {
        popup?.dismiss()
        popup = null
        accentRow = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
