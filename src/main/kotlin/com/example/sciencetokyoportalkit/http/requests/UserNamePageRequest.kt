package com.example.sciencetokyoportalkit.http.requests

import com.example.sciencetokyoportalkit.http.BaseURL
import com.example.sciencetokyoportalkit.http.HTTPMethod
import com.example.sciencetokyoportalkit.http.HTTPRequest

class UserNamePageRequest : HTTPRequest {
    override val url = "${BaseURL.origin}/auth/session"
    override val method = HTTPMethod.GET
    override val headerFields = mapOf(
        "Connection" to "keep-alive",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Encoding" to "br, gzip, deflate",
        "Accept-Language" to "ja-jp"
    )
}