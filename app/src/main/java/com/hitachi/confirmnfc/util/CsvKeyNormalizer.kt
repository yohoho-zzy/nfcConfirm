package com.hitachi.confirmnfc.util

import java.util.Locale

object CsvKeyNormalizer {
    fun normalize(value: String): String {
        return value.trim()
            .replace(Regex("[^0-9A-Fa-f]"), "")
            .uppercase(Locale.US)
    }
}
