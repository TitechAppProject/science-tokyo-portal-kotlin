package app.titech.sciencetokyoportalkit.utility

import java.nio.ByteBuffer
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

class TOTPError : Exception("Invalid Base32")

fun calculateTOTP(secret: String, current: Date): String {
    val unixTime = current.time / 1000
    val period = 30L
    val time = unixTime / period
    
    val timeBuffer = ByteBuffer.allocate(8)
    timeBuffer.putLong(time)
    val timeData = timeBuffer.array()
    
    val digits = 6
    val secretKey = base32Decode(secret) ?: throw TOTPError()
    
    val mac = Mac.getInstance("HmacSHA1")
    val keySpec = SecretKeySpec(secretKey, "HmacSHA1")
    mac.init(keySpec)
    val hash = mac.doFinal(timeData)
    
    val offset = hash[hash.size - 1].toInt() and 0x0f
    
    var truncatedHash = 0
    for (i in 0..3) {
        truncatedHash = truncatedHash shl 8
        truncatedHash = truncatedHash or (hash[offset + i].toInt() and 0xFF)
    }
    
    truncatedHash = truncatedHash and 0x7FFFFFFF
    truncatedHash = truncatedHash % 10.0.pow(digits).toInt()
    
    return String.format("%06d", truncatedHash)
}