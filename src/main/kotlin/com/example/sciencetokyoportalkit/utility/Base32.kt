package com.example.sciencetokyoportalkit.utility

fun base32Decode(base32String: String): ByteArray? {
    val base32Alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    val lookup = mutableMapOf<Char, Byte>()
    base32Alphabet.forEachIndexed { index, c ->
        lookup[c] = index.toByte()
    }

    val cleanedString = base32String
        .replace("=", "")
        .replace(" ", "")
        .uppercase()

    var buffer = 0L
    var bitsLeft = 0
    val result = mutableListOf<Byte>()

    for (char in cleanedString) {
        val value = lookup[char] ?: return null

        buffer = (buffer shl 5) or value.toLong()
        bitsLeft += 5

        if (bitsLeft >= 8) {
            bitsLeft -= 8
            val byte = ((buffer shr bitsLeft) and 0xFF).toByte()
            result.add(byte)
        }
    }

    return result.toByteArray()
}