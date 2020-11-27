package com.gin.pixivmanager2.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 列表相关工具类
 *
 * @author bx002
 * @date 2020/11/27 10:03
 */
public class ListUtils {
    public static <T> List<List<T>> spiltList(List<T> list, int length) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += length) {
            out.add(list.subList(i, Math.min(list.size(), i + length)));
        }
        return out;
    }
}
