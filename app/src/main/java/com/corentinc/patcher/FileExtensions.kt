package com.corentinc.patcher

import android.content.Context
import android.net.Uri
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