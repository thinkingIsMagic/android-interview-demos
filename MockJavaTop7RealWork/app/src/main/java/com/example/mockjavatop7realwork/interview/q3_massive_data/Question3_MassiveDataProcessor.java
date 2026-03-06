package com.example.mockjavatop7realwork.interview.q3_massive_data;

import java.io.*;
import java.util.*;

/**
 * 面试题3：海量数据 - 找出两个文件中相同的URL
 *
 * 题目描述：
 * 两个文件里存了50亿条URL，总共大小320G，内存限制4G
 * 如何找出两个文件中相同的URL？
 *
 * 思路提示：
 * - 分治：将大文件拆分成小文件
 * - Hash：通过hash函数将相同URL映射到同一文件
 * - 布隆过滤器：空间极致优化（可选方案）
 *
 * 候选人需要实现：
 * 1. Hash分治拆分方法
 * 2. 内存中查找相同URL
 * 3. 布隆过滤器方案（可选）
 */
public class Question3_MassiveDataProcessor {

    // 配置参数
    private static final int SPLIT_COUNT = 200;  // 拆分文件数量
    private static final long MEMORY_LIMIT = 4L * 1024 * 1024 * 1024; // 4GB

    // 布隆过滤器参数
    private static final double FALSE_POSITIVE_RATE = 0.01; // 误判率1%
    private static final int EXPECTED_URL_COUNT = 5_000_000_0; // 50亿

    /**
     * 候选人请实现：Hash分治 - 将大文件拆分
     *
     * 思路：
     * 1. 遍历原文件A中的每条URL
     * 2. 计算 hash(URL) % SPLIT_COUNT
     * 3. 根据余数将URL写入对应的小文件
     *
     * @param inputFilePath 输入文件路径
     * @param outputDir 输出目录
     * @param filePrefix 输出文件前缀
     */
    public void splitFileByHash(String inputFilePath, String outputDir, String filePrefix) {
        // TODO: 实现文件拆分逻辑
        // 提示：
        // 1. 创建 SPLIT_COUNT 个输出文件
        // 2. 逐行读取输入文件
        // 3. 计算 hash(url) % SPLIT_COUNT
        // 4. 写入对应的输出文件
    }

    /**
     * 候选人请实现：逐个对比小文件，找出相同URL
     *
     * 思路：
     * 1. 读取小文件a_i，使用HashSet存入内存
     * 2. 读取小文件b_i，检查每个URL是否在HashSet中
     * 3. 存在的即为重复URL，记录到结果
     *
     * @param dirA 文件A拆分后的目录
     * @param dirB 文件B拆分后的目录
     * @param outputFile 结果输出文件
     */
    public void findCommonUrls(String dirA, String dirB, String outputFile) {
        // TODO: 实现对比逻辑
        // 提示：
        // 1. 循环处理每一对文件 (0 ~ SPLIT_COUNT-1)
        // 2. 读取dirA中的文件a_i，构建HashSet
        // 3. 遍历dirB中的文件b_i
        // 4. 检查是否存在于HashSet，存在则写入结果
        // 5. 处理完一对后清空HashSet，释放内存
    }

    // ============================================================
    // 候选人可选实现：布隆过滤器方案
    // ============================================================
    public static class BloomFilter {

        private BitSet bitSet;
        private int size;
        private int hashCount;

        /**
         * TODO: 初始化布隆过滤器
         *
         * @param expectedNumber 期望存储的元素数量
         * @param falsePositiveRate 期望的误判率
         */
        public BloomFilter(int expectedNumber, double falsePositiveRate) {
            // TODO: 初始化
            // 公式：size = -n * ln(p) / (ln(2)^2)
            // 公式：hashCount = (size / n) * ln(2)
        }

        /**
         * TODO: 添加元素到布隆过滤器
         *
         * @param value 要添加的值
         */
        public void add(String value) {
            // TODO: 使用多个hash函数计算位置并设置bit
        }

        /**
         * TODO: 检查元素是否可能存在
         *
         * @param value 要检查的值
         * @return 可能存在(true)或一定不存在(false)
         */
        public boolean mightContain(String value) {
            // TODO: 检查所有hash位置是否都被设置
            return false;
        }

        /**
         * 获取位数组大小
         */
        public int getSize() {
            return size;
        }
    }

    /**
     * 候选人可选实现：使用布隆过滤器查找相同URL
     *
     * 优点：极省内存，适合内存极度受限的场景
     * 缺点：存在极小概率误判
     *
     * @param fileAPath 文件A路径
     * @param fileBPath 文件B路径
     * @param resultFile 结果文件
     */
    public void findCommonUrlsWithBloomFilter(String fileAPath, String fileBPath,
                                               String resultFile) {
        // TODO: 使用布隆过滤器实现
        // 提示：
        // 1. 创建布隆过滤器
        // 2. 遍历文件A，将所有URL加入布隆过滤器
        // 3. 遍历文件B，检查每个URL是否可能存在
        // 4. 可能存在的写入待验证结果
        // 5. 可选：对结果进行二次验证
    }

    // ============================================================
    // 工具方法
    // ============================================================

    /**
     * 计算字符串的hash值
     */
    private int hash(String url) {
        // 使用Java的hashCode，可自行优化
        return Math.abs(url.hashCode());
    }

    /**
     * 多hash函数计算
     */
    private int[] getHashPositions(String value, int hashCount, int size) {
        int[] positions = new int[hashCount];
        int hash1 = value.hashCode();
        int hash2 = hash1 >>> 16;

        for (int i = 0; i < hashCount; i++) {
            positions[i] = Math.abs((hash1 + i * hash2) % size);
        }
        return positions;
    }

    // ==================== 测试代码 ====================
    public static void main(String[] args) {
        System.out.println("=== 面试题3：海量数据处理 ===");
        System.out.println("场景：两个文件50亿条URL，总大小320G，内存4G");
        System.out.println();

        System.out.println("方案1：Hash分治法");
        System.out.println("  - 将文件拆分成200个小文件");
        System.out.println("  - 逐对对比，内存占用约1.6G/对");

        System.out.println();
        System.out.println("方案2：布隆过滤器（可选）");
        System.out.println("  - 空间极致优化");
        System.out.println("  - 存在极小概率误判");

        // 测试布隆过滤器
        System.out.println();
        System.out.println("--- 布隆过滤器测试 ---");
        BloomFilter filter = new BloomFilter(1000, 0.01);
        filter.add("test-url-1");
        filter.add("test-url-2");

        System.out.println("查找test-url-1: " + filter.mightContain("test-url-1")); // true
        System.out.println("查找test-url-999: " + filter.mightContain("test-url-999")); // false
    }
}
