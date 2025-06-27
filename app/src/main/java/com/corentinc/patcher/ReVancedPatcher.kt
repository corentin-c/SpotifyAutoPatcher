package com.corentinc.patcher

import android.content.Context
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.loadPatchesFromDex
import com.abdurazaaqmohammed.AntiSplit.main.PACKAGE_TO_PATCH
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reandroid.apkeditor.merge.Merger.LogListener
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

object ReVancedPatcher {
	suspend fun patch(context: Context, apk: File, tmpDirectory: File, logListener: LogListener): File {
		logListener.onLog("Getting patches...")
		val patchesFile = getPatches(tmpDirectory)
		val patches =
			loadPatchesFromDex(setOf(patchesFile), optimizedDexDirectory = tmpDirectory)
		logListener.onLog("Filtering patches...")
		val spotifyPatches = patches.filter { patch ->
			patch.compatiblePackages?.any { it.first == PACKAGE_TO_PATCH } ?: false
		}
		logListener.onLog("${spotifyPatches.size} patches to apply")
		var numberOfPatchesExecuted = 0
		logListener.onLog("Applying patches...")
		val patcherResult =
			Patcher(
				PatcherConfig(
					apkFile = apk,
					aaptBinaryPath = Aapt.binary(context).absolutePath,
					temporaryFilesPath = File(tmpDirectory, "temporaryFiles"),
					frameworkFileDirectory = tmpDirectory.path
				)
			).use { patcher ->
				patcher += spotifyPatches.toSet()
				patcher().collect { patchResult ->
					numberOfPatchesExecuted++
					if (patchResult.exception != null)
						logListener.onLog("\"${patchResult.patch}\" failed:\n${patchResult.exception} ($numberOfPatchesExecuted/${spotifyPatches.size})")
					else
						logListener.onLog("\"${patchResult.patch}\" succeeded ($numberOfPatchesExecuted/${spotifyPatches.size})")
					if (numberOfPatchesExecuted == spotifyPatches.size) {
						logListener.onLog("Rebuilding...")
					}
				}

				// Compile and save the patched APK file components.
				patcher.get()
			}
		logListener.onLog("Patching succeeded !")
		logListener.onLog("Creating APK...")
		patcherResult.applyTo(apk)
		logListener.onLog("APK creation succeeded !")
		val signedPatchedApk = File(tmpDirectory, "signedPatched.apk")
		logListener.onLog("Signing APK...")
		ApkUtils.signApk(
			apk,
			signedPatchedApk,
			"autoSpotify",
			ApkUtils.KeyStoreDetails(
				File(tmpDirectory, "autoSpotify.keystore"),
				"autoSpotify.keystore.password",
				"autoSpotify.key.alias",
				"autoSpotify.key.password"
			)
		)
		logListener.onLog("APK signing succeeded !")
		return signedPatchedApk
	}

	private fun readInputStream(inputStream: InputStream): String {
		val bufferedReader = BufferedReader(InputStreamReader(inputStream))
		var totalString = ""
		var line: String?
		while (bufferedReader.readLine().also { line = it } != null) {
			totalString += "$line\n"
		}
		return totalString
	}

	private fun getPatches(tmpDirectory: File): File {
		val patchesInfoURL = URL("https://api.revanced.app/v4/patches")
		val urlConnection = patchesInfoURL.openConnection()
		urlConnection.connectTimeout = 4000
		val string = readInputStream(urlConnection.getInputStream())
		val itemType = object : TypeToken<ReVancedPatchesInfo>() {
			// empty
		}.type
		val patchesInfo = Gson().fromJson<ReVancedPatchesInfo>(string, itemType)
		val patchesUrl = URL(patchesInfo.download_url)
		val patchesUrlConnection = patchesUrl.openConnection()
		patchesUrlConnection.connectTimeout = 4000
		val patchesFile = File(tmpDirectory, "patches.rvp")
		patchesFile.fromInputStream(patchesUrlConnection.getInputStream())
		return patchesFile
	}

}