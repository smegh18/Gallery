package com.quizango.gallery.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Undo/redo history of editor states. [record] ignores a state equal to the current one,
 * and drops the redo branch when a new edit is made. Oldest entries go past [limit].
 */
class EditHistory<T>(private val limit: Int = 30) {
    private val states = mutableListOf<T>()
    private var index by mutableIntStateOf(-1) // Compose-observable so buttons enable/disable

    val canUndo: Boolean get() = index > 0
    val canRedo: Boolean get() = index in 0 until states.size - 1

    fun record(state: T) {
        if (index >= 0 && states[index] == state) return
        while (states.size > index + 1) states.removeAt(states.size - 1)
        states += state
        if (states.size > limit) states.removeAt(0)
        index = states.size - 1
    }

    fun undo(): T? = if (canUndo) states[--index] else null
    fun redo(): T? = if (canRedo) states[++index] else null
}
