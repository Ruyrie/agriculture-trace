package com.example.agriculturetrace.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 哈希工具，用于模拟区块链数据指纹。
 */
public final class HashUtil {

    private HashUtil() {
    }

    /**
     * 计算输入字符串的 SHA-256 十六进制摘要。
     * null 会按空字符串处理，确保哈希调用方不用重复做空值判断。
     */
    public static String sha256(String input) {
        try {
            // 获取 JVM 内置的 SHA-256 算法实例；JDK 8+ 必然支持，不存在时说明运行环境异常。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 将字符串转为 UTF-8 字节数组后一次性计算哈希，得到固定长度（32字节/256位）的摘要。
            // null 统一视为空字符串，保证调用方无需做空值判断。
            byte[] hashed = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            // SHA-256 输出 32 字节，转十六进制需要 64 个字符，预分配好容量避免扩容。
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                // %02x：按两位小写十六进制格式化，不足两位时左补 '0'，保证输出始终是 64 位定长字符串。
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JDK 必须提供的算法，正常不会到这里；
            // 一旦触发，包装成 IllegalStateException 避免强迫调用方声明 checked 异常。
            throw new IllegalStateException("SHA-256算法不可用", e);
        }
    }
}
