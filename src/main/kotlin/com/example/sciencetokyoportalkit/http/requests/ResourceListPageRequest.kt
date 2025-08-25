package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.BaseURL
import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest
import app.titech.sciencetokyoportalkit.model.HTMLInput

class ResourceListPageRequest(
    htmlInputs: List<HTMLInput>,
    referer: String
) : HTTPRequest {
    override val url = "${BaseURL.origin}/idm/user/login/saml2/sso/user-isct"
    override val method = HTTPMethod.POST
    override val body: Map<String, String> = htmlInputs.associate { it.name to it.value }
    override val headerFields: Map<String, String> = mapOf(
        "Referer" to referer,
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
}