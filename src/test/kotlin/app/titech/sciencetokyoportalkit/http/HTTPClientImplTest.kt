package app.titech.sciencetokyoportalkit.http

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress
import kotlin.test.assertEquals

/**
 * localhost と 127.0.0.1 を別のホストとして使い、Cookie を送り先に合わせて付けるかを見る。
 */
class HTTPClientImplTest {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
        createContext("/") { exchange ->
            when (exchange.requestURI.path) {
                "/auth/session" -> {
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=old")
                    exchange.responseHeaders.add("Set-Cookie", "SESSION=host-only")
                    exchange.responseHeaders.add("Set-Cookie", "ROOT=1; Path=/")
                    exchange.responseHeaders.add("Set-Cookie", "SECURE=1; Path=/; Secure")
                    exchange.responseHeaders.add("Location", "http://127.0.0.1:${exchange.localAddress.port}/set")
                    exchange.sendResponseHeaders(302, -1)
                }
                "/set" -> {
                    exchange.responseHeaders.add("Set-Cookie", "OTHER=1; Path=/")
                    exchange.responseHeaders.add("Location", "http://127.0.0.1:${exchange.localAddress.port}/echo")
                    exchange.sendResponseHeaders(302, -1)
                }
                else -> {
                    val body = (exchange.requestHeaders["Cookie"] ?: emptyList()).joinToString("|").toByteArray()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
            }
            exchange.close()
        }
        start()
    }
    private val port = server.address.port

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    private class Get(override val url: String) : HTTPRequest {
        override val method = HTTPMethod.GET
        override val headerFields = mapOf("User-Agent" to "test")
    }

    @Test
    fun `Cookie は送り先のホストと path に合うものだけを付ける`() = runBlocking {
        val client = HTTPClientImpl("test")

        // リダイレクト先の 127.0.0.1 には、そこで受け取った Cookie だけを付け、localhost の Cookie は付けない
        val redirected = client.send(Get("http://localhost:$port/auth/session"))
        assertEquals("http://127.0.0.1:$port/echo", redirected.responseUrl)
        assertEquals("OTHER=1", redirected.html)

        // 同じ Cookie が 1 つのレスポンスに複数あれば後のものを使う (受け取った順を保つ)
        assertEquals(
            listOf(
                Triple("localhost", "SESSION", "host-only"),
                Triple("localhost", "ROOT", "1"),
                Triple("localhost", "SECURE", "1"),
                Triple("127.0.0.1", "OTHER", "1"),
            ),
            client.cookies().map { Triple(it.domain, it.name, it.value) }
        )
        assertEquals(listOf(true, true, true, true), client.cookies().map { it.hostOnly })
        assertEquals("SESSION=host-only; ROOT=1", client.send(Get("http://localhost:$port/auth/echo")).html)
        assertEquals("ROOT=1", client.send(Get("http://localhost:$port/echo")).html)
    }
}
