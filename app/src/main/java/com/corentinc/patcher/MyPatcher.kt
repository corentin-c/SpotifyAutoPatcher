package com.corentinc.patcher

import android.content.Context
import android.net.Uri
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.loadPatchesFromDex
import com.abdurazaaqmohammed.AntiSplit.R
import com.reandroid.apkeditor.merge.Merger.LogListener
import java.io.File
import java.io.FileOutputStream

object ReVancedPatcher {
	suspend fun patch(context: Context, apk: Uri, logListener: LogListener): File {
		logListener.onLog("Getting patches...")
		val patchesFile = File(context.cacheDir, "patches.rvp")
		patchesFile.copyRawResourceToFile(context, R.raw.patches)
		val patches =
			loadPatchesFromDex(setOf(patchesFile), optimizedDexDirectory = context.codeCacheDir)
		logListener.onLog("Filtering patches...")
		val spotifyPatches = patches.filter { patch ->
			patch.compatiblePackages?.any { it.first == "com.spotify.music" } ?: false
		}
		val unpatchedApkFile = File(context.cacheDir, "spotify.apk")
		unpatchedApkFile.copyUriToFile(context, apk)
		logListener.onLog("${spotifyPatches.size} patches to apply")
		var numberOfPatchesExecuted = 0
		logListener.onLog("Applying patches...")
		val patcherResult =
			Patcher(
				PatcherConfig(
					apkFile = unpatchedApkFile,
					aaptBinaryPath = Aapt.binary(context).absolutePath,
					temporaryFilesPath = context.filesDir,
					frameworkFileDirectory = context.filesDir.path
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
		patcherResult.applyTo(unpatchedApkFile)
		logListener.onLog("APK creation succeeded !")
		val signedPatchedApk = File(context.filesDir, "signedPatched.apk")
		logListener.onLog("Signing APK...")
		ApkUtils.signApk(
			unpatchedApkFile,
			signedPatchedApk,
			"autoSpotify",
			ApkUtils.KeyStoreDetails(
				File(context.cacheDir, "autoSpotify.keystore"),
				"autoSpotify.keystore.password",
				"autoSpotify.key.alias",
				"autoSpotify.key.password"
			)
		)
		logListener.onLog("APK signing succeeded !")
		return signedPatchedApk
	}

	private fun File.copyRawResourceToFile(context: Context, rawResId: Int) {
		context.resources.openRawResource(rawResId).use { inputStream ->
			FileOutputStream(this).use { outputStream ->
				inputStream.copyTo(outputStream)
			}
		}
	}

	private fun File.copyUriToFile(context: Context, uri: Uri) {
		context.contentResolver.openInputStream(uri).use { inputStream ->
			FileOutputStream(this).use { outputStream ->
				inputStream?.copyTo(outputStream) ?: throw Exception("Couldn't copy apk")
			}
		}
	}

}