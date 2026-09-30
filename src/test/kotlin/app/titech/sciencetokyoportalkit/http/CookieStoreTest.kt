package app.titech.sciencetokyoportalkit.http

import app.titech.sciencetokyoportalkit.model.ScienceTokyoPortalCookie
import org.junit.jupiter.api.Test
import java.net.URL
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CookieStoreTest {
    private var now = 1_000_000L
    private val store = CookieStore { now }

    private fun names(url: String) = store.cookiesFor(URL(url)).map { "${it.name}=${it.value}" }

    @Test
    fun `Domain 属性の無い Cookie は受け取ったホストだけの Cookie になる`() {
        store.store(listOf("SESSION=abc; Path=/; Secure; HttpOnly"), URL("https://isct.ex-tic.com/auth/session"))

        assertEquals(
            listOf(
                ScienceTokyoPortalCookie(
                    name = "SESSION",
                    value = "abc",
                    domain = "isct.ex-tic.com",
                    hostOnly = true,
                    path = "/",
                    secure = true,
                    httpOnly = true,
                    expiresAt = null,
                )
            ),
            store.all()
        )
        assertEquals(listOf("SESSION=abc"), names("https://isct.ex-tic.com/"))
        assertEquals(emptyList(), names("https://sub.isct.ex-tic.com/"))
        assertEquals(emptyList(), names("https://lms.s.isct.ac.jp/"))
    }

    @Test
    fun `Domain 属性が受け取ったホストと同じでもホストだけの Cookie にしない`() {
        store.store(listOf("SESSION=abc; Domain=isct.ex-tic.com; Path=/"), URL("https://isct.ex-tic.com/auth/session"))

        val cookie = store.all().single()
        assertEquals("isct.ex-tic.com", cookie.domain)
        assertFalse(cookie.hostOnly)
        assertEquals(listOf("SESSION=abc"), names("https://sub.isct.ex-tic.com/"))
    }

    @Test
    fun `Domain 属性は先頭の点の有無によらず同じドメインになる`() {
        store.store(listOf("A=1; Domain=.isct.ac.jp; Path=/"), URL("https://lms.s.isct.ac.jp/2025/"))
        store.store(listOf("B=2; Domain=ISCT.ac.jp; Path=/"), URL("https://lms.s.isct.ac.jp/2025/"))

        assertEquals(listOf("isct.ac.jp", "isct.ac.jp"), store.all().map { it.domain })
        assertEquals(listOf(false, false), store.all().map { it.hostOnly })
        assertEquals(listOf("A=1", "B=2"), names("https://portal.isct.ac.jp/"))
        assertEquals(listOf("A=1", "B=2"), names("https://isct.ac.jp/"))
        assertEquals(emptyList(), names("https://notisct.ac.jp/"))
    }

    @Test
    fun `受け取ったホストに合わない Domain 属性の Cookie は捨てる`() {
        store.store(
            listOf("A=1; Domain=example.com", "B=2; Domain=sub.isct.ex-tic.com"),
            URL("https://isct.ex-tic.com/")
        )

        assertEquals(emptyList(), store.all())
    }

    @Test
    fun `同じ名前でもホストが違う Cookie は別々に持つ`() {
        store.store(listOf("SESSION=extic; Path=/"), URL("https://isct.ex-tic.com/auth/session"))
        store.store(listOf("SESSION=lms; Path=/"), URL("https://lms.s.isct.ac.jp/2025/"))

        assertEquals(listOf("isct.ex-tic.com", "lms.s.isct.ac.jp"), store.all().map { it.domain })
        assertEquals(listOf("SESSION=extic"), names("https://isct.ex-tic.com/auth/session"))
        assertEquals(listOf("SESSION=lms"), names("https://lms.s.isct.ac.jp/2025/"))
    }

    @Test
    fun `同じ名前でも path が違う Cookie は別々に持ち path の長い順に送る`() {
        store.store(listOf("SESSION=root; Path=/"), URL("https://isct.ex-tic.com/"))
        store.store(listOf("SESSION=auth; Path=/auth"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("SESSION=auth", "SESSION=root"), names("https://isct.ex-tic.com/auth/session"))
        assertEquals(listOf("SESSION=root"), names("https://isct.ex-tic.com/idm"))
    }

    @Test
    fun `name・domain・path が同じ Cookie は上書きし最初に受け取った順を保つ`() {
        store.store(listOf("A=1; Path=/", "B=1; Path=/"), URL("https://isct.ex-tic.com/"))
        store.store(listOf("A=2; Path=/"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("A=2", "B=1"), names("https://isct.ex-tic.com/"))
    }

    @Test
    fun `ホストだけの Cookie と Domain 属性が同じホストの Cookie は同じ Cookie として上書きする`() {
        store.store(listOf("SESSION=old; Path=/"), URL("https://isct.ex-tic.com/"))
        store.store(listOf("SESSION=new; Domain=isct.ex-tic.com; Path=/"), URL("https://isct.ex-tic.com/"))

        val cookie = store.all().single()
        assertEquals("new", cookie.value)
        assertFalse(cookie.hostOnly)
    }

    @Test
    fun `Max-Age=0 と過去の Expires は同じ Cookie を消す`() {
        store.store(listOf("A=1; Path=/", "B=1; Path=/", "C=1; Path=/"), URL("https://isct.ex-tic.com/"))
        store.store(
            listOf("A=; Path=/; Max-Age=0", "B=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT"),
            URL("https://isct.ex-tic.com/")
        )

        assertEquals(listOf("C=1"), names("https://isct.ex-tic.com/"))
    }

    @Test
    fun `Max-Age が 0 以下なら -1 でも同じ Cookie を消す`() {
        store.store(listOf("A=1; Path=/", "B=1; Path=/"), URL("https://isct.ex-tic.com/"))
        store.store(listOf("A=; Path=/; Max-Age=-1"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("B=1"), names("https://isct.ex-tic.com/"))
    }

    @Test
    fun `Max-Age は Expires より優先する`() {
        store.store(
            listOf("A=1; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Max-Age=10"),
            URL("https://isct.ex-tic.com/")
        )

        assertEquals(listOf(now + 10_000), store.all().map { it.expiresAt })
    }

    @Test
    fun `同じ Cookie が 1 つのレスポンスに複数あれば後のものを使う`() {
        store.store(listOf("A=1; Path=/", "A=2; Path=/"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("A=2"), names("https://isct.ex-tic.com/"))
    }

    @Test
    fun `期限の切れた Cookie は送らない`() {
        store.store(listOf("A=1; Path=/; Max-Age=10", "B=1; Path=/"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf(now + 10_000, null), store.all().map { it.expiresAt })
        now += 9_000
        assertEquals(listOf("A=1", "B=1"), names("https://isct.ex-tic.com/"))
        now += 1_000
        assertEquals(listOf("B=1"), names("https://isct.ex-tic.com/"))
        assertEquals(listOf("B"), store.all().map { it.name })
    }

    @Test
    fun `Path 属性が無いか不正なら受け取った URL から既定の path を決める`() {
        store.store(listOf("A=1", "B=1; Path=relative"), URL("https://isct.ex-tic.com/auth/session"))
        store.store(listOf("C=1"), URL("https://isct.ex-tic.com/auth"))

        assertEquals(listOf("/auth", "/auth", "/"), store.all().map { it.path })
        assertEquals(listOf("C=1"), names("https://isct.ex-tic.com/idm"))
        assertEquals(listOf("A=1", "B=1", "C=1"), names("https://isct.ex-tic.com/auth/session/second_factor"))
    }

    @Test
    fun `Secure の Cookie は http には送らない`() {
        store.store(listOf("A=1; Path=/; Secure", "B=1; Path=/"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("B=1"), names("http://isct.ex-tic.com/"))
    }

    @Test
    fun `壊れた Set-Cookie は読み飛ばす`() {
        store.store(listOf("", "A=1; Path=/"), URL("https://isct.ex-tic.com/"))

        assertEquals(listOf("A=1"), names("https://isct.ex-tic.com/"))
    }

    @Test
    fun pathMatches() {
        assertTrue(pathMatches("/", "/"))
        assertTrue(pathMatches("/auth", "/"))
        assertTrue(pathMatches("/auth", "/auth"))
        assertTrue(pathMatches("/auth/session", "/auth"))
        assertTrue(pathMatches("/auth/session", "/auth/"))
        assertFalse(pathMatches("/authx", "/auth"))
        assertFalse(pathMatches("/", "/auth"))
    }

    @Test
    fun defaultPath() {
        assertEquals("/", defaultPath(URL("https://isct.ex-tic.com")))
        assertEquals("/", defaultPath(URL("https://isct.ex-tic.com/")))
        assertEquals("/", defaultPath(URL("https://isct.ex-tic.com/auth")))
        assertEquals("/auth", defaultPath(URL("https://isct.ex-tic.com/auth/session")))
        assertEquals("/2025", defaultPath(URL("https://lms.s.isct.ac.jp/2025/")))
    }

    @Test
    fun maxAgeAttribute() {
        assertEquals(10L, maxAgeAttribute("A=1; Path=/; max-age = 10"))
        assertEquals(-1L, maxAgeAttribute("A=1; Max-Age=-1"))
        assertEquals(null, maxAgeAttribute("A=1; Path=/"))
        assertEquals(null, maxAgeAttribute("Max-Age=1; Path=/"))
    }
}
