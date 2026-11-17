package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.HindiCourse
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studyVaultDataStore by preferencesDataStore(name = "studyvault_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val HINDI_COURSE = stringPreferencesKey("hindi_course")
    }

    val themeMode: Flow<ThemeMode> = context.studyVaultDataStore.data.map { prefs ->
        val raw = prefs[Keys.THEME_MODE]
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.DARK
    }

    val hindiCourse: Flow<HindiCourse> = context.studyVaultDataStore.data.map { prefs ->
        HindiCourse.fromRaw(prefs[Keys.HINDI_COURSE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.studyVaultDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setHindiCourse(course: HindiCourse) {
        context.studyVaultDataStore.edit { prefs ->
            prefs[Keys.HINDI_COURSE] = course.name
        }
    }
}
