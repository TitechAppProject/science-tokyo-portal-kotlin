package app.titech.sciencetokyoportalkit.http

import kotlinx.serialization.json.JsonObject

object BaseURL {
    var origin = "https://isct.ex-tic.com"
    var host = "isct.ex-tic.com"

    fun changeToMock() {
        origin = "https://extic-mock.isct.app"
        host = "extic-mock.isct.app"
    }
}

object LMSBaseURL {
    const val origin = "https://lms.s.isct.ac.jp/2025/"
    const val host = "lms.s.isct.ac.jp"
}

enum class HTTPMethod(val value: String) {
    GET("GET"),
    POST("POST")
}

interface HTTPRequest {
    val url: String
    val method: HTTPMethod
    val headerFields: Map<String, String>?
    val body: Map<String, String>?
        get() = null
}