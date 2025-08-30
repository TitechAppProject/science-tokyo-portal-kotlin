package app.titech.sciencetokyoportalkit.model

data class ScienceTokyoPortalAccount(
    val username: String,
    val password: String,
    val totpSecret: String? = null
)