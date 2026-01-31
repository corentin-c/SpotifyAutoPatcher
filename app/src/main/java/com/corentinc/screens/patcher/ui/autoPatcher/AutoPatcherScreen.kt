package com.corentinc.screens.patcher.ui.autoPatcher

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.corentinc.patcher.ApplicationSupported
import com.corentinc.screens.patcher.ui.OptionsAlertDialog
import com.github.corentinc.SpotifyAutoPatcher.R
import java.io.File

@Composable
fun AutoPatcherScreen(
    title: String,
    modifier: Modifier = Modifier,
    onCopyClick: (String) -> Unit = {},
    onInstallClick: (File?) -> Unit = {},
    onDownloadClick: (File?) -> Unit = {},
    onCancelClick: () -> Unit = {},
    onPatchingFinished: (File?) -> Unit = {},
    onError: (error: Throwable) -> Unit = {},
    defaultFolder: File,
    applicationChosen: ApplicationSupported,
    viewModel: PatcherViewModel = hiltViewModel()
) {

    val uiState = viewModel.uiState.collectAsState().value
    if (uiState.isPatchingFinished) {
        onPatchingFinished(uiState.patchedApk)
        viewModel.onPatchingFinishedHandled()
    }

    if (uiState.error != null) {
        onError(uiState.error!!)
        viewModel.onErrorHandled()
    }

    if (uiState.shouldShowStartProcessingDialog) {
        OptionsAlertDialog(
            dialogText = stringResource(
                R.string.before_start_message,
                applicationChosen.nameToDisplay
            ),
            firstOptionText = stringResource(R.string.start),
            firstOptionAction = {
                viewModel.onStart(defaultFolder, applicationChosen)
                viewModel.onAlertDialogHandled()
            },
        )
    }

    AutoPatcherScreenContent(
        title = title,
        modifier = modifier,
        onCopyClick = {
            onCopyClick(uiState.logText)
        },
        logText = uiState.logText,
        isCopyButtonVisible = uiState.isCopyButtonVisible,
        isInstallButtonVisible = uiState.isInstallButtonVisible,
        isSaveButtonVisible = uiState.isSaveButtonVisible,
        isCancelButtonVisible = uiState.isCancelButtonVisible,
        onInstallClick = {
            onInstallClick(uiState.patchedApk)
        },
        onDownloadClick = {
            onDownloadClick(uiState.patchedApk)
        },
        onCancelClick = onCancelClick
    )
}

@Composable
fun AutoPatcherScreenContent(
    title: String,
    modifier: Modifier = Modifier,
    logText: String = "",
    isCopyButtonVisible: Boolean = true,
    isInstallButtonVisible: Boolean = true,
    isSaveButtonVisible: Boolean = true,
    isCancelButtonVisible: Boolean = true,
    onCopyClick: () -> Unit = {},
    onInstallClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onCancelClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
			.fillMaxSize()
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
            if (isCopyButtonVisible) {
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
            }

            if (isInstallButtonVisible) {
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
            }

            if (isSaveButtonVisible) {
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
            }

            if (isCancelButtonVisible) {
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
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    AutoPatcherScreenContent(
        title = "Spotify Auto Patcher",
        logText = "Log text goes here",
        onCopyClick = {
            // empty
        },
        onInstallClick = {
            // empty
        },
        onDownloadClick = {
            // empty
        },
        onCancelClick = {
            // empty
        }
    )
}