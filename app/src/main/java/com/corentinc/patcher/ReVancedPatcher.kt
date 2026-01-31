package com.corentinc.patcher

import android.content.Context
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.Patcher
import app.revanced.patcher.PatcherConfig
import app.revanced.patcher.patch.loadPatchesFromDex

import com.github.corentinc.SpotifyAutoPatcher.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reandroid.apkeditor.merge.LogUtil.logMessage
import com.reandroid.apkeditor.merge.Merger.LogListener
import java.io.File

object ReVancedPatcher {
    suspend fun patch(
        context: Context,
        apk: File,
        tmpDirectory: File,
        logListener: LogListener,
        packageName: String
    ): File {
        logListener.onLog("Getting patches...")
        val patchesFile = getPatches(context, tmpDirectory)
        val patches =
            loadPatchesFromDex(setOf(patchesFile), optimizedDexDirectory = tmpDirectory)
        logListener.onLog("Filtering patches...")
        var spotifyPatches = patches.filter { patch ->
            patch.compatiblePackages?.any { it.first == packageName } ?: false
        }
        spotifyPatches = spotifyPatches + patches.find { it.name == "Change package name" }!!
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


    private fun getPatches(context: Context, tmpDirectory: File): File {
        val string = UrlDownloader.downloadStringFromUrl("https://api.revanced.app/v4/patches")
        val itemType = object : TypeToken<ReVancedPatchesInfo>() {
            // empty
        }.type
        val patchesInfo = Gson().fromJson<ReVancedPatchesInfo>(string, itemType)
        logMessage(
            context.getString(
                R.string.download_revanced_patches_version,
                patchesInfo.version
            )
        )
        return UrlDownloader.downloadFileFromUrl(patchesInfo.download_url, tmpDirectory)
    }

}