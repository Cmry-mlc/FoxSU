package com.loomsu.manager.domain.text

fun interface TextTransliterator {
    fun transliterate(value: String): String
}
