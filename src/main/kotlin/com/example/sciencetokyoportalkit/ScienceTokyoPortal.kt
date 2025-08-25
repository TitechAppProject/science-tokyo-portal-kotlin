package app.titech.sciencetokyoportalkit

import app.titech.sciencetokyoportalkit.http.*
import app.titech.sciencetokyoportalkit.http.requests.*
import app.titech.sciencetokyoportalkit.model.*
import app.titech.sciencetokyoportalkit.utility.calculateTOTP
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import java.net.HttpCookie
import java.util.*

class ScienceTokyoPortal(
    userAgent: String = DEFAULT_USER_AGENT
) {
    companion object {
        const val DEFAULT_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
    }
    
    private val httpClient: HTTPClient = HTTPClientImpl(userAgent)
    
    suspend fun login(account: ScienceTokyoPortalAccount) {
        val userNamePageHtml = fetchUserNamePage()
        
        if (validateResourceListPage(userNamePageHtml)) {
            throw ScienceTokyoPortalLoginError.AlreadyLoggedIn
        }
        
        if (!validateUserNamePage(userNamePageHtml)) {
            throw ScienceTokyoPortalLoginError.InvalidUserNamePage
        }
        
        val userNamePageMetas = parseHTMLMeta(userNamePageHtml)
        val extractedUserNameHtml = extractHTML(userNamePageHtml, "div#identifier-field-wrapper")
        val userNamePageInputs = parseHTMLInput(extractedUserNameHtml)
        
        val userNamePageSubmitJson = submitUserName(userNamePageInputs, userNamePageMetas, account.username)
        if (!validateUserNamePageSubmitJson(userNamePageSubmitJson, account)) {
            throw ScienceTokyoPortalLoginError.InvalidUserNamePage
        }
        
        val extractedPasswordPageHtml = extractHTML(userNamePageHtml, "form#login")
        val passwordPageInputs = parseHTMLInput(extractedPasswordPageHtml)
        val passwordPageSubmitStatusCode = submitPassword(passwordPageInputs, userNamePageMetas, account.username, account.password)
        if (!validateSubmitStatusCode(passwordPageSubmitStatusCode)) {
            throw ScienceTokyoPortalLoginError.InvalidPasswordPage
        }
        
        val methodSelectionPageHtml = fetchAuthorizationMethodSelectionPage()
        if (!validateMethodSelectionPage(methodSelectionPageHtml)) {
            throw ScienceTokyoPortalLoginError.InvalidMethodSelectionPage
        }
        
        val methodSelectionPageMetas = parseHTMLMeta(methodSelectionPageHtml)
        val extractedOTPPageHtml = extractHTML(methodSelectionPageHtml, "form#totp-form")
        val otpPageInputs = parseHTMLInput(extractedOTPPageHtml)
        val otpPageSubmitScript = submitTOTP(otpPageInputs, methodSelectionPageMetas, account)
        
        if (!validateSubmitScript(otpPageSubmitScript)) {
            throw ScienceTokyoPortalLoginError.InvalidTOTPPage
        }
        
        val otpPageSubmitURL = parseScriptToURL(otpPageSubmitScript)
        val waitingPageHtml = fetchWaitingPage(otpPageSubmitURL)
        
        if (!validateWaitingPage(waitingPageHtml)) {
            throw ScienceTokyoPortalLoginError.InvalidWaitingPage
        }
        
        val waitingPageHtmlInputs = parseHTMLInput(waitingPageHtml)
        val resourceListPageHtml = fetchResourceListPage(waitingPageHtmlInputs, otpPageSubmitURL)
        
        if (!validateResourceListPage(resourceListPageHtml)) {
            throw ScienceTokyoPortalLoginError.InvalidResourceListPage
        }
    }
    
    suspend fun getLMSDashboard() {
        val lmsPageHtml = fetchLMSPage()
        
        if (!validateLMSPage(httpClient.cookies())) {
            throw LMSLoginError.InvalidDashboardPage
        }
        
        val lmsPageHtmlInputs = parseHTMLInput(lmsPageHtml)
        val lmsRedirectPageHtml = fetchLMSRedirectPage(lmsPageHtmlInputs)
        
        if (detectPolicyError(lmsRedirectPageHtml)) {
            throw LMSLoginError.Policy
        }
        
        if (!validateLMSRedirectPage(lmsRedirectPageHtml)) {
            throw LMSLoginError.InvalidDashboardPage
        }
    }
    
    suspend fun getLMSToken(): String {
        val (lmsTokenHtml, responseUrl) = fetchLMSTokenPage()
        
        val doc = try {
            Jsoup.parse(lmsTokenHtml)
        } catch (e: Exception) {
            throw LMSLoginError.ParseHtml
        }
        
        val launchapp = doc.select("a#launchapp").first()
        val href = launchapp?.attr("href")
        
        if (href == null || !href.startsWith("moodlemobile://token=")) {
            throw LMSLoginError.ParseUrlScheme(lmsTokenHtml, responseUrl)
        }
        
        val base64Token = href.replace("moodlemobile://token=", "")
        val decodedData = Base64.getDecoder().decode(base64Token)
        val decodedStr = String(decodedData)
        
        val splitedToken = decodedStr.split(":::")
        
        return if (splitedToken.size > 1) {
            splitedToken[1]
        } else {
            throw LMSLoginError.ParseToken(lmsTokenHtml, responseUrl)
        }
    }
    
    suspend fun checkUsernamePassword(username: String, password: String): Boolean {
        val account = ScienceTokyoPortalAccount(username, password, null)
        
        val userNamePageHtml = fetchUserNamePage()
        
        if (validateResourceListPage(userNamePageHtml)) {
            throw ScienceTokyoPortalLoginError.AlreadyLoggedIn
        }
        
        if (!validateUserNamePage(userNamePageHtml)) {
            throw ScienceTokyoPortalLoginError.InvalidUserNamePage
        }
        
        val userNamePageMetas = parseHTMLMeta(userNamePageHtml)
        val extractedUserNameHtml = extractHTML(userNamePageHtml, "div#identifier-field-wrapper")
        val userNamePageInputs = parseHTMLInput(extractedUserNameHtml)
        
        val userNamePageSubmitJson = submitUserName(userNamePageInputs, userNamePageMetas, account.username)
        
        if (!validateUserNamePageSubmitJson(userNamePageSubmitJson, account)) {
            throw ScienceTokyoPortalLoginError.InvalidUserNamePage
        }
        
        val extractedPasswordPageHtml = extractHTML(userNamePageHtml, "form#login")
        val passwordPageInputs = parseHTMLInput(extractedPasswordPageHtml)
        val passwordPageSubmitStatusCode = submitPassword(passwordPageInputs, userNamePageMetas, account.username, account.password)
        
        return validateSubmitStatusCode(passwordPageSubmitStatusCode)
    }
    
    private suspend fun fetchUserNamePage(): String {
        val request = UserNamePageRequest()
        return httpClient.send(request).html
    }
    
    private suspend fun submitUserName(
        htmlInputs: List<HTMLInput>,
        htmlMetas: List<HTMLMeta>,
        username: String
    ): String {
        val filteredMetas = htmlMetas
            .filter { it.name == "csrf-token" }
            .map { HTMLMeta("X-CSRF-Token", it.content) }
        
        val injectedInputs = inject(htmlInputs, username, "")
        val request = UserNameSubmitRequest(injectedInputs, filteredMetas)
        
        return httpClient.send(request).html
    }
    
    private suspend fun submitPassword(
        htmlInputs: List<HTMLInput>,
        htmlMetas: List<HTMLMeta>,
        username: String,
        password: String
    ): Int {
        val filteredMetas = htmlMetas
            .filter { it.name == "csrf-token" }
            .map { HTMLMeta("X-CSRF-Token", it.content) }
        
        val injectedInputs = inject(htmlInputs, username, password)
        val request = PasswordSubmitRequest(injectedInputs, filteredMetas)
        
        return httpClient.send(request).statusCode
    }
    
    private suspend fun fetchAuthorizationMethodSelectionPage(): String {
        val request = AuthorizationMethodSelectionPageRequest()
        return httpClient.send(request).html
    }
    
    private suspend fun submitTOTP(
        htmlInputs: List<HTMLInput>,
        htmlMetas: List<HTMLMeta>,
        account: ScienceTokyoPortalAccount
    ): String {
        val filteredMetas = htmlMetas
            .filter { it.name == "csrf-token" }
            .map { HTMLMeta("X-CSRF-Token", it.content) }
        
        val accountTotp = account.totpSecret
            ?: throw ScienceTokyoPortalLoginError.InvalidUserNamePage
        
        val otp = calculateTOTP(accountTotp, Date())
        val injectedInputs = inject(htmlInputs, otp, "")
        val request = OTPSubmitRequest(injectedInputs, filteredMetas)
        
        return httpClient.send(request).html
    }
    
    private suspend fun fetchWaitingPage(url: String): String {
        val request = WaitingPageRequest(url)
        return httpClient.send(request).html
    }
    
    private suspend fun fetchResourceListPage(
        htmlInputs: List<HTMLInput>,
        referer: String
    ): String {
        val request = ResourceListPageRequest(htmlInputs, referer)
        return httpClient.send(request).html
    }
    
    private suspend fun fetchLMSPage(): String {
        val request = LMSPageRequest()
        return httpClient.send(request).html
    }
    
    private suspend fun fetchLMSRedirectPage(htmlInputs: List<HTMLInput>): String {
        val request = LMSRedirectPageRequest(htmlInputs, emptyList())
        return httpClient.send(request).html
    }
    
    private suspend fun fetchLMSTokenPage(): Pair<String, String?> {
        val request = LMSTokenRequest()
        val response = httpClient.send(request)
        return response.html to response.responseUrl
    }
    
    fun validateUserNamePage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val bodyHtml = doc.body().html()
        
        return bodyHtml.contains("Please set your e-mail address for password reissue to an e-mail other than m.isct.ac.jp.") ||
                bodyHtml.contains("パスワード再発行用メールアドレスをm.isct.ac.jp以外のメールアドレスに忘れず必ず設定してください。")
    }
    
    fun validateUserNamePageSubmitJson(json: String, account: ScienceTokyoPortalAccount): Boolean {
        return try {
            val jsonElement = Json.parseToJsonElement(json).jsonObject
            val password = jsonElement["password"]?.jsonPrimitive?.boolean ?: return false
            val username = jsonElement["identifier"]?.jsonPrimitive?.content ?: return false
            
            password && username == account.username
        } catch (e: Exception) {
            false
        }
    }

    fun validateSubmitScript(script: String): Boolean {
        val regex = Regex("""window\.location\s*=\s*"(.*)"""")
        return regex.containsMatchIn(script)
    }
    
    fun validateSubmitStatusCode(code: Int): Boolean {
        return code in 200..299
    }
    
    fun validateMethodSelectionPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val bodyHtml = doc.body().html()
        
        return bodyHtml.contains("Please select an authentication method.") ||
                bodyHtml.contains("認証方法を選択してください。")
    }
    
    fun validateWaitingPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val bodyHtml = doc.body().html()
        
        return bodyHtml.contains("Please wait for a moment") ||
                bodyHtml.contains("しばらくお待ちください。")
    }
    
    fun validateResourceListPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val bodyHtml = doc.body().html()
        
        return bodyHtml.contains("Account") || bodyHtml.contains("アカウント")
    }
    
    fun validateLMSPage(cookies: List<HttpCookie>): Boolean {
        return cookies.any { it.name == "MoodleSession" }
    }
    
    fun detectPolicyError(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val title = doc.title()
        
        return title.contains("ポリシー") || title.contains("Policies")
    }
    
    fun validateLMSRedirectPage(html: String): Boolean {
        val doc = Jsoup.parse(html)
        val bodyHtml = doc.body().html()
        
        return bodyHtml.contains("ダッシュボード") || bodyHtml.contains("Dashboard")
    }
    
    fun extractHTML(html: String, cssSelector: String): String {
        val doc = Jsoup.parse(html)
        return doc.select(cssSelector).first()?.html() ?: ""
    }
    
    fun parseHTMLInput(html: String): List<HTMLInput> {
        val doc = Jsoup.parse(html)
        
        return doc.select("input").map { element ->
            HTMLInput(
                name = element.attr("name"),
                type = HTMLInputType.fromValue(element.attr("type")),
                value = element.attr("value")
            )
        }
    }
    
    fun parseHTMLSelect(html: String): List<HTMLSelect> {
        val doc = Jsoup.parse(html)
        
        return doc.select("select").map { element ->
            HTMLSelect(
                name = element.attr("name"),
                values = element.select("option").map { it.attr("value") }
            )
        }
    }
    
    fun parseHTMLMeta(html: String): List<HTMLMeta> {
        val doc = Jsoup.parse(html)
        
        return doc.select("meta").map { element ->
            val name = when {
                element.hasAttr("name") -> element.attr("name")
                element.hasAttr("http-equiv") -> element.attr("http-equiv")
                element.hasAttr("charset") -> "charset"
                else -> ""
            }
            
            val content = when {
                element.hasAttr("content") -> element.attr("content")
                element.hasAttr("charset") -> element.attr("charset")
                else -> ""
            }
            
            HTMLMeta(name = name, content = content)
        }
    }
    
    fun parseScriptToURL(script: String): String {
        val components = script.split("\"")
        return if (components.size > 1) components[1] else ""
    }
    
    fun inject(inputs: List<HTMLInput>, username: String, password: String): List<HTMLInput> {
        val mutableInputs = inputs.toMutableList()
        
        val firstTextInput = mutableInputs.firstOrNull { it.type == HTMLInputType.TEXT }
        if (firstTextInput != null) {
            val index = mutableInputs.indexOf(firstTextInput)
            mutableInputs[index] = firstTextInput.copy(value = username)
        }
        
        val firstPasswordInput = mutableInputs.firstOrNull { it.type == HTMLInputType.PASSWORD }
        if (firstPasswordInput != null) {
            val index = mutableInputs.indexOf(firstPasswordInput)
            mutableInputs[index] = firstPasswordInput.copy(value = password)
        }
        
        return mutableInputs
    }
}