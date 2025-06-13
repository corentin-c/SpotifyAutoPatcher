package com.abdurazaaqmohammed.AntiSplit.main

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.Color
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
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.corentinc.patcher.ReVancedPatcher.patch
import com.github.corentinc.SpotifyAutoPatcher.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.reandroid.apk.ApkBundle
import com.reandroid.apkeditor.merge.LogUtil
import com.reandroid.apkeditor.merge.Merger
import com.reandroid.apkeditor.merge.Merger.LogListener
import com.reandroid.apkeditor.merge.Merger.signedApk
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.UnknownHostException
import java.nio.channels.ClosedByInterruptException
import java.util.Objects

const val PACKAGE_TO_PATCH = "com.spotify.music"

class MainActivity : AppCompatActivity(), LogListener {
	private var pkgName: String? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		handler = Handler(Looper.getMainLooper())

		deleteDir(cacheDir)
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
				pkgName = PACKAGE_TO_PATCH
				val file = File(applicationContext.cacheDir.path + "unpatched.apk")
				val uri = Uri.fromFile(file)
				signedApk = uri
				process(uri)
			},
		)

	}

	private fun styleAlertDialog(ad: AlertDialog) {
		val w = ad.window
		if (w != null) {
			val border = GradientDrawable()
			border.setColor(if (Companion.theme == com.google.android.material.R.style.Theme_Material3_Light_NoActionBar) Color.WHITE else Color.BLACK) // Background color
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

	override fun onDestroy() {
		deleteDir(cacheDir)
		super.onDestroy()
	}

	private fun process(outputUri: Uri) {
		onLog("Merging APK...")
		findViewById<View>(R.id.installButton).visibility =
			View.GONE
		val fabs = findViewById<LinearLayout>(R.id.fabs)
		fabs.alpha = 0.5f
		val cancelButton = findViewById<View>(R.id.cancelButton)
		cancelButton.visibility = View.VISIBLE
		cancelButton.setOnClickListener { v: View? ->
			cancel()
		}

		val copyButton = findViewById<View>(R.id.copyButton)
		copyButton.visibility = View.VISIBLE
		copyButton.setOnClickListener { v: View? ->
			copyText(
				StringBuilder().append(
					logField!!.text
				).append('\n')
					.append((findViewById<View>(R.id.errorField) as TextView).text)
			)
		}
		val coroutineExceptionHandler = CoroutineExceptionHandler { _, e ->
			this@MainActivity.showError(e)
		}
		lifecycleScope.launch(Dispatchers.Default + coroutineExceptionHandler) {
			try {
				val cacheDir = this@MainActivity.cacheDir
				deleteDir(cacheDir)
				val bundle = ApkBundle()
				bundle.loadApkDirectory(
					File(
						this@MainActivity.packageManager.getPackageInfo(
							pkgName!!, 0
						).applicationInfo!!.sourceDir
					).parentFile, false, this@MainActivity
				)
				Merger.run(bundle, cacheDir, outputUri, this@MainActivity, false)
				this@MainActivity.showSuccess()
			} catch (exception: Exception) {
				this@MainActivity.showError(exception)
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
	) {
		runOnUiThread {
			val builder = MaterialAlertDialogBuilder(this)
			builder.setMessage(text)
			builder.setPositiveButton(
				positiveButtonText
			) { dialog: DialogInterface, _: Int ->
				positiveButtonAction()
				dialog.dismiss()
			}

			styleAlertDialog(builder.create())
		}
	}

	private suspend fun showSuccess() {
		onLog("Merging APK succeeded !")

		val installButton = findViewById<View>(R.id.installButton)
		if (errorOccurred) installButton.visibility = View.GONE
		else {
			val success = this.getString(R.string.success_saved)
			LogUtil.logMessage(success)
			if (signedApk != null) {
				val patchedApk = patch(
					applicationContext, signedApk,
					this@MainActivity
				)
				runOnUiThread {
					installButton.setOnClickListener {
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
					installButton.visibility = View.VISIBLE
					onLog("Ready to install APK ! Uninstall existing package first before clicking on install")
					findViewById<View>(R.id.cancelButton).visibility =
						View.GONE
					showAlertDialog(
						getString(R.string.ready_to_install),
						positiveButtonText = getString(R.string.ok),
						positiveButtonAction = {
							// empty
						},
					)

				}
			} else installButton.visibility = View.GONE
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
		when (error) {
			is NameNotFoundException -> {
				showAlertDialog(
					getString(R.string.app_not_found_error),
					positiveButtonText = getString(R.string.retry),
					positiveButtonAction = {
						cancel()
					},
				)
			}

			is UnknownHostException -> {
				showAlertDialog(
					getString(R.string.network_unavailable_error),
					positiveButtonText = getString(R.string.retry),
					positiveButtonAction = {
						cancel()
					},
				)
			}

			!is ClosedByInterruptException -> {
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
				fullLog.append(currentVer).append('\n').append("Storage permission granted: ").append(
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
		var theme: Int = 0

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