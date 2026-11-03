package com.olinix.audioguest.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** The host's settings. The session talks to this interface, so tests can keep them in memory. */
interface SettingsRepository {

    /** What is saved now, or the defaults the first time. */
    suspend fun load(): HostSettings

    suspend fun save(settings: HostSettings)
}

private val Context.hostSettings: DataStore<Preferences> by preferencesDataStore(name = "host_settings")

/** Keeps the settings in a small key-value file on the tablet, so they survive restarts. */
class StoredSettingsRepository(context: Context) : SettingsRepository {

    private val store = context.applicationContext.hostSettings

    private val stored: Flow<HostSettings> = store.data.map { it.toSettings() }

    override suspend fun load(): HostSettings = stored.first()

    override suspend fun save(settings: HostSettings) {
        store.edit { prefs ->
            prefs[TYPE] = settings.type.name
            prefs[NAME_1] = settings.name1
            prefs[NAME_2] = settings.name2
            prefs[FAMILY] = settings.family
            prefs[BIRTHDAY_NAME] = settings.birthdayName
            if (settings.birthdayAge != null) prefs[BIRTHDAY_AGE] = settings.birthdayAge else prefs.remove(BIRTHDAY_AGE)
            prefs[COMPANY_EVENT] = settings.companyEvent
            prefs[COMPANY] = settings.company
            prefs[OTHER_TITLE] = settings.otherTitle
            prefs[OTHER_HOSTS] = settings.otherHosts
            prefs[DATE] = settings.date.toString()
            prefs[ACCENT] = settings.accent.name
            prefs[WELCOME_VIDEO] = settings.welcomeVideo
            prefs[WELCOME_SOUND] = settings.welcomeSound.name
            prefs[LANGUAGE] = settings.language.name
            prefs[ALLOWED] = settings.allowed.name
            prefs[SHOW_MESSAGES] = settings.showMessages
            prefs[ALLOW_SHARING] = settings.allowSharing
            prefs[SHOW_MODE] = settings.showMode
            prefs[SHOW_AFTER] = settings.showAfterSeconds
            prefs[CALL_MODE] = settings.callMode
            prefs[CALL_EVERY] = settings.callEveryMinutes
            prefs[CALL_ANSWER] = settings.callAnswer.name
        }
    }

    private fun Preferences.toSettings(): HostSettings {
        val defaults = HostSettings()
        // Saved before any setting existed: everything keeps its default, including the age.
        if (this[TYPE] == null) return defaults
        return HostSettings(
            type = enumOr(this[TYPE], defaults.type),
            name1 = this[NAME_1] ?: defaults.name1,
            name2 = this[NAME_2] ?: defaults.name2,
            family = this[FAMILY] ?: defaults.family,
            birthdayName = this[BIRTHDAY_NAME] ?: defaults.birthdayName,
            birthdayAge = this[BIRTHDAY_AGE],
            companyEvent = this[COMPANY_EVENT] ?: defaults.companyEvent,
            company = this[COMPANY] ?: defaults.company,
            otherTitle = this[OTHER_TITLE] ?: defaults.otherTitle,
            otherHosts = this[OTHER_HOSTS] ?: defaults.otherHosts,
            date = this[DATE]?.let(LocalDate::parse) ?: defaults.date,
            accent = enumOr(this[ACCENT], defaults.accent),
            welcomeVideo = this[WELCOME_VIDEO] ?: defaults.welcomeVideo,
            welcomeSound = enumOr(this[WELCOME_SOUND], defaults.welcomeSound),
            language = enumOr(this[LANGUAGE], defaults.language),
            allowed = enumOr(this[ALLOWED], defaults.allowed),
            showMessages = this[SHOW_MESSAGES] ?: defaults.showMessages,
            allowSharing = this[ALLOW_SHARING] ?: defaults.allowSharing,
            showMode = this[SHOW_MODE] ?: defaults.showMode,
            showAfterSeconds = this[SHOW_AFTER] ?: defaults.showAfterSeconds,
            callMode = this[CALL_MODE] ?: defaults.callMode,
            callEveryMinutes = this[CALL_EVERY] ?: defaults.callEveryMinutes,
            callAnswer = enumOr(this[CALL_ANSWER], defaults.callAnswer),
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: fallback

    private companion object {
        val TYPE = stringPreferencesKey("type")
        val NAME_1 = stringPreferencesKey("name1")
        val NAME_2 = stringPreferencesKey("name2")
        val FAMILY = stringPreferencesKey("family")
        val BIRTHDAY_NAME = stringPreferencesKey("birthday_name")
        val BIRTHDAY_AGE = intPreferencesKey("birthday_age")
        val COMPANY_EVENT = stringPreferencesKey("company_event")
        val COMPANY = stringPreferencesKey("company")
        val OTHER_TITLE = stringPreferencesKey("other_title")
        val OTHER_HOSTS = stringPreferencesKey("other_hosts")
        val DATE = stringPreferencesKey("date")
        val ACCENT = stringPreferencesKey("accent")
        val WELCOME_VIDEO = booleanPreferencesKey("welcome_video")
        val WELCOME_SOUND = stringPreferencesKey("welcome_sound")
        val LANGUAGE = stringPreferencesKey("language")
        val ALLOWED = stringPreferencesKey("allowed")
        val SHOW_MESSAGES = booleanPreferencesKey("show_messages")
        val ALLOW_SHARING = booleanPreferencesKey("allow_sharing")
        val SHOW_MODE = booleanPreferencesKey("show_mode")
        val SHOW_AFTER = intPreferencesKey("show_after")
        val CALL_MODE = booleanPreferencesKey("call_mode")
        val CALL_EVERY = intPreferencesKey("call_every")
        val CALL_ANSWER = stringPreferencesKey("call_answer")
    }
}
