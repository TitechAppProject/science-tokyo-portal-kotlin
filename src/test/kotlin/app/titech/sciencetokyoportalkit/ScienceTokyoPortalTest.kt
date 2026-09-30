package app.titech.sciencetokyoportalkit

import app.titech.sciencetokyoportalkit.model.*
import org.junit.jupiter.api.Test
import java.net.HttpCookie
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScienceTokyoPortalTest {
    
    private val portal = ScienceTokyoPortal()
    
    @Test
    fun `validateUserNamePage - valid page`() {
        val html = this::class.java.getResource("/UserNamePage.html")?.readText() ?: ""
        assertTrue(portal.validateUserNamePage(html))
    }
    
    @Test
    fun `validateUserNamePage - error page`() {
        val html = this::class.java.getResource("/ErrorPage.html")?.readText() ?: ""
        assertFalse(portal.validateUserNamePage(html))
    }
    
    @Test
    fun `parseHTMLMeta from UserNamePage`() {
        val html = this::class.java.getResource("/UserNamePage.html")?.readText() ?: ""
        val metas = portal.parseHTMLMeta(html)
        
        val expected = listOf(
            HTMLMeta(name = "charset", content = "utf-8"),
            HTMLMeta(name = "X-UA-Compatible", content = "IE=edge"),
            HTMLMeta(name = "viewport", content = "width=device-width, initial-scale=1"),
            HTMLMeta(name = "csrf-param", content = "authenticity_token"),
            HTMLMeta(name = "csrf-token", content = "pyZr_b_Fp9BLeYqBkp7QoeaAt_VZsF0fSz1JmQxW433FS4h8e8gC0ve0P8pDrKy_yTZpYH3dA0wzzLONVLvIGQ")
        )
        
        assertEquals(expected, metas)
    }
    
    @Test
    fun `parseHTMLInput from UserNamePage identifier field`() {
        val html = this::class.java.getResource("/UserNamePage.html")?.readText() ?: ""
        val extractedHtml = portal.extractHTML(html, "div#identifier-field-wrapper")
        val inputs = portal.parseHTMLInput(extractedHtml)
        
        val expected = listOf(
            HTMLInput(name = "identifier", type = HTMLInputType.TEXT, value = "")
        )
        
        assertEquals(expected, inputs)
    }
    
    @Test
    fun `validateUserNamePageSubmitJson - valid json`() {
        val account = ScienceTokyoPortalAccount(username = "abcd1234", password = "password", totpSecret = null)
        val json = this::class.java.getResource("/UserNamePageSubmit.json")?.readText() ?: ""
        
        assertTrue(portal.validateUserNamePageSubmitJson(json, account))
    }
    
    @Test
    fun `validateUserNamePageSubmitJson - error json`() {
        val account = ScienceTokyoPortalAccount(username = "abcd1234", password = "password", totpSecret = null)
        val json = this::class.java.getResource("/UserNamePageSubmitError.json")?.readText() ?: ""
        
        assertFalse(portal.validateUserNamePageSubmitJson(json, account))
    }
    
    @Test
    fun `parseHTMLInput from PasswordPage`() {
        val html = this::class.java.getResource("/UserNamePage.html")?.readText() ?: ""
        val extractedHtml = portal.extractHTML(html, "form#login")
        val inputs = portal.parseHTMLInput(extractedHtml)
        
        val expected = listOf(
            HTMLInput(name = "utf8", type = HTMLInputType.HIDDEN, value = "✓"),
            HTMLInput(name = "authenticity_token", type = HTMLInputType.HIDDEN, 
                value = "yYAaIZ55k-d7MWeXfMDKmDI5PAXQ6IKra7TtjWV7jYoVDbMpOkEgaOH9WNxZwg94BxJ1BxHALQVCr_e33zlq2g"),
            HTMLInput(name = "identifier", type = HTMLInputType.TEXT, value = ""),
            HTMLInput(name = "password", type = HTMLInputType.PASSWORD, value = "")
        )
        
        assertEquals(expected, inputs)
    }
    
    @Test
    fun `validateSubmitScript - valid password script`() {
        val script = this::class.java.getResource("/PasswordPageSubmitScript.js")?.readText() ?: ""
        assertTrue(portal.validateSubmitScript(script))
    }
    
    @Test
    fun `validateSubmitScript - empty script`() {
        val script = ""
        assertFalse(portal.validateSubmitScript(script))
    }
    
    @Test
    fun `validateMethodSelectionPage - valid page`() {
        val html = this::class.java.getResource("/MethodSelectionPage.html")?.readText() ?: ""
        assertTrue(portal.validateMethodSelectionPage(html))
    }
    
    @Test
    fun `validateMethodSelectionPage - redirected to username page`() {
        val html = this::class.java.getResource("/UserNamePage.html")?.readText() ?: ""
        assertFalse(portal.validateMethodSelectionPage(html))
    }
    
    @Test
    fun `parseHTMLMeta from MethodSelectionPage`() {
        val html = this::class.java.getResource("/MethodSelectionPage.html")?.readText() ?: ""
        val metas = portal.parseHTMLMeta(html)
        
        val expected = listOf(
            HTMLMeta(name = "charset", content = "utf-8"),
            HTMLMeta(name = "X-UA-Compatible", content = "IE=edge"),
            HTMLMeta(name = "viewport", content = "width=device-width, initial-scale=1"),
            HTMLMeta(name = "csrf-param", content = "authenticity_token"),
            HTMLMeta(name = "csrf-token", content = "VeEeyMqDgJjFAnlsaqRJX4tWERGS89qRhHPU2jetjTwpCkr_l4u-5SQdxB1mvXaRm67tMDwLQFKA6QOjEPGD2Q")
        )
        
        assertEquals(expected, metas)
    }
    
    @Test
    fun `parseHTMLInput from MethodSelectionPage totp form`() {
        val html = this::class.java.getResource("/MethodSelectionPage.html")?.readText() ?: ""
        val extractedHtml = portal.extractHTML(html, "form#totp-form")
        val inputs = portal.parseHTMLInput(extractedHtml)
        
        val expected = listOf(
            HTMLInput(name = "utf8", type = HTMLInputType.HIDDEN, value = "✓"),
            HTMLInput(name = "authenticity_token", type = HTMLInputType.HIDDEN,
                value = "-sXIN3BuAlYSqBeHqvXL46jh_jA4z06hgfed9ymvtH6PawKonTII3Z_TpmORb2f7vgDx37Rk8jtBRTN1gA_MIA"),
            HTMLInput(name = "totp", type = HTMLInputType.TEXT, value = "")
        )
        
        assertEquals(expected, inputs)
    }
    
    @Test
    fun `validateSubmitScript - valid OTP script`() {
        val script = this::class.java.getResource("/OtpPageSubmitScript.js")?.readText() ?: ""
        assertTrue(portal.validateSubmitScript(script))
    }
    
    @Test
    fun `validateWaitingPage - valid page`() {
        val html = this::class.java.getResource("/WaitingPage.html")?.readText() ?: ""
        assertTrue(portal.validateWaitingPage(html))
    }
    
    @Test
    fun `validateWaitingPage - error page`() {
        val html = this::class.java.getResource("/ErrorPage.html")?.readText() ?: ""
        assertFalse(portal.validateWaitingPage(html))
    }
    
    @Test
    fun `validateResourceListPage - valid page`() {
        val html = this::class.java.getResource("/ResourceListPage.html")?.readText() ?: ""
        assertTrue(portal.validateResourceListPage(html))
    }
    
    @Test
    fun `validateResourceListPage - system error page`() {
        val html = this::class.java.getResource("/SystemErrorPage.html")?.readText() ?: ""
        assertFalse(portal.validateResourceListPage(html))
    }
    
    private fun lmsCookie(name: String, domain: String = "lms.s.isct.ac.jp", path: String = "/2025/") =
        ScienceTokyoPortalCookie(
            name = name,
            value = "test",
            domain = domain,
            hostOnly = true,
            path = path,
            secure = true,
            httpOnly = true,
        )

    @Test
    fun `validateLMSPage - has MoodleSession cookie`() {
        assertTrue(portal.validateLMSPage(listOf(lmsCookie("MoodleSession"))))
    }
    
    @Test
    fun `validateLMSPage - no MoodleSession cookie`() {
        assertFalse(portal.validateLMSPage(listOf(lmsCookie("OtherCookie"))))
    }

    @Test
    fun `validateLMSPage - MoodleSession cookie for another host`() {
        assertFalse(portal.validateLMSPage(listOf(lmsCookie("MoodleSession", domain = "isct.ex-tic.com"))))
    }

    @Test
    fun `validateLMSPage - MoodleSession cookie for another path`() {
        assertFalse(portal.validateLMSPage(listOf(lmsCookie("MoodleSession", path = "/2024/"))))
    }

    @Suppress("DEPRECATION")
    @Test
    fun `validateLMSPage - HttpCookie`() {
        val cookies = listOf(
            HttpCookie("MoodleSession", "test").apply {
                path = "/"
                domain = "lms.s.isct.ac.jp"
            }
        )
        assertTrue(portal.validateLMSPage(cookies))
    }
    
    @Test
    fun `validateLMSRedirectPage - valid page`() {
        val html = this::class.java.getResource("/LmsRedirectPage.html")?.readText() ?: ""
        assertTrue(portal.validateLMSRedirectPage(html))
    }
    
    @Test
    fun `validateLMSRedirectPage - error page`() {
        val html = this::class.java.getResource("/LMSErrorPage.html")?.readText() ?: ""
        assertFalse(portal.validateLMSRedirectPage(html))
    }
    
    @Test
    fun `detectPolicyError - no error`() {
        val html = this::class.java.getResource("/LmsPage.html")?.readText() ?: ""
        assertFalse(portal.detectPolicyError(html))
    }
    
    @Test
    fun `detectPolicyError - has policy error`() {
        val errorHtml = """
        <html>
        <title>
            Policies
        </title>
        <body>
        </body>
        </html>
        """
        assertTrue(portal.detectPolicyError(errorHtml))
    }
}