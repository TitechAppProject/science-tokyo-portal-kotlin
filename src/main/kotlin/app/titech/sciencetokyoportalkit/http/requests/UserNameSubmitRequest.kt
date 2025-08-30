package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.BaseURL
import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest
import app.titech.sciencetokyoportalkit.model.HTMLInput
import app.titech.sciencetokyoportalkit.model.HTMLMeta
import java.net.URLEncoder

class UserNameSubmitRequest(
    htmlInputs: List<HTMLInput>,
    htmlMetas: List<HTMLMeta>
) : HTTPRequest {
    override val url: String
    override val method = HTTPMethod.GET
    override val headerFields: Map<String, String>
    
    init {
        val baseUrl = "${BaseURL.origin}/auth/session/first_factor"
        val queryParams = htmlInputs.joinToString("&") { input ->
            "${URLEncoder.encode(input.name, "UTF-8")}=${URLEncoder.encode(input.value, "UTF-8")}"
        }
        url = if (queryParams.isNotEmpty()) "$baseUrl?$queryParams" else baseUrl
        
        val headers = mutableMapOf(
            "Referer" to "${BaseURL.origin}/auth/session",
            "Host" to BaseURL.host,
            "Origin" to BaseURL.origin,
            "Connection" to "keep-alive",
            "Accept" to "application/json, text/javascript, */*; q=0.01",
            "Accept-Encoding" to "br, gzip, deflate",
            "Accept-Language" to "ja",
            "Sec-Fetch-Dest" to "empty",
            "Sec-Fetch-Mode" to "cors",
            "Sec-Fetch-Site" to "same-origin",
            "X-Requested-With" to "XMLHttpRequest",
            "Priority" to "u=3, i"
        )
        
        htmlMetas.forEach { meta ->
            headers[meta.name] = meta.content
        }
        
        headerFields = headers
    }
}