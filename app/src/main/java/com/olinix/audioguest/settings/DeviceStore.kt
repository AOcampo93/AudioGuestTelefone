package com.olinix.audioguest.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private val Context.hostSettings: DataStore<Preferences> by preferencesDataStore(name = "host_settings")

/**
 * Small values that belong to the tablet rather than to one event: which event is active.
 * The same file holds the panel's settings from before events existed, read once to create the first event.
 */
class DeviceStore(context: Context) {

    private val store = context.applicationContext.hostSettings

    suspend fun activeEventId(): Long? = store.data.first()[ACTIVE_EVENT]

    suspend fun setActiveEventId(id: Long) {
        store.edit { it[ACTIVE_EVENT] = id }
    }

    /** The settings saved before events existed, or null if the panel was never used. */
    suspend fun legacySettings(): HostSettings? = store.data.first().toSettings()

    private fun Preferences.toSettings(): HostSettings? {
        if (this[TYPE] == null) return null
        val defaults = HostSettings()
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
        val ACTIVE_EVENT = longPreferencesKey("active_event")
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
