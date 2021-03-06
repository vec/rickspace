package com.app.juawcevada.rickspace.ui.shared

import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar

data class SnackbarMessage(
    @StringRes val messageId: Int,
    val duration: Int = Snackbar.LENGTH_SHORT
)