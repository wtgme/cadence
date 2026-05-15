package io.cadence.music.data.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")
private val KEY_COMPLETED = booleanPreferencesKey("completed")

@Singleton
class OnboardingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val completed: Flow<Boolean> = context.onboardingDataStore.data
        .map { it[KEY_COMPLETED] ?: false }

    suspend fun markCompleted() {
        context.onboardingDataStore.edit { it[KEY_COMPLETED] = true }
    }
}
