package com.corentinc.patcher

import android.content.Context
import app.revanced.library.ApkUtils
import app.revanced.library.ApkUtils.applyTo
import app.revanced.patcher.patch.loadPatches
import app.revanced.patcher.patcher
import com.github.corentinc.SpotifyAutoPatcher.R
import com.google.gson.Gson
import com.reandroid.apkeditor.merge.LogUtil.logMessage
import com.reandroid.apkeditor.merge.Merger.LogListener
import java.io.File

object ReVancedPatcher {
    fun patchApk(
        context: Context,
        apk: File,
        tmpDirectory: File,
        logListener: LogListener,
        applicationToPatch: ApplicationSupported
    ): File {
        logListener.onLog("Getting patches...")
        val patchesFile = getPatches(context, tmpDirectory)
        val patches =
            loadPatches(
                onFailedToLoad = { _, throwable ->
                    logListener.onLog("ERROR : Failed to load patches ! : $throwable")
                },
                patchesFiles = arrayOf(patchesFile)
            )
        
        logListener.onLog("Filtering patches...")
        var filteredPatches = patches.filter { patch ->
            patch.compatiblePackages?.any { it.first == applicationToPatch.packageName } ?: false
        }
        
        if (applicationToPatch.requireChangePackageNamePatch) {
            filteredPatches = filteredPatches + patches.find { it.name == "Change package name" }!!
        }
        
        logListener.onLog("${filteredPatches.size} patches to apply")
        var numberOfPatchesExecuted = 0
        logListener.onLog("Applying patches...")
        
        val revancedTmpFile = File(tmpDirectory, "revanced-patcher")
        revancedTmpFile.mkdir()
        
        val patcher = patcher(
            apkFile = apk,
            temporaryFilesPath = revancedTmpFile,
            aaptBinaryPath = Aapt.binary(context),
            frameworkFileDirectory = revancedTmpFile.absolutePath,
            getPatches = { _, _ ->
                filteredPatches.toSet()
            }
        )
        
        val patcherResult = patcher { patchResult ->
            numberOfPatchesExecuted++
            if (patchResult.exception != null)
                logListener.onLog("\"${patchResult.patch}\" failed:\n${patchResult.exception} ($numberOfPatchesExecuted/${filteredPatches.size})")
            else
                logListener.onLog("\"${patchResult.patch}\" succeeded ($numberOfPatchesExecuted/${filteredPatches.size})")
            if (numberOfPatchesExecuted == filteredPatches.size) {
                logListener.onLog("Rebuilding...")
            }
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
        val string = UrlDownloader.downloadStringFromUrl("https://api.revanced.app/v5/patches")
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