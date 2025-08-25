package com.example.sciencetokyoportalkit.utility

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.*
import kotlin.test.assertEquals

class TOTPCalculatorTest {
    
    @Test
    fun `test valid secret`() {
        // Swift's Date(timeIntervalSinceReferenceDate: 0) corresponds to 2001-01-01 00:00:00 UTC
        // This is 978307200 seconds after Unix epoch
        val swiftReferenceDate = Date(978307200000L) // 2001-01-01 00:00:00 UTC
        val result = calculateTOTP("AAA", swiftReferenceDate)
        val expected = "877465"
        
        assertEquals(expected, result)
    }
    
    @Test
    fun `test invalid secret`() {
        val referenceDate = Date(978307200000L)
        
        assertThrows<TOTPError> {
            calculateTOTP("1", referenceDate)
        }
    }
}