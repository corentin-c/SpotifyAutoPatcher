package com.corentinc.patcher

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL

object UrlDownloader {
	fun downloadStringFromUrl(url: String): String {
		val urlConnection = URL(url).openConnection()
		urlConnection.connectTimeout = 4000
		return readInputStream(urlConnection.getInputStream())
	}

	fun downloadFileFromUrl(url: String, tmpDirectory: File): File {
		val urlConnection = URL(url).openConnection()
		urlConnection.connectTimeout = 4000
		val file = File(tmpDirectory, "downloadedFile.apk")
		file.fromInputStream(urlConnection.getInputStream())
		return file
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
}