package com.corentinc.screens.patcher.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
		containerColor = Color.Black,
		title = {
			dialogTitle?.let {
				Text(
					modifier = Modifier
						.fillMaxWidth()
						.padding(vertical = 12.dp),
					text = it,
					color = Color.White,
					style = MaterialTheme.typography.titleSmall,
					fontSize = 12.sp,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
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
					color = Color.White,
					style = MaterialTheme.typography.labelSmall,
					textAlign = TextAlign.Left
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
			Text(text = "Button")
		}
	)
}
