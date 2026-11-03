package com.olinix.audioguest.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** How a guest wants to receive their message. */
enum class DeliveryChannel {
    EMAIL,
    WHATSAPP;

    /** Whether [address] looks like something the host can send to. Only catches typos; the host checks the rest. */
    fun accepts(address: String): Boolean = when (this) {
        EMAIL -> EMAIL_PATTERN.matches(address.trim())
        // A phone number with its country code: digits, maybe a leading +, spaces or dashes.
        WHATSAPP -> PHONE_PATTERN.matches(address.trim()) && address.count(Char::isDigit) in 8..15
    }

    private companion object {
        val EMAIL_PATTERN = Regex("""[^@\s]+@[^@\s]+\.[^@\s]{2,}""")
        val PHONE_PATTERN = Regex("""\+?[0-9][0-9 ()-]*""")
    }
}

/**
 * A guest asked to receive their message. For now the host sends it by hand after the event,
 * by e-mail or WhatsApp, and ticks it off in the panel.
 */
@Entity(tableName = "deliveries", indices = [Index("eventId"), Index("recordingId")])
data class Delivery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: Long,
    val recordingId: Long,
    /** The message's file, so an exported list still says which file goes to whom. */
    val fileName: String,
    val channel: DeliveryChannel,
    /** The e-mail address or the WhatsApp number, as the guest typed it. */
    val address: String,
    /** The guest also ticked "Quero receber novidades". */
    val wantsNews: Boolean,
    /** When the guest asked, in epoch milliseconds. */
    val createdAt: Long,
    /** When the host marked it as sent, or null while it is still to be sent. */
    val sentAt: Long? = null,
)
