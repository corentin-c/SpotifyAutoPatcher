package com.corentinc.screens.patcher.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun CustomAlertDialog(
	dialogTitle: String? = null,
	dialogText: String,
	extraContent: (@Composable () -> Unit)? = null,
	onDismissRequest: (() -> Unit)? = null,
	buttons: @Composable () -> Unit
) {
	AlertDialog(
		shape = RoundedCornerShape(0.dp),
		title = {
			dialogTitle?.let {
				Text(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 12.dp),
					text = it,
					style = MaterialTheme.typography.titleLarge,
				)
			}
		},
		text = {
			Column(
				horizontalAlignment = Alignment.CenterHorizontally,
			) {
				Text(
					modifier = Modifier.fillMaxWidth(),
					text = dialogText,
					style = MaterialTheme.typography.bodyMedium
				)
				extraContent?.invoke()
			}
		},
		onDismissRequest = {
			onDismissRequest?.invoke()
		},
		confirmButton = {
			buttons()
		}
	)
}

@Preview(showBackground = true)
@Composable
fun CustomAlertDialogPreview() {
	CustomAlertDialog(
		dialogTitle = "title",
		dialogText = "text",
		onDismissRequest = {
			// empty
		},
		buttons = {
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.End
			) {
				TextButton(onClick = {}) {
					Text("Button")
				}
				TextButton(onClick = {}) {
					Text("Button")
				}
			}
		}
	)
}
