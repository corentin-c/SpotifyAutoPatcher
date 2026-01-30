package com.corentinc.screens.patcher

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdurazaaqmohammed.AntiSplit.main.PACKAGE_TO_PATCH
import com.corentinc.patcher.ReVancedPatcher.patch
import com.corentinc.patcher.copyUriToFile
import com.github.corentinc.SpotifyAutoPatcher.R
import com.reandroid.apk.ApkBundle
import com.reandroid.apkeditor.merge.LogUtil
import com.reandroid.apkeditor.merge.Merger
import com.reandroid.apkeditor.merge.Merger.LogListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PatcherViewModel @Inject constructor(
	@param:ApplicationContext private val context: Context,
) : ViewModel(), LogListener {
	fun onStart(defaultFolder: File) {
		uiStateFlow.update { state ->
			state.copy(shouldShowStartProcessingDialog = false)
		}
		LogUtil.setLogListener(this)
		mergeAndPatchApk(defaultFolder)
	}

	override fun onLog(msg: CharSequence) {
		onLog(msg.toString())
	}

	override fun onLog(log: String) {
		Log.i("", log)
		uiStateFlow.update { state ->
			state.copy(logText = state.logText + log + "\n")
		}
	}

	override fun onLog(resID: Int) {
		onLog(context.getString(resID))
	}

	private fun mergeAndPatchApk(defaultFolder: File) {
		onLog("Merging APK...")

		uiStateFlow.update { state ->
			state.copy(isCancelButtonVisible = true)
		}

		val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
			uiStateFlow.update { state ->
				state.copy(error = throwable)
			}
		}



		viewModelScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
			try {
				val bundle = ApkBundle()
				bundle.loadApkDirectory(
					File(
						context.packageManager.getPackageInfo(
							PACKAGE_TO_PATCH, 0
						).applicationInfo!!.sourceDir
					).parentFile, false, context
				)
				val uri = File(defaultFolder, "mergingResult.apk").toUri()
				Merger.run(bundle, defaultFolder, uri, context)
				val apk = File(defaultFolder, "unpatched.apk")
				apk.copyUriToFile(context, uri)
				startPatching(apk, defaultFolder)
			} catch (exception: Exception) {
				uiStateFlow.update { state ->
					state.copy(error = exception)
				}
			}
		}
	}

	private suspend fun startPatching(file: File, defaultFolder: File) {
		onLog(context.getString(R.string.merging_apk_succeeded))

		val success = context.getString(R.string.success_saved)
		LogUtil.logMessage(success)
		val patch = patch(
			context, file, defaultFolder,
			this
		)
		uiStateFlow.update { state ->
			state.copy(
				patchedApk = patch,
				isInstallButtonVisible = true,
				isSaveButtonVisible = true,
				isPatchingFinished = true
			)
		}
	}

	fun onPatchingFinishedHandled() {
		uiStateFlow.update { state ->
			state.copy(
				isPatchingFinished = false
			)
		}
	}

	fun onErrorHandled() {
		uiStateFlow.update { state ->
			state.copy(
				error = null
			)
		}
	}

	private val uiStateFlow = MutableStateFlow(UiState())
	val uiState: StateFlow<UiState> = uiStateFlow.asStateFlow()

	data class UiState(
		var isCopyButtonVisible: Boolean = false,
		var isInstallButtonVisible: Boolean = false,
		var isSaveButtonVisible: Boolean = false,
		var isCancelButtonVisible: Boolean = false,
		var logText: String = "",
		var error: Throwable? = null,
		var patchedApk: File? = null,
		var isPatchingFinished: Boolean = false,
		var shouldShowStartProcessingDialog: Boolean = true,
	)
}