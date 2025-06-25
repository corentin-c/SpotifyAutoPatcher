package com.corentinc.patcher

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

private val NETWORK_ERROR_EXCEPTIONS_LIST = listOf(
	UnknownHostException::class.java,
	SocketTimeoutException::class.java,
	ConnectException::class.java,
	SSLHandshakeException::class.java
)

fun Throwable.isNetworkException(): Boolean {
	return this::class.java in NETWORK_ERROR_EXCEPTIONS_LIST || (this.cause != null && this.cause!!.isNetworkException())
}