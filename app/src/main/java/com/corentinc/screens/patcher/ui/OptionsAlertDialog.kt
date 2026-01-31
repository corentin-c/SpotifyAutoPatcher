package com.corentinc.screens.patcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun OptionsAlertDialog(
	dialogTitle: String? = null,
	dialogText: String,
	extraContent: (@Composable () -> Unit)? = null,
	onDismissRequest: (() -> Unit)? = null,
	firstOptionText: String,
	firstOptionAction: (() -> Unit),
	secondOptionText: String? = null,
	secondOptionAction: (() -> Unit)? = null
) {
	CustomAlertDialog(
		dialogTitle = dialogTitle,
		dialogText = dialogText,
		onDismissRequest = onDismissRequest,
		extraContent = extraContent,
		buttons = {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End
			) {
				secondOptionText?.let {
					TextButton(
						modifier = Modifier.weight(0.5f),
						onClick = secondOptionAction ?: {}
					) {
						Text(secondOptionText)
					}
				}
				TextButton(
					modifier = Modifier.weight(0.5f),
					onClick = firstOptionAction
				) {
					Text(firstOptionText)
				}
			}
		}
	)
}

@Preview(showBackground = true)
@Composable
fun OptionsAlertDialogPreview() {
	OptionsAlertDialog(
		dialogTitle = "title",
		dialogText = "text",
		onDismissRequest = {
			// empty
		},
		firstOptionText = "firstButton",
		firstOptionAction = {},
		secondOptionText = "secondtButton",
		secondOptionAction = {}
	)
}