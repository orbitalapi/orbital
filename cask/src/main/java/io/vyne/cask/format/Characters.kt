package io.vyne.cask.format

object Characters {
    const val NULL_CHARACTER = '\u0000';
}

fun String.padToLength(length: Int): String {
    if (this.length > length) error("String exceeds max length")
    return this.padEnd(length, Characters.NULL_CHARACTER)
}

fun String.byteArrayOfLength(length: Int): ByteArray {
    val result = this.padToLength(length).toByteArray()
    require(result.size == length) {"Padded byteArray had wrong length of ${result.size}, where expected $length"}
    return result
}

fun String.unPad(): String {
    return this.substringBefore(Characters.NULL_CHARACTER)
}
