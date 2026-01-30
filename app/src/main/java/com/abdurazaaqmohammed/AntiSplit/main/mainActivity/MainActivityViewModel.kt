package com.abdurazaaqmohammed.AntiSplit.main.mainActivity

import androidx.lifecycle.ViewModel
import com.corentinc.screens.patcher.ui.AlertDialogData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor() : ViewModel() {
	private val uiStateFlow = MutableStateFlow(UiState())
	val uiState: StateFlow<UiState> = uiStateFlow.asStateFlow()

	fun displayAlertDialog(alertDialogData: AlertDialogData) {
		uiStateFlow.update { state ->
			state.copy(
				alertDialogData = alertDialogData
			)
		}
	}

	fun onAlertDialogDismissed() {
		uiStateFlow.update { state ->
			state.copy(
				alertDialogData = null
			)
		}
	}

	data class UiState(
		var alertDialogData: AlertDialogData? = null,
	)
}