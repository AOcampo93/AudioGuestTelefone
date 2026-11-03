package com.olinix.audioguest.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeliveryChannelTest {

    @Test
    fun emailsNeedAnAtAndADomain() {
        assertTrue(DeliveryChannel.EMAIL.accepts("ana.souza@mail.pt"))
        assertTrue(DeliveryChannel.EMAIL.accepts("  ana@mail.com "))
        assertFalse(DeliveryChannel.EMAIL.accepts("ana@mail"))
        assertFalse(DeliveryChannel.EMAIL.accepts("ana mail.pt"))
        assertFalse(DeliveryChannel.EMAIL.accepts(""))
    }

    @Test
    fun whatsAppNeedsAWholeNumber() {
        assertTrue(DeliveryChannel.WHATSAPP.accepts("+351 912 345 678"))
        assertTrue(DeliveryChannel.WHATSAPP.accepts("+52 1 55-1234-5678"))
        assertFalse(DeliveryChannel.WHATSAPP.accepts("+351 "))
        assertFalse(DeliveryChannel.WHATSAPP.accepts("+351 912 abc"))
        assertFalse(DeliveryChannel.WHATSAPP.accepts("ana@mail.pt"))
    }
}
