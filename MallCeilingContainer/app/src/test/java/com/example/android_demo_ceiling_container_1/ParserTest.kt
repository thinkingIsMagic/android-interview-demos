package com.example.android_demo_ceiling_container_1

import com.example.android_demo_ceiling_container_1.data.mock.MockConfigDataSource
import com.example.android_demo_ceiling_container_1.data.model.BottomConfig
import com.example.android_demo_ceiling_container_1.data.model.ComponentType
import com.example.android_demo_ceiling_container_1.data.parser.BottomConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BottomConfigParser 单元测试
 */
class ParserTest {

    private lateinit var parser: BottomConfigParser

    @Before
    fun setup() {
        parser = BottomConfigParser()
    }

    @Test
    fun `parse default mock json should return success`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue("解析应该成功", result.isSuccess)
        result.onSuccess { config ->
            assertEquals("1.0", config.version)
            assertTrue("应该启用", config.enable)
        }
    }

    @Test
    fun `parse should return 3 components`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            assertEquals("组件数量应该是 3", 3, config.components.size)
        }
    }

    @Test
    fun `parse coupon component should have correct values`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val coupon = config.components.find { it.type == ComponentType.COUPON }
            assertNotNull("应该存在 Coupon 组件", coupon)
            coupon?.let {
                assertEquals("coupon_001", it.id)
                assertEquals(100, it.priority)
                assertEquals("product_detail", it.scene)
                assertTrue("应该启用", it.enable)
            }
        }
    }

    @Test
    fun `parse banner component should have correct values`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val banner = config.components.find { it.type == ComponentType.BANNER }
            assertNotNull("应该存在 Banner 组件", banner)
            banner?.let {
                assertEquals("banner_001", it.id)
                assertEquals(80, it.priority)
            }
        }
    }

    @Test
    fun `parse floating widget component should have correct values`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val floating = config.components.find { it.type == ComponentType.FLOATING_WIDGET }
            assertNotNull("应该存在 Floating Widget 组件", floating)
            floating?.let {
                assertEquals("float_001", it.id)
                assertEquals(60, it.priority)
            }
        }
    }

    @Test
    fun `components should be sorted by priority descending`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val sorted = config.getSortedComponents()
            assertTrue("应该按优先级降序排列", sorted[0].priority >= sorted[1].priority)
            assertTrue("应该按优先级降序排列", sorted[1].priority >= sorted[2].priority)
        }
    }

    @Test
    fun `parse disabled config should return enable false`() {
        val json = MockConfigDataSource.getDisabledConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            assertFalse("应该禁用", config.enable)
        }
    }

    @Test
    fun `parse empty components should return empty list`() {
        val json = MockConfigDataSource.getEmptyComponentsConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            assertTrue("组件列表应该为空", config.components.isEmpty())
        }
    }

    @Test
    fun `parse scroll threshold config should have visibility settings`() {
        val json = MockConfigDataSource.getScrollThresholdConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val component = config.components.firstOrNull()
            assertNotNull(component)
            component?.let {
                assertEquals(150, it.visibility.scrollThreshold)
                assertEquals("down", it.visibility.scrollDirection)
            }
        }
    }

    @Test
    fun `generate mock json should be parseable`() {
        val json = parser.generateMockJson()
        val result = parser.parse(json)

        assertTrue("生成的 Mock JSON 应该可解析", result.isSuccess)
    }

    @Test
    fun `getEnabledComponents should filter correctly`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            val enabled = config.getEnabledComponents()
            assertEquals("所有 Mock 组件都启用，数量为 3", 3, enabled.size)
        }
    }

    @Test
    fun `isValid should return true for valid config`() {
        val json = MockConfigDataSource.getDefaultConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            assertTrue("配置应该有效", config.isValid())
        }
    }

    @Test
    fun `isValid should return false for empty components`() {
        val json = MockConfigDataSource.getEmptyComponentsConfig()
        val result = parser.parse(json)

        assertTrue(result.isSuccess)
        result.onSuccess { config ->
            assertFalse("空组件配置应该无效", config.isValid())
        }
    }
}
