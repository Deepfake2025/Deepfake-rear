package com.basis.utils;

import java.util.UUID;

public class UsernameUtil {
    
    /**
     * 基于UUID生成唯一的用户名
     * @return 生成的用户名字符串
     */
    public static String generateUsernameFromUUID() {
        // 生成UUID并去掉其中的横线，取前8位作为用户名
        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
    
    /**
     * 基于用户邮箱和时间戳生成用户名
     * @param email 用户邮箱
     * @return 生成的用户名字符串
     */
    public static String generateUsernameFromEmail(String email) {
        // 使用邮箱前缀+时间戳的方式生成用户名
        String emailPrefix = email.split("@")[0];
        return emailPrefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}