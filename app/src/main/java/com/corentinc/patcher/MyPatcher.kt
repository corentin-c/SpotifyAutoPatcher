package com.corentinc.patcher

import android.content.Context
import android.util.Log
import app.revanced.patcher.patch.loadPatchesFromJar
import com.abdurazaaqmohammed.AntiSplit.R
import com.abdurazaaqmohammed.AntiSplit.main.MainActivity
import java.io.File
import java.io.FileOutputStream


object CPatcher {
	fun patch(activity: MainActivity) {
		val file = File(activity.cacheDir, "patches.rvp")
		file.setReadOnly()
		copyRawResourceToFile(activity, R.raw.patches, file)
		val patches = loadPatchesFromJar(setOf(file))
		patches.forEach {
			Log.i("KAPPA ", "$it.name")
		}
//		val patcherResult = Patcher(PatcherConfig(apkFile = File("some.apk"))).use { patcher ->
//			// Here you can access metadata about the APK file through patcher.context.packageMetadata
//			// such as package name, version code, version name, etc.
//
//			// Add patches.
//			patcher += patches
//
//			// Execute the patches.
//			runBlocking {
//				patcher().collect { patchResult ->
//					if (patchResult.exception != null)
//						Log.i("", "\"${patchResult.patch}\" failed:\n${patchResult.exception}")
//					else
//						Log.i("", "\"${patchResult.patch}\" succeeded")
//				}
//			}
//
//			// Compile and save the patched APK file components.
//			patcher.get()
//		}
//
//// The result of the patcher contains the modified components of the APK file that can be repackaged into a new APK file.
//		val dexFiles = patcherResult.dexFiles
//		val resources = patcherResult.resources
	}

	fun copyRawResourceToFile(context: Context, rawResId: Int, outputFile: File) {
		context.resources.openRawResource(rawResId).use { inputStream ->
			FileOutputStream(outputFile).use { outputStream ->
				inputStream.copyTo(outputStream)
			}
		}
	}

}