package com.gin.pixivmanager2.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * fanbox请求
 */
public class FanboxPost {
    /**
     * 按作者名获取作品信息
     */
    private final static String LIST_CREATOR = "https://api.fanbox.cc/post.listCreator?limit=300&creatorId=";
    private final static String LIST_SUPPORTING = "https://api.fanbox.cc/post.listSupporting?limit=";
    private final static String POST_ID = "https://api.fanbox.cc/post.info?postId=";
    private final static String REFERER = "https://www.fanbox.cc";

    public static JSONArray listCreator(String creatorId, String cookie) {
        return getArray(cookie, LIST_CREATOR + creatorId);
    }

    public static JSONArray listSupporting(String cookie, Integer limit) {
        return getArray(cookie, LIST_SUPPORTING + limit);
    }

    private static JSONArray getArray(String cookie, String url) {
        String result = Request.create(url)
                .setCookie(cookie)
                .setReferer(REFERER)
                .setOrigin(REFERER)
                .get().getResult();
        if (result != null) {
            JSONObject json = JSONObject.parseObject(result);
            JSONObject body = json.getJSONObject("body");
            if (body != null) {
                return body.getJSONArray("items");
            }
        }
        return new JSONArray();
    }


    public static JSONObject postId(String postId, String cookie) {
        String url = POST_ID + postId;
        String result = Request.create(url)
                .setCookie(cookie)
                .setReferer(REFERER)
                .setOrigin(REFERER)
                .get().getResult();
        if (result != null) {
            JSONObject json = JSONObject.parseObject(result);
            JSONObject body = json.getJSONObject("body");
            if (body != null) {
                return body;
            }
        }
        return null;
    }

}
