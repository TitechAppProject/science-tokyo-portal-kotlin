package app.titech.sciencetokyoportalkit.http.requests

import app.titech.sciencetokyoportalkit.http.HTTPMethod
import app.titech.sciencetokyoportalkit.http.HTTPRequest
import app.titech.sciencetokyoportalkit.http.LMSBaseURL
import kotlin.random.Random

class LMSTokenRequest : HTTPRequest {
    override val url: String
    override val method = HTTPMethod.GET
    override val headerFields = mapOf(
        "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
    )
    override val body: Map<String, String>? = null
    
    init {
        val queryParameters = mapOf(
            "service" to "moodle_mobile_app",
            "passport" to Random.nextDouble(0.0, 1000.0).toString(),
            "urlscheme" to "moodlemobile"
        )
        url = LMSBaseURL.origin + "admin/tool/mobile/launch.php?" + 
              queryParameters.map { "${it.key}=${it.value}" }.joinToString("&")
    }
}

