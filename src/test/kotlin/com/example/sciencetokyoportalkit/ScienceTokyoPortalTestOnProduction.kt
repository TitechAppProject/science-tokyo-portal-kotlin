package app.titech.sciencetokyoportalkit

import app.titech.sciencetokyoportalkit.model.ScienceTokyoPortalAccount
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class ScienceTokyoPortalTestOnProduction {
    @Test
    fun login() {
        val portal = ScienceTokyoPortal()
        val account = ScienceTokyoPortalAccount(
            username = "sss",
            password = "aa",
            totpSecret =""
        )
        runBlocking {
            try {
                portal.login(account)
                portal.getLMSDashboard()
                val token = portal.getLMSToken()
                println(token)
            } catch (e: Exception) {
                println(e)
            }
        }
    }

    @Test
    fun checkUsernamePassword() {
        val portal = ScienceTokyoPortal()
        runBlocking {
            try {
                val bool = portal.checkUsernamePassword("account", "password")
                println(bool)
            } catch (e: Exception) {
                println(e)
            }
        }
    }
}