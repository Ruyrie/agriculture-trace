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
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((input == null ? "" : input).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256算法不可用", e);
        }
    }
}
