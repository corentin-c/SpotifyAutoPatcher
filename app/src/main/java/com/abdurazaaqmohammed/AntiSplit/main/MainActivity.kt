package com.abdurazaaqmohammed.AntiSplit.main

import android.Manifest
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.support.v4.content.FileProvider
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.corentinc.patcher.ReVancedPatcher.patch
import com.corentinc.patcher.clearDirectory
import com.corentinc.patcher.copyUriToFile
import com.corentinc.patcher.isNetworkException
import com.corentinc.patcher.saveToDownloadsFolder
import com.github.corentinc.SpotifyAutoPatcher.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reandroid.apk.ApkBundle
import com.reandroid.apkeditor.merge.LogUtil
import com.reandroid.apkeditor.merge.Merger
import com.reandroid.apkeditor.merge.Merger.LogListener
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.nio.channels.ClosedByInterruptException
import java.util.Calendar
import java.util.zip.ZipException
import kotlin.io.path.createDirectory

const val PACKAGE_TO_PATCH = "com.spotify.music"
private const val TEMP_FOLDER = "temp"
private const val FILE_PROVIDER_NAME = "com.github.corentinc.SpotifyAutoPatcher.provider"

class MainActivity : AppCompatActivity(), LogListener {
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
			installAppOrShowPopUpIfAlreadyInstalled(patchedApk)
		}


	private var logField: TextView? = null
	private var scrollView: NestedScrollView? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
				this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			) != PackageManager.PERMISSION_GRANTED
		) {
			requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
		}
		defaultFolder = File(cacheDir, TEMP_FOLDER)
		if (!defaultFolder.exists()) defaultFolder.toPath().createDirectory()
		handler = Handler(Looper.getMainLooper())
		defaultFolder.clearDirectory()
		WindowCompat.setDecorFitsSystemWindows(window, false)
		setContentView(R.layout.activity_main)
		scrollView = findViewById(R.id.scrollView)
		logField = findViewById(R.id.logField)
		LogUtil.setLogListener(this)
		LogUtil.logEnabled = true
		showAlertDialog(
			getString(R.string.before_start_message),
			positiveButtonText = getString(R.string.start),
			positiveButtonAction = {
				mergeAndPatchApk()
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
			val background: Drawable = findViewById<FrameLayout>(R.id.main).background
			border.setColor((background as ColorDrawable).color) // Background color
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

	override fun onLog(msg: CharSequence) {
		onLog(msg.toString())
	}

	override fun onLog(log: String) {
		Log.i("", log)
		runOnUiThread {
			logField!!.append(StringBuilder(log).append('\n'))
			scrollView!!.post { scrollView!!.fullScroll(View.FOCUS_DOWN) }
		}
	}

	override fun onLog(resID: Int) {
		onLog(this.getString(resID))
	}

	var handler: Handler? = null
		private set

	private fun mergeAndPatchApk() {
		onLog("Merging APK...")
		findViewById<View>(R.id.installButton).visibility =
			View.GONE
		val fabs = findViewById<LinearLayout>(R.id.fabs)
		fabs.alpha = 0.5f
		val cancelButton = findViewById<View>(R.id.cancelButton)
		cancelButton.visibility = View.VISIBLE
		cancelButton.setOnClickListener {
			restartActivity()
		}

		val copyButton = findViewById<View>(R.id.copyButton)
		copyButton.visibility = View.VISIBLE
		copyButton.setOnClickListener {
			copyText(
				StringBuilder().append(
					logField!!.text
				)
			)
		}
		val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
			showError(e)
		}
		lifecycleScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
			try {
				val bundle = ApkBundle()
				bundle.loadApkDirectory(
					File(
						packageManager.getPackageInfo(
							PACKAGE_TO_PATCH, 0
						).applicationInfo!!.sourceDir
					).parentFile, false, this@MainActivity
				)
				val uri = File(defaultFolder, "mergingResult.apk").toUri()
				Merger.run(bundle, defaultFolder, uri, this@MainActivity)
				val apk = File(defaultFolder, "unpatched.apk")
				apk.copyUriToFile(this@MainActivity, uri)
				startPatching(apk)
			} catch (exception: Exception) {
				showError(exception)
			}
		}
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
					installApp(patchedApk)
				}
			)
		} catch (exception: NameNotFoundException) {
			installApp(patchedApk)
		}
	}

	private fun installApp(patchedApk: File) {
		startActivity(
			Intent(Intent.ACTION_INSTALL_PACKAGE)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.setData(
					FileProvider.getUriForFile(
						applicationContext,
						FILE_PROVIDER_NAME,
						patchedApk
					)
				)
		)
	}

	private fun uninstallApp() {
		val uri = "package:$PACKAGE_TO_PATCH".toUri()
		uninstallCallback.launch(
			Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		)
	}

	private lateinit var patchedApk: File
	private suspend fun startPatching(file: File) {
		onLog(getString(R.string.merging_apk_succeeded))

		val installButton = findViewById<View>(R.id.installButton)
		val success = this.getString(R.string.success_saved)
		LogUtil.logMessage(success)
		patchedApk = patch(
			applicationContext, file, defaultFolder,
			this@MainActivity
		)
		runOnUiThread {
			installButton.setOnClickListener {
				installAppOrShowPopUpIfAlreadyInstalled(patchedApk)
			}
			installButton.visibility = View.VISIBLE
			onLog(getString(R.string.ready_to_install))
			findViewById<View>(R.id.cancelButton).visibility =
				View.GONE

			val downloadButton = findViewById<View>(R.id.downloadButton)
			downloadButton.setOnClickListener {
				saveApk(patchedApk)
			}
			downloadButton.visibility = View.VISIBLE

			showAlertDialog(
				getString(R.string.ready_to_install),
				positiveButtonText = getString(R.string.next),
				positiveButtonAction = {
					uninstallApp()
				},
			)
		}
	}

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

				handler!!.post {
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
}