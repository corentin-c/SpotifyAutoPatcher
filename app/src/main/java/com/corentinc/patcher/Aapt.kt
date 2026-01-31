package com.corentinc.patcher

import android.content.Context
import java.io.File

object Aapt {
	fun binary(context: Context): File {
		return File(context.applicationInfo.nativeLibraryDir).resolveAapt()
	}
}

private fun File.resolveAapt(): File {
    val file = resolve(list { _, f -> !File(f).isDirectory && f.contains("aapt") }!!.first())
    println(file.path)
    return file
}