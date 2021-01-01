package io.github.edsuns.adfilter

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Created by Edsuns@qq.com on 2021/1/1.
 */
internal class FilterSharedPreferences(private val context: Context) {

    val hasInstallation: Boolean
        get() = preferences.contains(KEY_FILTER_MAP)

    var isEnabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, true)
        set(value) = preferences.edit { putBoolean(KEY_ENABLED, value) }

    var filterMap: String
        get() = preferences.getString(KEY_FILTER_MAP, "{}")!!
        set(value) = preferences.edit { putString(KEY_FILTER_MAP, value) }

    private val preferences: SharedPreferences
        get() = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE)

    companion object {
        private const val FILENAME = "io.github.edsuns.filter"
        private const val KEY_FILTER_MAP = "filter_map"
        private const val KEY_ENABLED = "filter_enabled"
    }
}