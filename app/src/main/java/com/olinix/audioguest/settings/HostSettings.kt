package com.olinix.audioguest.settings

import com.olinix.audioguest.audio.SoundOutput
import com.olinix.audioguest.session.RecordingMode
import java.time.LocalDate

/** The kinds of event the panel offers. Each one asks for different names. */
enum class EventType(val defaultAccent: Accent) {
    WEDDING(Accent.GOLD),
    CHRISTMAS(Accent.RED),
    BIRTHDAY(Accent.PINK),
    COMPANY(Accent.BLUE),
    OTHER(Accent.GOLD),
}

/** The five accent colors of the mockups. */
enum class Accent(val argb: Long) {
    GOLD(0xFFD6AB62),
    RED(0xFFE0735F),
    GREEN(0xFF8FBF8A),
    BLUE(0xFF8FB3E0),
    PINK(0xFFE3A0B4),
}

/** Language of the whole app, guest screens and host panel alike. */
enum class GuestLanguage(val tag: String) {
    /** Portugal's Portuguese: the pt texts with its own plural rules, where 0 counts as plural. */
    PORTUGUESE("pt-PT"),
    SPANISH("es"),
    ENGLISH("en"),
}

/** Which messages guests may leave. The gallery and show mode only show these too. */
enum class AllowedMessages(val modes: Set<RecordingMode>) {
    BOTH(setOf(RecordingMode.AUDIO, RecordingMode.VIDEO)),
    VOICE_ONLY(setOf(RecordingMode.AUDIO)),
    VIDEO_ONLY(setOf(RecordingMode.VIDEO)),
}

/**
 * Everything the host sets up in the panel. Each event type keeps its own names, so switching
 * types back and forth does not lose what was typed. Defaults are the mockup's examples.
 */
data class HostSettings(
    val type: EventType = EventType.WEDDING,
    val name1: String = "Ana",
    val name2: String = "Miguel",
    val family: String = "Família Souza",
    val birthdayName: String = "Lucía",
    /** Optional. With an age the invitation reads "40 años de" instead of "Cumpleaños de". */
    val birthdayAge: Int? = 40,
    val companyEvent: String = "Cena de fin de año",
    val company: String = "Grupo Atlas",
    val otherTitle: String = "Graduación de",
    val otherHosts: String = "Sofía Ramírez",
    val date: LocalDate = LocalDate.of(2027, 6, 14),
    val accent: Accent = Accent.GOLD,
    /** Play the host's welcome video when a guest taps the invitation. */
    val welcomeVideo: Boolean = false,
    val welcomeSound: SoundOutput = SoundOutput.HANDSET,
    val language: GuestLanguage = GuestLanguage.PORTUGUESE,
    val allowed: AllowedMessages = AllowedMessages.BOTH,
    /** "Ver grabaciones" in the menu. Off, messages are still saved but not shown on the tablet. */
    val showMessages: Boolean = true,
    /** "Compartir mi mensaje" on the thanks screen, through Android's share sheet. */
    val allowSharing: Boolean = true,
    /** Plays saved messages at random while nobody uses the booth. Needs [showMessages]. */
    val showMode: Boolean = false,
    val showAfterSeconds: Int = 60,
    /** The booth rings now and then; whoever answers hears the host's question and answers it. */
    val callMode: Boolean = false,
    val callEveryMinutes: Int = 10,
    /** How guests answer the call, when both kinds are allowed. */
    val callAnswer: RecordingMode = RecordingMode.AUDIO,
) {
    /** Changing the type also brings back that type's default color. */
    fun withType(newType: EventType): HostSettings = copy(type = newType, accent = newType.defaultAccent)

    /**
     * A new event keeps how the booth behaves here (language, allowed messages, show and call
     * timing) but starts with its own names on [date], and with no welcome or question video yet.
     */
    fun forNewEvent(date: LocalDate): HostSettings = HostSettings(
        date = date,
        welcomeSound = welcomeSound,
        language = language,
        allowed = allowed,
        showMessages = showMessages,
        allowSharing = allowSharing,
        showMode = showMode,
        showAfterSeconds = showAfterSeconds,
        callEveryMinutes = callEveryMinutes,
        callAnswer = callAnswer,
    )

    /** The big line of the invitation: the couple, the family, the person or the organizer. */
    val hosts: String
        get() = when (type) {
            EventType.WEDDING -> listOf(name1, name2).filter { it.isNotBlank() }.joinToString(" & ")
            EventType.CHRISTMAS -> family
            EventType.BIRTHDAY -> birthdayName
            EventType.COMPANY -> company
            EventType.OTHER -> otherHosts
        }.trim()

    /** Show mode only makes sense when guests may see the messages. */
    val showModeActive: Boolean get() = showMode && showMessages

    /** Whether guests may leave, see and hear messages of this kind. */
    fun allows(mode: RecordingMode): Boolean = mode in allowed.modes

    /** How the call is answered: the host's pick, unless that kind is not allowed any more. */
    val callAnswerMode: RecordingMode get() = if (allows(callAnswer)) callAnswer else allowed.modes.first()

    companion object {
        /** Character limits from the mockup's "Campos por tipo de evento" table. */
        const val MAX_NAME = 14
        const val MAX_FAMILY = 24
        const val MAX_BIRTHDAY_NAME = 18
        const val MAX_AGE = 120
        const val MAX_TITLE = 28
        const val MAX_ORGANIZER = 24

        /** "Empezar tras" choices for show mode. */
        val SHOW_DELAYS = listOf(30, 60, 180)

        /** "Llamar cada" choices for call mode, in minutes. */
        val CALL_INTERVALS = listOf(5, 10, 20)
    }
}
