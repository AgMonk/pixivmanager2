package com.gin.pixivmanager2.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则工具类
 *
 * @author bx002
 * @date 2020/12/3 11:38
 */
public class PatternUtils {
    public static String get(Pattern pattern, String input) {
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }
}
