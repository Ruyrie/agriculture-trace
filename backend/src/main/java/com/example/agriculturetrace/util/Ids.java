package com.example.agriculturetrace.util;

import java.util.UUID;

/**
 * 主键生成工具。
 *
 * 需求要求主键为 varchar(32)，因此统一使用去横线 UUID。
 */
public final class Ids {

    private Ids() {
    }

    public static String uuid32() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
