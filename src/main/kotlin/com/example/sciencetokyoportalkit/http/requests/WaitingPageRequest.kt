package com.example.sciencetokyoportalkit.http.requests

import com.example.sciencetokyoportalkit.http.BaseURL
import com.example.sciencetokyoportalkit.http.HTTPMethod
import com.example.sciencetokyoportalkit.http.HTTPRequest

class WaitingPageRequest(
    override val url: String
) : HTTPRequest {
    override val method = HTTPMethod.GET
    override val headerFields = mapOf(
        "Referer" to "${BaseURL.origin}/auth/session/second_factor",
        "Host" to BaseURL.host,
        "Origin" to BaseURL.origin,
        "Connection" to "keep-alive",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Encoding" to "br, gzip, deflate",
        "Accept-Language" to "ja",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "same-origin",
        "Priority" to "u=3, i"
    )
    override val body: Map<String, String>? = null
}

