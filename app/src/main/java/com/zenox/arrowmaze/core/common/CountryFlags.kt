package com.zenox.arrowmaze.core.common

/**
 * Country-flag helpers shared across feature screens (Leaderboard, Friends,
 * Profile). Converts an ISO-2 country code into the corresponding regional
 * indicator symbol pair so it renders as an emoji flag on every platform.
 */
object CountryFlags {

    /** Returns the flag emoji for [countryCode]; "🏳️" if the code is invalid. */
    fun emoji(countryCode: String?): String {
        if (countryCode == null) return "🏳️"
        val upper = countryCode.uppercase()
        if (upper.length != 2) return "🏳️"
        val c1 = upper[0]
        val c2 = upper[1]
        if (c1 !in 'A'..'Z' || c2 !in 'A'..'Z') return "🏳️"
        val base = 0x1F1E6 - 'A'.code
        val sb = StringBuilder(2)
        sb.appendCodePoint(base + c1.code)
        sb.appendCodePoint(base + c2.code)
        return sb.toString()
    }
}

/** Top-level convenience alias for [CountryFlags.emoji]. */
fun flagForCountry(countryCode: String?): String = CountryFlags.emoji(countryCode)
