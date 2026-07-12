package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalAppearanceDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val dynamicColor: Flow<Boolean> = dataStore.data
        .map { it[DynamicColorKey] ?: true }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit {
            it[DynamicColorKey] = enabled
        }
    }

    companion object {
        private val DynamicColorKey = booleanPreferencesKey("dynamic_color_enabled")
    }
}
