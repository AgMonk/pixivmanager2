package com.gin.pixivmanager2.util;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author bx002
 */
public class TimeUtil {
    public final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public final static ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    /**
     * 东八区当前时间
     *
     * @return ZonedDateTime
     */
    public static ZonedDateTime now() {
        return ZonedDateTime.now(ZONE_ID);
    }

    /**
     * 格式输出当前时间
     *
     * @return 当前时间
     */
    public static String nowString() {
        return now().format(FORMATTER);
    }

    /**
     * 指定格式输出当前时间
     *
     * @param pattern 格式
     * @return 当前时间
     */
    public static String nowString(String pattern) {
        return now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 当前毫秒数
     *
     * @return 毫秒
     */
    public static long nowMillis() {
        return now().toInstant().toEpochMilli();
    }

    /**
     * 当前秒数
     *
     * @return 秒数
     */
    public static long nowSeconds() {
        return now().toEpochSecond();
    }

    /**
     * 解析时间字符串为ZonedDateTime
     *
     * @param timeString 时间字符串
     * @return ZonedDateTime
     */
    public static ZonedDateTime parse(String timeString) {
        String t = "T";
        if (timeString.contains(t) && timeString.indexOf(t) == 10) {
            return ZonedDateTime.parse(timeString);
        }
        return ZonedDateTime.parse(timeString, FORMATTER);
    }


    /**
     * 解析时间字符串为毫秒
     *
     * @param timeString 时间字符串
     * @return long
     * @author bx002
     * @date 2020/11/9 12:29
     */
    public static long parseToMillis(String timeString) {
        return parse(timeString).toInstant().toEpochMilli();
    }

    /**
     * 解析时间字符串为秒
     *
     * @param timeString 时间字符串
     * @return long
     * @author bx002
     * @date 2020/11/9 12:31
     */
    public static long parseToSeconds(String timeString) {
        return parse(timeString).toEpochSecond();
    }

    public static void main(String[] args) {
        System.err.println(now());
        System.err.println(nowString());
        System.err.println(nowMillis());
        System.err.println(nowSeconds());
        System.err.println(parse("2020-11-09T14:48:02.835692600+08:00[Asia/Shanghai]"));
        System.err.println(parseToMillis("2020-11-09T14:48:02.835692600+08:00[Asia/Shanghai]"));
        System.err.println(parseToSeconds("2020-11-09T14:48:02.835692600+08:00[Asia/Shanghai]"));
    }
}
