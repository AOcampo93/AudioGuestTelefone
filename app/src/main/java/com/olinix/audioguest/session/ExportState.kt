package com.olinix.audioguest.session

import com.olinix.audioguest.export.ExportResult

/** Where an export of an event stands, for the panel to show. */
sealed interface ExportState {
    val eventId: Long

    data class Working(override val eventId: Long) : ExportState

    /** [share] is true until the share sheet with the exported files has been opened. */
    data class Done(override val eventId: Long, val result: ExportResult, val share: Boolean) : ExportState

    data class Failed(override val eventId: Long) : ExportState
}
