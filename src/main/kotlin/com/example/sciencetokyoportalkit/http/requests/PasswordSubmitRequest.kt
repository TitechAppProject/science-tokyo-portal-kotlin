package com.example.sciencetokyoportalkit.http.requests

import com.example.sciencetokyoportalkit.http.BaseURL
import com.example.sciencetokyoportalkit.http.HTTPMethod
import com.example.sciencetokyoportalkit.http.HTTPRequest
import com.example.sciencetokyoportalkit.model.HTMLInput
import com.example.sciencetokyoportalkit.model.HTMLMeta

class PasswordSubmitRequest(
    htmlInputs: List<HTMLInput>,
    htmlMetas: List<HTMLMeta>
) : HTTPRequest {
    override val url = "${BaseURL.origin}/auth/session"
    override val method = HTTPMethod.POST
    override val body: Map<String, String> = htmlInputs.associate { it.name to it.value }
    override val headerFields: Map<String, String>
    
    init {

        val headers = mutableMapOf(
            "Referer" to "${BaseURL.origin}/auth/session",
            "Host" to BaseURL.host,
            "Origin" to BaseURL.origin,
            "Connection" to "keep-alive",
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
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