package com.sismics.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码工具类。
 *
 * @author Scott-Einstein
 */
public class PasswordUtil {

    /**
     * 哈希密码。
     *
     * @param password 原始密码
     * @return 哈希后的密码
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 验证密码。
     *
     * @param password 原始密码
     * @param hashedPassword 哈希后的密码
     * @return 密码是否匹配
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}