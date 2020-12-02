package com.gin.pixivmanager2.util;

import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 编码解码工具类
 *
 * @author bx002
 * @date 2020/12/2 9:13
 */
public class CodeUtils {
    /**
     * 解码
     *
     * @param s   待解码字符串
     * @param enc 编码格式 默认utf nga gbk
     * @return 解码完成字符串
     */
    public static String decode(String s, String enc) {
        String encode = null;
        enc = StringUtils.isEmpty(enc) ? StandardCharsets.UTF_8.toString() : enc;
        try {
            encode = URLDecoder
                    .decode(s, enc);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }

    /**
     * 编码
     *
     * @param s   待编码字符串
     * @param enc 编码格式 默认utf nga gbk
     * @return 编码完成字符串
     */
    public static String encode(String s, String enc) {
        String encode = null;
        enc = StringUtils.isEmpty(enc) ? StandardCharsets.UTF_8.toString() : enc;
        try {
            encode = URLEncoder
                    .encode(s, enc)
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }
}
