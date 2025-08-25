package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest
import app.titech.sciencetokyoportalkit.http.LMSBaseURL

class LMSPageRequest : HTTPRequest {
    override val url = LMSBaseURL.origin
    override val method = HTTPMethod.GET
    override val headerFields = mapOf(
        "Connection" to "keep-alive",
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Encoding" to "br, gzip, deflate",
        "Accept-Language" to "ja-jp"
    )
}