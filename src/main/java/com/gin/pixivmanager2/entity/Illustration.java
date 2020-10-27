package com.gin.pixivmanager2.entity;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author bx002
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@TableName(value = "t_illustration")
public class Illustration {
    public static Map<String, String> dic;
    /**
     * 插画
     */
    final static int ILLUST_TYPE_ILLUSTRATION = 0;
    /**
     * 漫画
     */
    final static int ILLUST_TYPE_MANGA = 1;
    /**
     * 动图
     */
    final static int ILLUST_TYPE_GIF = 2;

    final static String[] USERNAME_TRASH = new String[]{"@", "＠", "|", "FANBOX", "fanbox", "仕事", "■"};
    final static Map<String, String> ILLEGAL_CHAR = new HashMap<>();

    static {
        ILLEGAL_CHAR.put(":", "：");
        ILLEGAL_CHAR.put("\n", "");
        ILLEGAL_CHAR.put("?", "？");
        ILLEGAL_CHAR.put("<", "《");
        ILLEGAL_CHAR.put(">", "》");
        ILLEGAL_CHAR.put("*", "×");
        ILLEGAL_CHAR.put("|", "^");
        ILLEGAL_CHAR.put("\"", "“");
        ILLEGAL_CHAR.put("\\", "_");
        ILLEGAL_CHAR.put("/", "~");
    }


    /**
     * 作品id pid
     */
    private String id;
    /**
     * 作者id
     */
    @TableField("userId")
    private String userId;
    /**
     * 作者名
     */
    @TableField("userName")
    private String userName;
    @TableField("lastUpdate")
    Long lastUpdate;
    /**
     * 标题
     */
    String title;
    /**
     * 描述
     */
    String description;
    /**
     * 字符串格式的tag列表
     */
    String tag;
    /**
     * 字符串格式的tag对应翻译列表
     */
    @TableField("tagTranslated")
    String tagTranslated;
    /**
     * url前缀
     */
    @TableField("urlPrefix")
    String urlPrefix;
    /**
     * 文件名
     */
    @TableField("fileName")
    String fileName;
    /**
     * 作品数量
     */
    @TableField("pageCount")
    Integer pageCount;
    @TableField("illustType")
    Integer illustType;
    /**
     * 收藏数
     */
    @TableField("bookmarkCount")
    Integer bookmarkCount;
    /**
     * 是否已收藏
     */
    @TableField("bookmarkData")
    Integer bookmarkData;

    public static Illustration parse(JSONObject body) {
        Illustration ill = new Illustration();

        String userName = body.getString("userName");
        for (String s : USERNAME_TRASH) {
            if (userName.contains(s)) {
                userName = userName.substring(0, userName.indexOf(s));
            }
        }
        ill
                .setId(body.getString("id"))
                .setUserId(body.getString("userId"))
                .setUserName(userName)
                .setTitle(body.getString("title"))
                .setBookmarkCount(body.getInteger("bookmarkCount"))
                .setPageCount(body.getInteger("pageCount"))
                .setIllustType(body.getInteger("illustType"))
                .setBookmarkData(body.get("bookmarkData") != null ? 1 : 0)
                .setLastUpdate(System.currentTimeMillis())
        ;
        String description = body.getString("description");
        ill.setDescription(description != null ? description.substring(0, Math.min(4000, description.length())) : null);

        JSONObject urlsJson = body.getJSONObject("urls");
        if (urlsJson != null) {
            String original = urlsJson.getString("original");

            if (ill.getIllustType() == ILLUST_TYPE_GIF) {
                original = original.replace("img-original", "img-zip-ugoira");
                original = original.substring(0, original.lastIndexOf("_")) + "_ugoira1920x1080.zip";
            }

            int endIndex = original.lastIndexOf("/") + 1;
            ill.setUrlPrefix(original.substring(0, endIndex))
                    .setFileName(original.substring(endIndex));
        }


        try {
            //解析tag
            StringBuilder tagBuilder = new StringBuilder();
            StringBuilder translationBuilder = new StringBuilder();
            JSONArray tagsArray = body.getJSONObject("tags").getJSONArray("tags");
            for (int i = 0; i < tagsArray.size(); i++) {
                JSONObject tag = tagsArray.getJSONObject(i);
                String tagString = tag.getString("tag");
                JSONObject trans = tag.getJSONObject("translation");

                tagBuilder.append(tagString).append(",");
                translationBuilder.append(trans != null ? trans.getString("en") : tagString).append(",");
            }
            ill.setTag(tagBuilder.substring(0, tagBuilder.length() - 1))
                    .setTagTranslated(translationBuilder.substring(0, translationBuilder.length() - 1));
        } catch (ClassCastException e) {
            //cast错误 说明是简短tags
            JSONArray tagsArray = body.getJSONArray("tags");
            StringBuilder tagBuilder = new StringBuilder();
            for (int i = 0; i < tagsArray.size(); i++) {
                tagBuilder.append(tagsArray.getString(i)).append(",");
            }
            ill.setTag(tagBuilder.toString());
        }
        return ill;
    }


    public List<String> getUrlList() {
        if (StringUtils.isEmpty(urlPrefix) || StringUtils.isEmpty(fileName)) {
            return null;
        }
        List<String> list = new ArrayList<>();
        if (illustType == ILLUST_TYPE_GIF) {
            list.add(urlPrefix + fileName);
        } else {
            for (int i = 0; i < pageCount; i++) {
                String name = fileName.replace("_p0", "_p" + i);
                list.add(urlPrefix + name);
            }
        }
        return list;
    }

    public String getTagString() {
        return Arrays.stream(tag.split(","))
                .map(s -> dic.getOrDefault(s.toLowerCase(), s).toLowerCase())
                .flatMap(s -> Arrays.stream(s.replace(")", "").split("\\(")))
                .distinct()
                .collect(Collectors.joining(","));
    }

    public String getFilePath(Integer count) {
        if (StringUtils.isEmpty(fileName)) {
            return null;
        }
        String sb = addBrackets("bmk", bookmarkCount) +
                addBrackets(id, "p" + count) +
                addBrackets("title", title) +
                addBrackets("tags", getTagString()) +
                fileName.substring(fileName.lastIndexOf("."));
        return clean(sb);
    }

    public String getAuthorPath() {
        return "/" + clean(addBrackets("u", userName)) + addBrackets("uid", userId) + "/";
    }

    public List<String> getFilePathList() {
        ArrayList<String> list = new ArrayList<>();
        for (Integer i = 0; i < pageCount; i++) {
            list.add(getFilePath(i));
        }
        return list;
    }

    private static String addBrackets(String title, Object content) {
        return "[" + title + "_" + content + "]";
    }

    public String getUrl() {
        return "https://www.pixiv.net/artworks/" + id;
    }

    public String getUrlAjax() {
        return "https://www.pixiv.net/ajax/illust/" + id;
    }

    private String clean(String s) {
        for (Map.Entry<String, String> entry : ILLEGAL_CHAR.entrySet()) {
            s = s.replace(entry.getKey(), entry.getValue());
        }
        return s;
    }
}
