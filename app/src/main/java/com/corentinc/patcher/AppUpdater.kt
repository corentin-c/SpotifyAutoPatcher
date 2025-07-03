package com.corentinc.patcher

import com.github.corentinc.SpotifyAutoPatcher.BuildConfig

object AppUpdater {
	fun promptUpdateIfNeeded() {
		if (isUpdateAvailable()) {

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
		val string =
			UrlDownloader.downloadStringFromUrl("https://github.com/corentin-c/SpotifyAutoPatcher/releases/latest")
		val versionNumberRegex = Regex("<title>Release\\s+([0-9]+(?:\\.[0-9]+)+)")
		val match = versionNumberRegex.find(string)
		return match?.groupValues?.getOrNull(1)
	}
}