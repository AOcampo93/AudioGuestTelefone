package com.olinix.audioguest.export

import com.olinix.audioguest.data.Recording
import com.olinix.audioguest.session.RecordingMode
import com.olinix.audioguest.settings.EventType
import com.olinix.audioguest.settings.HostSettings
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ExportNamesTest {

    @Test
    fun eachEventGetsAFolderNamedByDateAndHosts() {
        val settings = HostSettings(type = EventType.WEDDING, name1 = "Ana", name2 = "Miguel", date = LocalDate.of(2027, 6, 14))
        assertEquals("2027-06-14 Ana & Miguel", exportFolderName(settings))
        assertEquals("2027-06-14 ACDC", exportFolderName(settings.copy(type = EventType.COMPANY, company = "AC/DC")))
    }

    @Test
    fun filesAreNamedByWhenTheyWereRecorded() {
        // 14 June 2027, 21:42:00 UTC.
        val recording = Recording(1, 1, RecordingMode.VIDEO, "video-1813009320000.mp4", 30, savedAt = 1_813_009_320_000)
        assertEquals("2027-06-14 21.42.00 Vídeo.mp4", exportFileName(recording, "Vídeo", ZoneId.of("UTC")))
    }

    @Test
    fun cellsWithSeparatorsAreQuotedAndFormulasNeutralized() {
        assertEquals("ana@mail.pt", csvCell("ana@mail.pt"))
        assertEquals("\"a;b\"", csvCell("a;b"))
        assertEquals("\"say \"\"hi\"\"\"", csvCell("say \"hi\""))
        assertEquals("'+351 912 345 678", csvCell("+351 912 345 678"))
        assertEquals("'=1+1@x.pt", csvCell("=1+1@x.pt"))
    }

    @Test
    fun theListStartsWithTheHeader() {
        val text = csvText("file;contact", listOf(listOf("a.mp4", "ana@mail.pt")))
        assertEquals("\uFEFFfile;contact\r\na.mp4;ana@mail.pt\r\n", text)
    }
}
