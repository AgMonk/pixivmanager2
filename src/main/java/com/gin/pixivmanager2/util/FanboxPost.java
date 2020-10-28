package com.gin.pixivmanager2.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.entity.FanboxItem;

/**
 * fanbox请求
 */
public class FanboxPost {
    /**
     * 按作者名获取作品信息
     */
    private final static String LIST_CREATOR = "https://api.fanbox.cc/post.listCreator?limit=300&creatorId=";
    private final static String LIST_SUPPORTING = "https://api.fanbox.cc/post.listSupporting?limit=300";
    private final static String POST_ID = "https://api.fanbox.cc/post.info?postId=";
    private final static String REFERER = "https://www.fanbox.cc";

    public static JSONArray listCreator(String creatorId, String cookie) {
        return getArray(cookie, LIST_CREATOR + creatorId);
    }

    public static JSONArray listSupporting(String cookie) {
        return getArray(cookie, LIST_SUPPORTING);
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

    public static void main(String[] args) {
        String cookie = "p_ab_id=1; p_ab_id_2=3; p_ab_d_id=1331259489; _ga=GA1.2.453670194.1595292215; privacy_policy_agreement=2; FANBOXSESSID=57680761_6TbM7A4j9KZGvl702plY3YiM8QnVAmWw; _gid=GA1.2.2123714715.1603070156";
//        JSONArray array = listCreator("turisasu", cookie);
        JSONArray array = listSupporting(cookie);
        JSONObject jsonObject = array.getJSONObject(1);
//        JSONObject jsonObject = postId("1534199", cookie);
//        JSONObject jsonObject = postId("1524728", cookie);
//        JSONObject jsonObject = postId("1530491", cookie);
        FanboxItem item = new FanboxItem(jsonObject);

        System.err.println(item);

//        item.getUrlMap().forEach((s, s2) -> {
//            System.err.println(item.getParentPath() + "/" + s + " >> " + s2);
//        });
    }
}
