package com.example.sciencetokyoportalkit.http.requests

import com.example.sciencetokyoportalkit.http.BaseURL
import com.example.sciencetokyoportalkit.http.HTTPMethod
import com.example.sciencetokyoportalkit.http.HTTPRequest
import com.example.sciencetokyoportalkit.http.LMSBaseURL
import com.example.sciencetokyoportalkit.model.HTMLInput
import com.example.sciencetokyoportalkit.model.HTMLMeta

class LMSRedirectPageRequest(
    htmlInputs: List<HTMLInput>,
    htmlMetas: List<HTMLMeta>
) : HTTPRequest {
    override val url = "${LMSBaseURL.origin}auth/saml2/sp/saml2-acs.php/lms.isct.ac.jp"
    override val method = HTTPMethod.POST
    override val body: Map<String, String>
    override val headerFields: Map<String, String>
    
    init {
        body = htmlInputs.associate { it.name to it.value }
        
        val headers = mutableMapOf(
            "Referer" to BaseURL.origin,
            "Host" to LMSBaseURL.host,
            "Origin" to BaseURL.origin,
            "Connection" to "keep-alive",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
            "Accept-Encoding" to "br, gzip, deflate",
            "Accept-Language" to "ja",
            "Sec-Fetch-Dest" to "document",
            "Sec-Fetch-Mode" to "navigate",
            "Sec-Fetch-Site" to "cross-site",
            "Priority" to "u=3, i"
        )
        
        htmlMetas.forEach { meta ->
            headers[meta.name] = meta.content
        }
        
        headerFields = headers
    }
}

