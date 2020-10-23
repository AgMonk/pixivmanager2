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

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

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
    Integer downloaded;

    public static Illustration parse(JSONObject body) {
        Illustration ill = new Illustration();

        ill
                .setId(body.getString("id"))
                .setUserId(body.getString("userId"))
                .setUserName(body.getString("userName"))
                .setTitle(body.getString("title"))
                .setBookmarkCount(body.getInteger("bookmarkCount"))
                .setPageCount(body.getInteger("pageCount"))
                .setIllustType(body.getInteger("illustType"))
                .setBookmarkData(body.get("bookmarkData") != null ? 1 : 0)
                .setLastUpdate(System.currentTimeMillis())
                .setDownloaded(0)
        ;
        String description = body.getString("description");
        ill.setDescription(description != null ? description.substring(0, Math.min(4000, description.length())) : null);

        JSONObject urlsJson = body.getJSONObject("urls");
        if (urlsJson!=null) {
            String original = urlsJson.getString("original");

            if (ill.getIllustType()==ILLUST_TYPE_GIF) {
                original=original.replace("img-original", "img-zip-ugoira");
                original = original.substring(0,original.lastIndexOf("_"))+"_ugoira1920x1080.zip";
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
            ill.setTag(tagBuilder.toString()).setTagTranslated(translationBuilder.toString());
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


    public List<String> getUrlList(){
        List<String> list = new ArrayList<>();
        if (illustType==ILLUST_TYPE_GIF) {
            list.add(urlPrefix+fileName);
        }else{
            for (int i = 0; i < pageCount; i++) {
                String name = fileName.replace("_p0", "_p" + i);
                list.add(urlPrefix + name);
            }
        }
        return list;
    }
}
