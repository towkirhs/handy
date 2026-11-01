package com.towkirhs.handy.access

import kotlinx.coroutines.flow.MutableStateFlow

/** Whether an editable, non-password text field currently has focus. Drives the floating bar's visibility. */
object FocusState {
    val editable = MutableStateFlow(false)
}
