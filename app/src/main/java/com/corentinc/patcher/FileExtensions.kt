package com.corentinc.patcher

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.reandroid.apkeditor.merge.LogUtil
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun File.copyUriToFile(context: Context, uri: Uri) {
	context.contentResolver.openInputStream(uri).use { inputStream ->
		FileOutputStream(this).use { outputStream ->
			inputStream?.copyTo(outputStream) ?: throw Exception("Couldn't copy apk")
		}
	}
}

fun File.fromInputStream(inputStream: InputStream) {
	inputStream.use { stream ->
		FileOutputStream(this).use { outputStream ->
			stream.copyTo(outputStream)
		}
	}
}

fun File.saveToDownloadsFolder(contentResolver: ContentResolver, fileName: String) = runCatching {
	val downloadsDirectory = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_DOWNLOADS
	)

	if (!downloadsDirectory.exists()) {
		downloadsDirectory.mkdirs()
	}
	// Android 10 and above
	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
		val contentValues = ContentValues()
		contentValues.put(
			MediaStore.Downloads.DISPLAY_NAME,
			fileName
		)
		contentValues.put(
			MediaStore.Downloads.MIME_TYPE,
			"application/vnd.android.package-archive"
		)
		contentValues.put(
			MediaStore.Downloads.RELATIVE_PATH,
			Environment.DIRECTORY_DOWNLOADS
		)

		val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
		val itemUri: Uri? = contentResolver.insert(contentUri, contentValues)
		itemUri?.let {
			contentResolver.openOutputStream(it).use { outputStream ->
				outputStream?.let {
					this.inputStream().use { stream ->
						stream.copyTo(outputStream)
					}
				} ?: run {
					val message = "Couldn't save apk, output stream is null"
					LogUtil.logMessage(message)
					throw Exception(message)
				}
			}
		} ?: run {
			val message = "Couldn't save apk, item uri is null"
			LogUtil.logMessage(message)
			throw Exception(message)
		}
	} else {
		// Android 9 and below
		val savedFile = File(
			downloadsDirectory,
			fileName
		)
		this.inputStream().use { stream ->
			savedFile.outputStream().use { outputStream ->
				stream.copyTo(outputStream)
			}
		}
	}
}

fun File.clearDirectory() {
	this.listFiles()?.forEach {
		it.deleteRecursively()
	}
}