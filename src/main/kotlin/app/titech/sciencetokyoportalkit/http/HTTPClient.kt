package app.titech.sciencetokyoportalkit.http

import app.titech.sciencetokyoportalkit.model.ScienceTokyoPortalCookie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

interface HTTPClient {
    suspend fun send(request: HTTPRequest): HTTPResponse
    fun cookies(): List<ScienceTokyoPortalCookie>
}

data class HTTPResponse(
    val html: String,
    val statusCode: Int,
    val responseUrl: String?
)

class HTTPClientImpl(
    private val userAgent: String
) : HTTPClient {
    private val cookieStore = CookieStore()

    override suspend fun send(request: HTTPRequest): HTTPResponse = withContext(Dispatchers.IO) {
        val url = URL(request.url)
        var connection = generateUrlConnection(url, request.method.value, request.headerFields)

        do {
            println("RequestURL: " + connection.url.toString())
            println("Method: " + connection.requestMethod.toString())
            println("RequestHeaders: " + connection.requestProperties.toString())
            println("RequestBody: " + request.body.toString())
            connection.doOutput = request.body != null && connection.requestMethod == "POST"
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

            val setCookie = connection.headerFields
                .filterKeys { it != null && it.equals("Set-Cookie", ignoreCase = true) }
                .values
                .flatten()
            cookieStore.store(setCookie, connection.url)

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
                        generateUrlConnection(locationURL, "GET", request.headerFields)
                    needRedirect = true
                } catch (e: Exception) {
                    needRedirect = false
                }
            }
        } while (needRedirect)

        try {
            val br = BufferedReader(InputStreamReader(decodedStream(connection)))

            val sb = StringBuilder()

            for (line in br.readLines()) {
                sb.append(line)
            }

            br.close()

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

    override fun cookies(): List<ScienceTokyoPortalCookie> = cookieStore.all()

    /// Accept-Encodingを明示的に指定しているため、gzipの透過的な解凍は行われない。
    /// Content-Encodingを見て自前で解凍する。
    private fun decodedStream(connection: HttpURLConnection): InputStream {
        val stream = connection.inputStream

        return if (connection.contentEncoding?.equals("gzip", ignoreCase = true) == true) {
            GZIPInputStream(stream)
        } else {
            stream
        }
    }

    private fun generateUrlConnection(
        url: URL,
        httpMethod: String,
        headerFields: Map<String, String>?
    ): HttpURLConnection {
        val connection = url.openConnection() as HttpURLConnection

        headerFields?.forEach {
            connection.setRequestProperty(it.key, it.value)
        }
        val cookies = cookieStore.cookiesFor(url)
        if (cookies.isNotEmpty()) {
            connection.setRequestProperty(
                "Cookie",
                cookies.joinToString("; ") { "${it.name}=${it.value}" }
            )
        }
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