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
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

object CPatcher {
	fun patch(context: Context, apk: Uri, logListener: LogListener): File {
		val patchesFile = File(context.cacheDir, "patches.rvp")
		patchesFile.copyRawResourceToFile(context, R.raw.patches)
		val patches =
			loadPatchesFromDex(setOf(patchesFile), optimizedDexDirectory = context.codeCacheDir)
		val spotifyPatches = patches.filter { patch ->
			patch.compatiblePackages?.any { it.first == "com.spotify.music" } ?: false
		}
		val unpatchedApkFile = File(context.cacheDir, "spotify.apk")
		unpatchedApkFile.copyUriToFile(context, apk)
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
				runBlocking {
					patcher().collect { patchResult ->
						if (patchResult.exception != null)
							logListener.onLog("\"${patchResult.patch}\" failed:\n${patchResult.exception}")
						else
							logListener.onLog("\"${patchResult.patch}\" succeeded")
					}
				}

				// Compile and save the patched APK file components.
				patcher.get()
			}
		logListener.onLog("FINISHED PATCHING !!")
		patcherResult.applyTo(unpatchedApkFile)
		val signedPatchedApk = File(context.filesDir, "signedPatched.apk")
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


		logListener.onLog("FINISHED SIGNING !!")
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