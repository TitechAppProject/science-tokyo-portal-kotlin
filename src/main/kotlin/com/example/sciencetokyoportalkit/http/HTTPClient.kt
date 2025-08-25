package com.example.sciencetokyoportalkit.http

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.collections.flatMap

interface HTTPClient {
    suspend fun send(request: HTTPRequest): HTTPResponse
    fun cookies(): List<HttpCookie>
}

data class HTTPResponse(
    val html: String,
    val statusCode: Int,
    val responseUrl: String?
)

class HTTPClientImpl(
    private val userAgent: String
) : HTTPClient {
    private var cookies = mutableSetOf<HttpCookie>()

    override suspend fun send(request: HTTPRequest): HTTPResponse = withContext(Dispatchers.IO) {
        val url = URL(request.url)
        var connection = generateUrlConnection(url, request.method.value, request.headerFields, cookies)

        do {
            println("RequestURL: " + connection.url.toString())
            println("Method: " + connection.requestMethod.toString())
            println("RequestHeaders: " + connection.requestProperties.toString())
            println("RequestBody: " + request.body.toString())
            connection.setDoOutput(true)
            connection.connect()

            if (request.body != null && connection.requestMethod == "POST") {
                val outputStream = connection.outputStream
                val ps = PrintStream(outputStream)
                val bodyString = request.body!!.map {
                    it.key + "=" + URLEncoder.encode(it.value, "utf-8")
                }.joinToString("&")
                ps.print(bodyString)
                ps.close()
                outputStream.close()
            }

            println("responseHeaders: " + connection.headerFields.toString())
            println("responseCode: " + connection.responseCode.toString())

            val setCookie = connection.headerFields["Set-Cookie"] ?: connection.headerFields["Set-cookie"]
            setCookie
                ?.flatMap {
                    HttpCookie.parse(it)
                }?.filter {
                    !it.hasExpired()
                }?.forEach { cookie ->
                    // FIXME: n^2
                    if (cookie.domain == null) {
                        cookie.domain = connection.url.host
                    }
                    val sameNameCookie = cookies.firstOrNull { it.name == cookie.name }

                    if (sameNameCookie != null) {
                        cookies.remove(sameNameCookie)
                    }
                    cookies.add(cookie)
                }

            var needRedirect = false
            if (connection.responseCode in 300..399) {
                val location = connection.getHeaderField("Location") ?: connection.getHeaderField("location")
                try {
                    val locationURL = when {
                        location.startsWith("?") -> URL(url.protocol + "://" + url.host + url.path + location)
                        location.startsWith("/") -> URL(url.protocol + "://" + url.host + location)
                        else -> URL(location)
                    }
                    connection =
                        generateUrlConnection(locationURL, "GET", request.headerFields, cookies)
                    needRedirect = true
                } catch (e: Exception) {
                    needRedirect = false
                }
            }
        } while (needRedirect)

        try {
            val br = BufferedReader(InputStreamReader(connection.inputStream))

            val sb = StringBuilder()

            for (line in br.readLines()) {
                sb.append(line)
            }

            br.close()

            connection.inputStream.close()

            HTTPResponse(
                sb.toString(),
                connection.responseCode,
                connection.url.toString()
            )
        } catch (e: Exception) {
            HTTPResponse(
            "",
            connection.responseCode,
            connection.url.toString()
            )
        }




    }

    override fun cookies(): List<HttpCookie> = cookies.toList()


    private fun generateUrlConnection(
        url: URL,
        httpMethod: String,
        headerFields: Map<String, String>?,
        cookies: Set<HttpCookie>
    ): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection

        headerFields?.forEach {
            connection.setRequestProperty(it.key, it.value)
        }
        connection.setRequestProperty(
            "Cookie",
            cookies.joinToString("; ") { "${it.name}=${it.value}" }
        )
        if (headerFields?.containsKey("User-Agent") == false) {
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 12; Pixel 7 XL) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/111.0.5563.57 Mobile Safari/537.36"
            )
        }
        connection.requestMethod = httpMethod
        connection.instanceFollowRedirects = false

        return connection
    }
}