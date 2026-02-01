package com.corentinc.screens.patcher.ui

data class AlertDialogData(
	val title: String? = null,
	val text: String,
	val positiveButtonText: String,
	val positiveButtonAction: () -> Unit,
	val neutralButtonText: String? = null,
	val neutralButtonAction: (() -> Unit)? = null,
)
