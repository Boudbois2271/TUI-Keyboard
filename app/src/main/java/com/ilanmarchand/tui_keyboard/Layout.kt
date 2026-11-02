// SPDX-License-Identifier: GPL-3.0-or-later

package com.ilanmarchand.tui_keyboard

import android.view.KeyEvent

// The whole keyboard is data: edit the rows here to change a layer.
// Every layer has the same shape (5 rows, same key counts and weights), so the
// service can reuse its views and only change their labels and actions.

enum class Layer { BASE, FN, SYM }

sealed interface Action {
    // Text typed on press. `shifted` is used while Shift is on.
    data class Text(val base: String, val shifted: String = base) : Action

    // A real key event, such as Esc, Tab, Enter, Backspace or the arrows.
    data class Code(val keyCode: Int) : Action

    data object Shift : Action
    data object Ctrl : Action
    data object Space : Action
    data object Backspace : Action
    data class ToggleLayer(val layer: Layer) : Action
    data object Spacer : Action
}

// `accents` is the long-press list for a letter, the way French needs it.
// Weight is the key width relative to the other keys of its row.
data class Key(
    val action: Action,
    val weight: Float = 1f,
    val accents: String = "",
)

private fun letter(c: Char, accents: String = ""): Key {
    val upper = c.uppercaseChar().toString()
    return Key(Action.Text(c.toString(), upper), accents = accents)
}

private fun symbol(text: String) = Key(Action.Text(text))

private fun code(keyCode: Int, weight: Float = 1f) = Key(Action.Code(keyCode), weight)

private val blank = Key(Action.Spacer)

private fun blanks(count: Int): List<Key> = List(count) { blank }

// Half a key of empty space on each side, to centre a 9-key row under a 10-key row.
private fun centred(keys: List<Key>): List<Key> =
    listOf(Key(Action.Spacer, 0.5f)) + keys + listOf(Key(Action.Spacer, 0.5f))

object Layout {

    private val digits = listOf(
        Key(Action.Text("1", "!")), Key(Action.Text("2", "@")), Key(Action.Text("3", "#")),
        Key(Action.Text("4", "$")), Key(Action.Text("5", "%")), Key(Action.Text("6", "^")),
        Key(Action.Text("7", "&")), Key(Action.Text("8", "*")), Key(Action.Text("9", "(")),
        Key(Action.Text("0", ")")),
    )

    private val qRow = listOf(
        letter('q'), letter('w'), letter('e', "éèêë"), letter('r'), letter('t'),
        letter('y', "ÿ"), letter('u', "ùûüú"), letter('i', "îïí"), letter('o', "ôœöó"), letter('p'),
    )

    private val aRow = listOf(
        letter('a', "àâäæá"), letter('s', "ß"), letter('d'), letter('f'), letter('g'),
        letter('h'), letter('j'), letter('k'), letter('l'),
    )

    private val zRow = listOf(
        letter('z'), letter('x'), letter('c', "ç"), letter('v'), letter('b'),
        letter('n', "ñ"), letter('m'),
    )

    private val shiftKey = Key(Action.Shift, 1.5f)
    private val backspaceKey = Key(Action.Backspace, 1.5f)

    // Bottom row, left to right: Ctrl, Fn, Space, Sym, Enter.
    // Each key but Space is 1.5 units wide, the width of the Enter key. Space takes the rest.
    private val bottomRow = listOf(
        Key(Action.Ctrl, 1.5f),
        Key(Action.ToggleLayer(Layer.FN), 1.5f),
        Key(Action.Space, 4f),
        Key(Action.ToggleLayer(Layer.SYM), 1.5f),
        code(KeyEvent.KEYCODE_ENTER, 1.5f),
    )

    // Half a key of comma and period on each side of the a row.
    private val aRowWithPunctuation =
        listOf(Key(Action.Text(","), 0.5f)) + aRow + listOf(Key(Action.Text("."), 0.5f))

    val base: List<List<Key>> = listOf(
        digits,
        qRow,
        aRowWithPunctuation,
        listOf(shiftKey) + zRow + listOf(backspaceKey),
        bottomRow,
    )

    // Fn sends keys and never types text. Fn + Q is Esc, Fn + A is Tab.
    // Fn + W/E/R/T are Home, End, PgUp, PgDn, Fn + D is Del, and Fn + H/J/K/L are
    // the arrows in vim's order (left, down, up, right).
    val fn: List<List<Key>> = listOf(
        blanks(10),
        listOf(
            code(KeyEvent.KEYCODE_ESCAPE),
            code(KeyEvent.KEYCODE_MOVE_HOME),
            code(KeyEvent.KEYCODE_MOVE_END),
            code(KeyEvent.KEYCODE_PAGE_UP),
            code(KeyEvent.KEYCODE_PAGE_DOWN),
        ) + blanks(5),
        centred(
            listOf(
                code(KeyEvent.KEYCODE_TAB),
                blank,
                code(KeyEvent.KEYCODE_FORWARD_DEL),
                blank,
                blank,
                code(KeyEvent.KEYCODE_DPAD_LEFT),
                code(KeyEvent.KEYCODE_DPAD_DOWN),
                code(KeyEvent.KEYCODE_DPAD_UP),
                code(KeyEvent.KEYCODE_DPAD_RIGHT),
            )
        ),
        listOf(shiftKey) + blanks(7) + listOf(backspaceKey),
        bottomRow,
    )

    // Sym types text only: the PC punctuation, grouped by where it sits on the PC.
    val sym: List<List<Key>> = listOf(
        listOf("|", "\\", "(", ")", "[", "]", "{", "}", "<", ">").map { symbol(it) },
        listOf("'", "\"", "`", "~", "?", "/", ",", ".", "-", "=").map { symbol(it) },
        centred(listOf(":", ";", "_", "+", "@", "&", "%", "^", "*").map { symbol(it) }),
        listOf(shiftKey, symbol("!"), symbol("#"), symbol("$")) + blanks(4) + listOf(backspaceKey),
        bottomRow,
    )

    fun rows(layer: Layer): List<List<Key>> = when (layer) {
        Layer.BASE -> base
        Layer.FN -> fn
        Layer.SYM -> sym
    }
}
