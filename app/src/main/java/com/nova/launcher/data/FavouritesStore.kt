package com.nova.launcher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** User-pinned favourites (in addition to the four hero apps), stored as ordered package names. */
class FavouritesStore(context: Context) {
    private val prefs = context.getSharedPreferences("nova_favourites", Context.MODE_PRIVATE)
    private val _pinned = MutableStateFlow(read())
    val pinned: StateFlow<List<String>> = _pinned

    private fun read(): List<String> =
        prefs.getString(KEY, null)?.split('\n')?.filter { it.isNotBlank() } ?: emptyList()

    fun toggle(packageName: String) {
        val current = _pinned.value
        val next = if (packageName in current) current - packageName else current + packageName
        prefs.edit().putString(KEY, next.joinToString("\n")).apply()
        _pinned.value = next
    }

    private companion object { const val KEY = "pinned" }
}
