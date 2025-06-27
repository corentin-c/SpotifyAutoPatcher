package com.abdurazaaqmohammed.AntiSplit.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
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
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.corentinc.patcher.ReVancedPatcher.patch
import com.corentinc.patcher.copyUriToFile
import com.corentinc.patcher.isNetworkException
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
import java.util.Objects

const val PACKAGE_TO_PATCH = "com.spotify.music"

class MainActivity : AppCompatActivity(), LogListener {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		handler = Handler(Looper.getMainLooper())
		deleteDir(cacheDir)
		deleteDir(codeCacheDir)
		deleteDir(filesDir)
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
				process()
			},
		)

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

	private var logField: TextView? = null
	private var scrollView: NestedScrollView? = null

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

	private fun process() {
		onLog("Merging APK...")
		findViewById<View>(R.id.installButton).visibility =
			View.GONE
		val fabs = findViewById<LinearLayout>(R.id.fabs)
		fabs.alpha = 0.5f
		val cancelButton = findViewById<View>(R.id.cancelButton)
		cancelButton.visibility = View.VISIBLE
		cancelButton.setOnClickListener {
			cancel()
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
				val uri = File(cacheDir, "temp.apk").toUri()
				Merger.run(bundle, cacheDir, uri, this@MainActivity)
				// don't know why I can't reuse the same file but it doesn't work otherwise
				val apk = File(cacheDir, "unpatched.apk")
				apk.copyUriToFile(this@MainActivity, uri)
				startPatching(apk)
			} catch (exception: Exception) {
				showError(exception)
			}
		}
	}

	private fun cancel() {
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
						"com.github.corentinc.SpotifyAutoPatcher.provider",
						patchedApk
					)
				)
		)
	}

	private val uninstallCallback =
		registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
			installAppOrShowPopUpIfAlreadyInstalled(patchedApk)
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
		onLog("Merging APK succeeded !")

		val installButton = findViewById<View>(R.id.installButton)
		if (errorOccurred) installButton.visibility = View.GONE
		else {
			val success = this.getString(R.string.success_saved)
			LogUtil.logMessage(success)
				patchedApk = patch(
					applicationContext, file,
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
					showAlertDialog(
						getString(R.string.ready_to_install),
						positiveButtonText = getString(R.string.next),
						positiveButtonAction = {
							uninstallApp()
						},
					)

				}
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
						cancel()
					},
				)
			}

			error is NameNotFoundException -> {
				showAlertDialog(
					getString(R.string.app_not_found_error),
					positiveButtonText = getString(R.string.retry),
					positiveButtonAction = {
						cancel()
					},
				)
			}

			error !is ClosedByInterruptException -> {
				val mainErr = error.toString()
				errorOccurred = mainErr != this.getString(R.string.sign_failed)

				val stackTrace = StringBuilder(mainErr)

				for (line in error.stackTrace) stackTrace.append(line).append('\n')
				val fullLog = StringBuilder(stackTrace).append('\n')
					.append("SDK ").append(Build.VERSION.SDK_INT).append('\n')
					.append(this.getString(R.string.app_name)).append(' ')
				val currentVer = try {
					packageManager.getPackageInfo(packageName, 0).versionName
				} catch (ex: Exception) {
					"2.1.1"
				}
				fullLog.append(currentVer).append('\n').append("Storage permission granted: ")
					.append(
						!doesNotHaveStoragePerm(
							this
						)
					)
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

	companion object {
		var errorOccurred: Boolean = false
		var lang: String? = null

		@JvmStatic
		fun doesNotHaveStoragePerm(context: Context): Boolean {
			return (if (LegacyUtils.supportsWriteExternalStorage) context.checkSelfPermission(
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			) == PackageManager.PERMISSION_DENIED else !Environment.isExternalStorageManager())
		}

		fun deleteDir(dir: File) {
			// There should never be folders in here.
			for (child in dir.list()!!) File(dir, child).delete()
		}

		@JvmStatic
		fun getOriginalFileName(context: Context, uri: Uri): String {
			var result: String? = null
			try {
				if (uri.scheme == "content") {
					context.contentResolver.query(uri, null, null, null, null).use { cursor ->
						if (cursor != null && cursor.moveToFirst()) {
							result =
								cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
						}
					}
				}
				if (result == null) {
					result = uri.path
					val cut = Objects.requireNonNull<String?>(result)
						.lastIndexOf('/') // Ensure it throw the NullPointerException here to be caught
					if (cut != -1) result = result!!.substring(cut + 1)
				}
				LogUtil.logMessage(result)
				val suffix = "_antisplit"
				return result!!.replaceFirst("\\.(?:xapk|aspk|apk[sm])".toRegex(), "$suffix.apk")
			} catch (ignored: Exception) {
				return "filename_not_found"
			}
		}
	}
}