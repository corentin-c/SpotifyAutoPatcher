package com.corentinc.patcher

import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import androidx.activity.result.ActivityResultLauncher
import androidx.core.net.toUri
import com.abdurazaaqmohammed.AntiSplit.main.PACKAGE_TO_PATCH
import java.io.File

private const val FILE_PROVIDER_NAME = "com.github.corentinc.SpotifyAutoPatcher.provider"

object AppInstaller {
	fun installApp(context: Context, patchedApk: File) {
		context.startActivity(
			Intent(Intent.ACTION_INSTALL_PACKAGE)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
				.setData(
					FileProvider.getUriForFile(
						context,
						FILE_PROVIDER_NAME,
						patchedApk
					)
				)
		)
	}

	fun uninstallApp(uninstallCallback: ActivityResultLauncher<Intent>) {
		val uri = "package:$PACKAGE_TO_PATCH".toUri()
		uninstallCallback.launch(
			Intent(Intent.ACTION_UNINSTALL_PACKAGE, uri)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
		)
	}
}