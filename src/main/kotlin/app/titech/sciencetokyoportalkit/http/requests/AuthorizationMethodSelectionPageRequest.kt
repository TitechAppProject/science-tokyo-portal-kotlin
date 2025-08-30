package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.BaseURL
import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest

class AuthorizationMethodSelectionPageRequest : HTTPRequest {
    override val url = "${BaseURL.origin}/auth/session/second_factor"
    override val method = HTTPMethod.GET
    override val headerFields = mapOf(
        "Referer" to "${BaseURL.origin}/auth/session",
        "Host" to BaseURL.host,
        "Connection" to "keep-alive",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Encoding" to "br, gzip, deflate",
        "Accept-Language" to "ja",
        "Sec-Fetch-Dest" to "document",
        "Sec-Fetch-Mode" to "navigate",
        "Sec-Fetch-Site" to "same-origin",
        "Priority" to "u=0, i"
    )
}