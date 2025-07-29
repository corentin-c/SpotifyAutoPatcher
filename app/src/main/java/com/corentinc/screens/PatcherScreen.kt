package com.corentinc.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.corentinc.SpotifyAutoPatcher.R

@Composable
fun MainScreen(
	title: String,
	logText: String,
	modifier: Modifier = Modifier,
	onCopyClick: () -> Unit = {},
	onInstallClick: () -> Unit = {},
	onDownloadClick: () -> Unit = {},
	onCancelClick: () -> Unit = {}
) {
	Box(
		modifier = modifier
			.fillMaxSize()
			.background(MaterialTheme.colorScheme.background) // or windowBackground equivalent
			.systemBarsPadding()
	) {
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(top = 50.dp)
		) {
			// Scrollable content
			Column(
				modifier = Modifier
					.weight(1f)
					.verticalScroll(rememberScrollState())
					.padding(16.dp)
			) {
				Text(
					text = title,
					style = MaterialTheme.typography.headlineMedium,
					modifier = Modifier.fillMaxWidth(),
					softWrap = true
				)

				Spacer(modifier = Modifier.height(8.dp))

				Text(
					text = logText,
					style = MaterialTheme.typography.bodyMedium,
					modifier = Modifier.fillMaxWidth(),
					softWrap = true
				)
			}
		}

		// Floating action buttons
		Column(
			modifier = Modifier
				.align(Alignment.BottomEnd)
				.padding(16.dp),
			verticalArrangement = Arrangement.spacedBy(5.dp)
		) {
			// Show/hide using conditions like `if (showCopyButton)`
			FloatingActionButton(
				onClick = onCopyClick,
				modifier = Modifier,
				elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
				content = {
					Icon(
						painter = painterResource(R.drawable.copy),
						contentDescription = stringResource(R.string.copy_log)
					)
				}
			)

			FloatingActionButton(
				onClick = onInstallClick,
				elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
				content = {
					Icon(
						painter = painterResource(R.drawable.open_in_new),
						contentDescription = stringResource(R.string.install)
					)
				}
			)

			FloatingActionButton(
				onClick = onDownloadClick,
				elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
				content = {
					Icon(
						painter = painterResource(android.R.drawable.ic_menu_save),
						contentDescription = stringResource(R.string.app_name)
					)
				}
			)

			FloatingActionButton(
				onClick = onCancelClick,
				elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp),
				content = {
					Icon(
						imageVector = Icons.Default.Close, // or use painterResource
						contentDescription = stringResource(R.string.cancel)
					)
				}
			)
		}
	}
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
		MainScreen(
			title = "Spotify Auto Patcher",
			logText = "Log text goes here",
			onCopyClick = {},
			onInstallClick = {},
			onDownloadClick = {},
			onCancelClick = {}
		)
}