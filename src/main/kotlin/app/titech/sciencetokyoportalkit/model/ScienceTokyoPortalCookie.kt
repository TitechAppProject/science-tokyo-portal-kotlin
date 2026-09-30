package app.titech.sciencetokyoportalkit.model

/**
 * ログインで受け取った Cookie。RFC 6265 と同じく name・[domain]・[path] の組で 1 つの Cookie を表す。
 */
data class ScienceTokyoPortalCookie(
    val name: String,
    val value: String,
    /**
     * [hostOnly] なら Cookie を受け取ったホスト名。そうでなければ Set-Cookie の Domain 属性の値。
     * どちらも小文字で、先頭の `.` は付かない。
     */
    val domain: String,
    /**
     * Set-Cookie に Domain 属性が無く、受け取ったホスト ([domain]) にだけ送る Cookie なら true。
     * Domain 属性があれば、値が受け取ったホストと同じでも false (サブドメインにも送る)。
     */
    val hostOnly: Boolean,
    /** Path 属性。無いか `/` で始まらないときは、受け取った URL から決まる既定の path (RFC 6265 5.1.4) */
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    /** 有効期限 (UNIX 時間のミリ秒)。Max-Age・Expires 属性が無いセッション Cookie なら null */
    val expiresAt: Long?,
)
