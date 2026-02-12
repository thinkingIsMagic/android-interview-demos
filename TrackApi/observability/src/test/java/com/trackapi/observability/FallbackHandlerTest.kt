/*
 * FallbackHandlerTest.kt
 *
 * FallbackHandler 单元测试
 *
 * 面试亮点：
 * - 测试异常兜底机制
 * - 测试自我保护能力
 */
package com.trackapi.observability

import org.junit.Assert.*
import org.junit.Test

/**
 * FallbackHandler 单元测试
 */
class FallbackHandlerTest {

    @Test
    fun `safeExecute should return result on success`() {
        val result = FallbackHandler.safeExecute {
            42
        }

        assertEquals("Should return the result", 42, result)
    }

    @Test
    fun `safeExecute should return null on exception`() {
        val result = FallbackHandler.safeExecute {
            throw RuntimeException("Test exception")
        }

        assertNull("Should return null on exception", result)
    }

    @Test
    fun `safeExecute without return should handle exception`() {
        var executed = false
        var exceptionCaught = false

        FallbackHandler.safeExecute {
            executed = true
            throw RuntimeException("Test")
        }

        assertTrue("Code should have been executed", executed)
        // Should not throw, exception is caught internally
    }

    @Test
    fun `safeExecute with error should not throw`() {
        assertDoesNotThrow {
            FallbackHandler.safeExecute {
                throw Error("VirtualMachineError")
            }
        }
    }
}
