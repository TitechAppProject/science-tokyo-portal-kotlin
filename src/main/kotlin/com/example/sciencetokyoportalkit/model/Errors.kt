package com.example.sciencetokyoportalkit.model

sealed class ScienceTokyoPortalLoginError : Exception() {
    object InvalidUserNamePage : ScienceTokyoPortalLoginError()
    object InvalidPasswordPage : ScienceTokyoPortalLoginError()
    object InvalidMethodSelectionPage : ScienceTokyoPortalLoginError()
    object InvalidTOTPPage : ScienceTokyoPortalLoginError()
    object InvalidWaitingPage : ScienceTokyoPortalLoginError()
    object InvalidResourceListPage : ScienceTokyoPortalLoginError()
    object AlreadyLoggedIn : ScienceTokyoPortalLoginError()
}

sealed class LMSLoginError : Exception() {
    object Policy : LMSLoginError()
    object InvalidDashboardPage : LMSLoginError()
    object ParseHtml : LMSLoginError()
    data class ParseUrlScheme(val responseHTML: String, val responseUrl: String?) : LMSLoginError()
    data class ParseToken(val responseHTML: String, val responseUrl: String?) : LMSLoginError()
}