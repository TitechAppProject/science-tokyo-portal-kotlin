package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.BaseURL
import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest
import app.titech.sciencetokyoportalkit.model.HTMLInput
import app.titech.sciencetokyoportalkit.model.HTMLMeta

class OTPSubmitRequest(
    htmlInputs: List<HTMLInput>,
    htmlMetas: List<HTMLMeta>
) : HTTPRequest {
    override val url = "${BaseURL.origin}/auth/session/second_factor"
    override val method = HTTPMethod.POST
    override val body: Map<String, String> = htmlInputs.associate { it.name to it.value }
    override val headerFields: Map<String, String>
    
    init {
        val headers = mutableMapOf(
            "Referer" to "${BaseURL.origin}/auth/session/second_factor",
            "Host" to BaseURL.host,
            "Origin" to BaseURL.origin,
            "Connection" to "keep-alive",
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
            "Accept" to "*/*;q=0.5, text/javascript, application/javascript, application/ecmascript, application/x-ecmascript",
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