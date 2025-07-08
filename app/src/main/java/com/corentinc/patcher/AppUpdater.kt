package com.corentinc.patcher

import android.content.Context
import com.github.corentinc.SpotifyAutoPatcher.BuildConfig
import com.github.corentinc.SpotifyAutoPatcher.R
import com.reandroid.apkeditor.merge.LogUtil
import java.io.File

object AppUpdater {
	private var latestVersionName: String? = null

	fun checkIfAnUpdateIsAvailable(
		context: Context,
		tmpDirectory: File,
		onUpdateAvailable: (latestVersionApk: File) -> Unit,
		onUpdateNotAvailable: () -> Unit,
	) =
		runCatching {
			if (isUpdateAvailable()) {
				LogUtil.logMessage(context.getString(R.string.update_available))
				LogUtil.logMessage(context.getString(R.string.downloading_latest_version) + " v${getLatestVersionName()}")
				val latestVersionApk = downloadLatestVersion(tmpDirectory)
				onUpdateAvailable(latestVersionApk)
			} else {
				onUpdateNotAvailable()
			}
		}

	private fun isUpdateAvailable(): Boolean {
		getLatestVersionName()?.let { latestVersionName ->
			val latestVersionNameInNumber = latestVersionName.replace(".", "").toInt()
			val currentVersionName = BuildConfig.VERSION_NAME
			val currentVersionNameInNumber = currentVersionName.replace(".", "").toInt()
			return currentVersionNameInNumber < latestVersionNameInNumber
		} ?: return false
	}

	private fun getLatestVersionName(): String? {
		if (latestVersionName != null) {
			return latestVersionName
		} else {
			val string =
				UrlDownloader.downloadStringFromUrl("https://github.com/corentin-c/SpotifyAutoPatcher/releases/latest")
			val versionNumberRegex = Regex("<title>Release\\s+([0-9]+(?:\\.[0-9]+)+)")
			val match = versionNumberRegex.find(string)
			val versionName = match?.groups?.get(1)?.value
			latestVersionName = versionName
			return versionName
		}
	}

	private fun downloadLatestVersion(tmpDirectory: File): File {
		val latestVersion = getLatestVersionName()
		val url =
			"https://github.com/corentin-c/SpotifyAutoPatcher/releases/latest/download/SpotifyAutoPatcher-$latestVersion.apk"
		return UrlDownloader.downloadFileFromUrl(url, tmpDirectory)
	}
}