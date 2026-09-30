package app.titech.sciencetokyoportalkit.http

import app.titech.sciencetokyoportalkit.model.ScienceTokyoPortalCookie
import java.net.HttpCookie
import java.net.URL

/**
 * RFC 6265 に沿って Cookie を持つ。
 *
 * - Domain 属性の無い Cookie は受け取ったホストだけの Cookie ([ScienceTokyoPortalCookie.hostOnly]) にする
 * - name・domain・path が同じものを同じ Cookie として上書きする
 * - リクエストには送り先の URL に合う Cookie だけを付ける
 *
 * Public Suffix List による Domain 属性の検査はしない。
 */
internal class CookieStore(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private class Entry(
        val cookie: ScienceTokyoPortalCookie,
        /** null なら期限の無い (セッション) Cookie */
        val expiresAt: Long?,
        /** 最初に受け取った順。上書きしても変えない (RFC 6265 5.3 の creation-time) */
        val creationOrder: Long,
    )

    private val entries = mutableListOf<Entry>()
    private var nextCreationOrder = 0L

    /** [url] へのリクエストのレスポンスで受け取った Set-Cookie ヘッダの値を取り込む */
    @Synchronized
    fun store(setCookieHeaders: List<String>, url: URL) {
        val now = currentTimeMillis()
        setCookieHeaders
            .flatMap { header -> runCatching { HttpCookie.parse(header) }.getOrDefault(emptyList()) }
            .forEach { store(it, url, now) }
    }

    private fun store(parsed: HttpCookie, url: URL, now: Long) {
        val host = url.host.lowercase()
        // HttpCookie.parse は Domain 属性が無ければ null にし、値は先頭の . の有無も含めてそのまま入れる
        val domainAttribute = parsed.domain?.trimStart('.')?.lowercase()?.ifEmpty { null }
        if (domainAttribute != null && !domainMatches(host, domainAttribute)) return

        val cookie = ScienceTokyoPortalCookie(
            name = parsed.name,
            value = parsed.value,
            domain = domainAttribute ?: host,
            hostOnly = domainAttribute == null,
            path = parsed.path?.takeIf { it.startsWith("/") } ?: defaultPath(url),
            secure = parsed.secure,
            httpOnly = parsed.isHttpOnly,
        )

        val old = entries.firstOrNull {
            it.cookie.name == cookie.name && it.cookie.domain == cookie.domain && it.cookie.path == cookie.path
        }
        if (old != null) entries.remove(old)

        // Max-Age=0 や過去の Expires (HttpCookie.parse で maxAge が 0 になる) は同じ Cookie を消すだけ
        val maxAge = parsed.maxAge
        if (maxAge == 0L || maxAge < -1L) return

        entries.add(
            Entry(
                cookie = cookie,
                expiresAt = when {
                    maxAge < 0 -> null
                    maxAge > (Long.MAX_VALUE - now) / 1000 -> null
                    else -> now + maxAge * 1000
                },
                creationOrder = old?.creationOrder ?: nextCreationOrder++,
            )
        )
    }

    /** [url] へのリクエストに付ける Cookie。path の長いものから、同じ長さなら先に受け取ったものから並べる */
    @Synchronized
    fun cookiesFor(url: URL): List<ScienceTokyoPortalCookie> {
        removeExpired()
        return entries
            .filter { it.cookie.matches(url) }
            .sortedWith(compareByDescending<Entry> { it.cookie.path.length }.thenBy { it.creationOrder })
            .map { it.cookie }
    }

    /** 期限の切れていない全ての Cookie。受け取った順に並べる */
    @Synchronized
    fun all(): List<ScienceTokyoPortalCookie> {
        removeExpired()
        return entries.sortedBy { it.creationOrder }.map { it.cookie }
    }

    private fun removeExpired() {
        val now = currentTimeMillis()
        entries.removeAll { entry -> entry.expiresAt?.let { it <= now } ?: false }
    }
}

/** [url] へのリクエストに付ける Cookie か (RFC 6265 5.4 の domain・path・secure の条件) */
internal fun ScienceTokyoPortalCookie.matches(url: URL): Boolean {
    val host = url.host.lowercase()
    val domainOk = if (hostOnly) host == domain else domainMatches(host, domain)
    val secureOk = !secure || url.protocol.equals("https", ignoreCase = true)
    return domainOk && pathMatches(url.path.ifEmpty { "/" }, path) && secureOk
}

/**
 * [ScienceTokyoPortal.currentCookies] 用の [HttpCookie]。
 * 以前と同じくホストだけの Cookie は domain にホスト名を入れ、Domain 属性のある Cookie は `.` を付けて区別する。
 * 有効期限は引き継がない。
 */
internal fun ScienceTokyoPortalCookie.toHttpCookie(): HttpCookie =
    HttpCookie(name, value).also {
        it.domain = if (hostOnly) domain else ".$domain"
        it.path = path
        it.secure = secure
        it.isHttpOnly = httpOnly
    }

/** RFC 6265 5.1.3。[host] と [domain] は小文字で、[domain] は先頭の `.` を除いたもの */
internal fun domainMatches(host: String, domain: String): Boolean {
    if (host == domain) return true
    return host.endsWith(".$domain") && !isIPAddress(host)
}

/** RFC 6265 5.1.4 */
internal fun pathMatches(requestPath: String, cookiePath: String): Boolean {
    if (requestPath == cookiePath) return true
    if (!requestPath.startsWith(cookiePath)) return false
    return cookiePath.endsWith("/") || requestPath[cookiePath.length] == '/'
}

/** RFC 6265 5.1.4 の default-path */
internal fun defaultPath(url: URL): String {
    val path = url.path
    if (!path.startsWith("/")) return "/"
    val lastSlash = path.lastIndexOf('/')
    return if (lastSlash == 0) "/" else path.substring(0, lastSlash)
}

private fun isIPAddress(host: String): Boolean =
    host.contains(':') || host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))
