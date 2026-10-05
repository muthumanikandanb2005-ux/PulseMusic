package com.pulse.music.ui.screen.profile

data class IndianLanguageInfo(
    val id: String,
    val englishName: String,
    val nativeScript: String,
)

val INDIAN_LANGUAGES = listOf(
    IndianLanguageInfo("ta", "Tamil", "தமிழ்"),
    IndianLanguageInfo("te", "Telugu", "తెలుగు"),
    IndianLanguageInfo("hi", "Hindi", "हिन्दी"),
    IndianLanguageInfo("ml", "Malayalam", "മലയാളം"),
    IndianLanguageInfo("kn", "Kannada", "ಕನ್ನಡ"),
    IndianLanguageInfo("bn", "Bengali", "বাংলা"),
    IndianLanguageInfo("mr", "Marathi", "मराठी"),
    IndianLanguageInfo("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    IndianLanguageInfo("gu", "Gujarati", "ગુજરાતી"),
    IndianLanguageInfo("or", "Odia", "ଓଡ଼ିଆ"),
    IndianLanguageInfo("as", "Assamese", "অসমীয়া"),
    IndianLanguageInfo("en_in", "English (India)", "English"),
)

val GENDER_OPTIONS = listOf(
    "Male",
    "Female",
    "Non-binary",
    "Prefer not to say",
)
