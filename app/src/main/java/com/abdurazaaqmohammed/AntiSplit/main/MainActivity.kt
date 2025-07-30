package com.abdurazaaqmohammed.AntiSplit.main

import android.Manifest
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.corentinc.patcher.AppInstaller
import com.corentinc.patcher.AppUpdater
import com.corentinc.patcher.clearDirectory
import com.corentinc.patcher.isNetworkException
import com.corentinc.patcher.saveToDownloadsFolder
import com.corentinc.screens.patcher.ui.AutoPatcherScreen
import com.github.corentinc.SpotifyAutoPatcher.R
import com.github.corentinc.httpcodescats.ui.theme.AutoPatcherTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reandroid.apkeditor.merge.LogUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.channels.ClosedByInterruptException
import java.util.Calendar
import java.util.zip.ZipException
import kotlin.io.path.createDirectory

const val PACKAGE_TO_PATCH = "com.google.android.apps.youtube.music"
private const val TEMP_FOLDER = "temp"

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
	private lateinit var defaultFolder: File

	private val requestWritePermissionLauncher = registerForActivityResult(
		ActivityResultContracts.RequestPermission()
	) { isGranted: Boolean ->
		if (!isGranted) {
			runOnUiThread {
				showAlertDialog(
					getString(R.string.storage_permission_denied_warning),
					positiveButtonText = getString(R.string.ok),
					positiveButtonAction = {
						// empty
					},
				)
			}
		}
	}

	private val uninstallCallback =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
			patchedApk?.let {
				installAppOrShowPopUpIfAlreadyInstalled(it)
			}
		}


	private var logField: TextView? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			) != PackageManager.PERMISSION_GRANTED
		) {
			requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		}
		setContent {
			AutoPatcherTheme {
				AutoPatcherScreen(
					title = getString(R.string.app_name),
					onPatchingFinished = {
						showAlertDialog(
							getString(R.string.ready_to_install),
							positiveButtonText = getString(R.string.next),
							positiveButtonAction = {
								AppInstaller.uninstallApp(uninstallCallback)
							},
						)
					},
					onCopyClick = { text ->
						copyText(
							text
						)
					},
					onInstallClick = { patch ->
						patch?.let {
							patchedApk = patch
							installAppOrShowPopUpIfAlreadyInstalled(patch)
						}
					},
					onCancelClick = {
						restartActivity()
					},
					onDownloadClick = { patch ->
						patch?.let {
							patchedApk = patch
							installAppOrShowPopUpIfAlreadyInstalled(patch)
						}
					},
					onError = { error ->
						showError(error)
					},
					defaultFolder = defaultFolder
				)
			}
		}

		defaultFolder = File(cacheDir, TEMP_FOLDER)
		if (!defaultFolder.exists()) defaultFolder.toPath().createDirectory()
		defaultFolder.clearDirectory()
		WindowCompat.setDecorFitsSystemWindows(window, false)

		LogUtil.logEnabled = true

		lifecycleScope.launch(Dispatchers.IO) {
			AppUpdater.checkIfAnUpdateIsAvailable(
				this@MainActivity,
				defaultFolder,
				onUpdateAvailable = { latestVersionApk ->
					showAlertDialog(
						getString(R.string.a_new_version_of_spotifyautopatcher_is_available_do_you_want_to_install_it),
						positiveButtonText = getString(R.string.install),
						positiveButtonAction = {
							AppInstaller.installApp(this@MainActivity, latestVersionApk)
							showStartProcessDialog()

						},
						neutralButtonText = getString(R.string.skip),
						neutralButtonAction = {
							showStartProcessDialog()
						}
					)
				},
				onUpdateNotAvailable = {
					LogUtil.logMessage(getString(R.string.no_update_available))
					showStartProcessDialog()
				}).onFailure { exception ->
				LogUtil.logMessage(
					getString(
						R.string.could_not_check_for_spotifyautopatcher_updates,
						exception
					)
				)
				showStartProcessDialog()
			}
		}
	}

	private fun showStartProcessDialog() {
		showAlertDialog(
			getString(R.string.before_start_message),
			positiveButtonText = getString(R.string.start),
			positiveButtonAction = {
				//mergeAndPatchApk()
			},
		)
	}

	private fun showAlertDialog(
		text: String,
		positiveButtonText: String,
		positiveButtonAction: () -> Unit,
		neutralButtonText: String? = null,
		neutralButtonAction: (() -> Unit)? = null,
	) {
		runOnUiThread {
			val builder = MaterialAlertDialogBuilder(this)
			builder.setCancelable(false)
			builder.setMessage(text)
			builder.setPositiveButton(
				positiveButtonText
			) { dialog: DialogInterface, _: Int ->
				positiveButtonAction()
				dialog.dismiss()
			}
			neutralButtonText?.let {
				neutralButtonAction?.let {
					builder.setNeutralButton(
						neutralButtonText
					) { dialog: DialogInterface, _: Int ->
						neutralButtonAction()
						dialog.dismiss()
					}
				}
			}
			styleAlertDialog(builder.create())
		}
	}

	private fun styleAlertDialog(ad: AlertDialog) {
		val w = ad.window
		if (w != null) {
			val border = GradientDrawable()
			val typedValue = TypedValue()
			theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
			border.setStroke(5, typedValue.data) // Border width and color
			border.cornerRadius = 24f
			w.setBackgroundDrawable(border)
			val m = 0.8
			val displayMetrics = this.resources.displayMetrics
			val height = (displayMetrics.heightPixels * m).toInt()
			val width = (displayMetrics.widthPixels * m).toInt()
			w.setLayout(width, height)
		}
		runOnUiThread { ad.show() }
	}

	override fun onResume() {
		super.onResume()
		val window = this.window
		window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}

	override fun onPause() {
		super.onPause()
		val window = this.window
		window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
	}


	private fun saveApk(apk: File) {
		apk.saveToDownloadsFolder(
			contentResolver,
			"Spotify(SpotifyAutoPatcher)-" + Calendar.getInstance().timeInMillis + ".apk"
		)
			.onFailure {
				showAlertDialog(
					getString(R.string.could_not_save_apk),
					positiveButtonText = getString(R.string.ok),
					positiveButtonAction = {
						// empty
					},
				)
			}
			.onSuccess {
				val downloadsDirectory = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_DOWNLOADS
				)
				showAlertDialog(
					getString(R.string.apk_saved) + " : ${downloadsDirectory.path}",
					positiveButtonText = getString(R.string.ok),
					positiveButtonAction = {
						// empty
					},
				)
			}
	}

	private fun restartActivity() {
		var intent = packageManager.getLaunchIntentForPackage(packageName)
		if (intent == null) {
			intent = getIntent()
			finish()
			startActivity(intent)
		} else {
			startActivity(Intent.makeRestartActivityTask(intent.component))
			Runtime.getRuntime().exit(0)
		}
	}

	private fun installAppOrShowPopUpIfAlreadyInstalled(patchedApk: File) {
		try {
			this@MainActivity.packageManager.getPackageInfo(
				PACKAGE_TO_PATCH, 0
			).applicationInfo!!.sourceDir
			showAlertDialog(
				getString(R.string.spotify_detected_before_install),
				positiveButtonText = getString(R.string.ok),
				positiveButtonAction = {
					// empty
				},
				neutralButtonText = getString(R.string.install_anyway_not_recommended),
				neutralButtonAction = {
					AppInstaller.installApp(this, patchedApk)
				}
			)
		} catch (exception: NameNotFoundException) {
			AppInstaller.installApp(this, patchedApk)
		}
	}

	private var patchedApk: File? = null

	private fun copyText(text: CharSequence) {
		(getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
			ClipData.newPlainText(
				"log",
				text
			)
		)
		Toast.makeText(
			this,
			this.getString(R.string.copied_log), Toast.LENGTH_SHORT
		).show()
	}

	private fun showError(error: Throwable) {
		when {
			error.isNetworkException() -> {
				showAlertDialog(
					getString(R.string.network_unavailable_error),
					positiveButtonText = getString(R.string.retry),
					positiveButtonAction = {
						restartActivity()
					},
				)
			}

			error is NameNotFoundException -> {
				showAlertDialog(
					getString(R.string.app_not_found_error),
					positiveButtonText = getString(R.string.retry),
					positiveButtonAction = {
						restartActivity()
					},
				)
			}

			error is ZipException -> {
				showAlertDialog(
					getString(R.string.zip_exception_error),
					positiveButtonText = getString(R.string.fix),
					positiveButtonAction = {
						(getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
					},
					neutralButtonText = getString(R.string.retry),
					neutralButtonAction = {
						restartActivity()
					}
				)
			}

			error !is ClosedByInterruptException -> {
				val mainErr = error.toString()

				val stackTrace = StringBuilder(mainErr)

				for (line in error.stackTrace) stackTrace.append(line).append('\n')
				val fullLog = StringBuilder(stackTrace).append('\n')
					.append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
					.append(this.getString(R.string.app_name)).append(' ')
				val currentVer = packageManager.getPackageInfo(packageName, 0).versionName
				fullLog.append(currentVer).append('\n').append("Storage permission granted: ")
					.append('\n').append(logField!!.text)

				runOnUiThread {
					val dialogView = layoutInflater.inflate(
						R.layout.dialog_button_layout,
						null
					)
					(dialogView.findViewById<View>(R.id.errorD) as TextView).text =
						stackTrace

					styleAlertDialog(
						MaterialAlertDialogBuilder(this)
							.setTitle(mainErr)
							.setCancelable(false)
							.setView(dialogView)
							.setPositiveButton(
								this.getString(R.string.copy_log)
							) { dialog: DialogInterface, _: Int ->
								copyText(fullLog)
								dialog.dismiss()
							}
							.setNegativeButton(
								this.getString(R.string.create_issue)
							) { dialog: DialogInterface, _: Int ->
								startActivity(
									Intent(
										Intent.ACTION_VIEW,
										"https://github.com/corentin-c/SpotifyAutoPatcher/issues/new?title=Crash%20Report&body=$fullLog".toUri()
									)
								)
								dialog.dismiss()
							}
							.setNeutralButton(
								this.getString(R.string.cancel)
							) { dialog: DialogInterface, _: Int -> dialog.dismiss() }
							.create())
					val scrollView =
						dialogView.findViewById<ScrollView>(R.id.errorView)

					val params = scrollView.layoutParams
					params.height =
						(this.resources.displayMetrics.heightPixels * 0.5).toInt()
					scrollView.layoutParams = params
				}
			}
		}
	}
}