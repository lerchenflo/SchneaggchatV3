package org.lerchenflo.schneaggchatv3mp

val BASE_SERVER_URL = "https://schneaggchat.lerchenflo.eu"

val BASE_SERVER_URL_TEST = "https://schneaggchattest.lerchenflo.eu"

val TEST_ACCOUNT_USERNAME = "testaccount"

val SUPPORT_EMAIL = "schneaggchat@gmail.com"

fun getPrivacyPolicyUrl(baseUrl: String) = "$baseUrl/privacypolicy.html"

fun getDeleteAccountUrl(baseUrl: String) = "$baseUrl/delete_account.html"

fun getPasswordResetUrl(baseUrl: String) = "$baseUrl/reset_password.html"

fun getFaqUrl(baseUrl: String) = "$baseUrl/faq.html"

fun getDonationsUrl(baseUrl: String) = "$baseUrl/donations.html"

var URL_INSTAGRAM = "https://www.instagram.com/schneaggchat?utm_source=ig_web_button_share_sheet&igsh=ZDNlZDc0MzIxNw=="

val GITHUB_URL = "https://raw.githubusercontent.com/lerchenflo/SchneaggchatV3"


val PLAYSTORE_TESTER_URI = "https://play.google.com/apps/testing/org.lerchenflo.schneaggchatv3mp"
val APPSTORE_TESTER_URI = "https://testflight.apple.com/join/XkUjnst6"

var USERPROFILEPICTURE_FILE_NAME = ".upb"

var GROUPPROFILEPICTURE_FILE_NAME = ".gpb"


var PICTURE_FILE_NAME = "_bild.drawable"

var VOICEMSG_FILE_NAME = "_voicemsg.m4a"

/** File endings of every picture the app stores: user and group profile pictures, image messages. */
val STORED_PICTURE_FILE_ENDINGS: List<String>
    get() = listOf(USERPROFILEPICTURE_FILE_NAME, GROUPPROFILEPICTURE_FILE_NAME, PICTURE_FILE_NAME)

val MIN_GROUPNAME_LENGTH = 3
val MAX_GROUPNAME_LENGTH = 25



