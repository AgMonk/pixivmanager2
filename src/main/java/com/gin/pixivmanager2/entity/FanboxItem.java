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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;

/**
 * fanbox的作品
 *
 * @author bx002
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@TableName(value = "t_fanbox_items")
public class FanboxItem {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String TYPE_ARTICLE = "article";
    private static final String TYPE_IMAGE = "image";
    private static final String TYPE_FILE = "file";
    private static final String URL_PREFIX = "https://downloads.fanbox.cc/{type}/post/";

    String id;
    @TableField("updatedDatetime")
    Long updatedDatetime;
    String title;
    @TableField("creatorId")
    String creatorId;
    String type;
    String images = "";
    String files = "";

    public String getParentPath() {
        return "fanbox/" + creatorId + "/[" + id + "]" + "[title_" + title + "]";
    }

    public String getTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(updatedDatetime), ZoneId.of("Asia/Shanghai"));
        return formatter.format(zonedDateTime);
    }

    public TreeMap<String, String> getUrlMap() {
        TreeMap<String, String> map = new TreeMap<>();
        if (!StringUtils.isEmpty(images)) {
            String[] imagesArray = images.split(";");
            for (int i = 0; i < imagesArray.length; i++) {
                String fileName = imagesArray[i];
                String key = (i < 10 ? "0" : "") + i + fileName.substring(fileName.lastIndexOf("."));
                String value = URL_PREFIX.replace("{type}", "images") + id + "/" + fileName;
                map.put(key, value);
            }
        }
        if (!StringUtils.isEmpty(files)) {

            String[] filesArray = files.split(";");
            for (String fileName : filesArray) {
                String value = URL_PREFIX.replace("{type}", "files") + id + "/" + fileName;
                map.put(fileName, value);
            }
        }
        return map;
    }


    public FanboxItem(JSONObject json) {
        if (json == null) {
            return;
        }
        id = json.getString("id");
        title = json.getString("title");
        creatorId = json.getString("creatorId");
        type = json.getString("type");
        String dateTime = json.getString("updatedDatetime");
        updatedDatetime = ZonedDateTime.parse(dateTime).toInstant().toEpochMilli();

        JSONObject body = json.getJSONObject("body");
        if (body != null) {
            if (TYPE_IMAGE.equals(type) || TYPE_FILE.equals(type)) {
                JSONArray itemArray = body.getJSONArray(type + "s");
                for (int i = 0; i < itemArray.size(); i++) {
                    JSONObject item = itemArray.getJSONObject(i);
                    String originalUrl = item.getString("originalUrl");
                    originalUrl = originalUrl == null ? item.getString("url") : originalUrl;
                    String fileName = originalUrl.substring(originalUrl.lastIndexOf("/") + 1);
                    if (TYPE_IMAGE.equals(type)) {
                        images += fileName + ";";
                    } else {
                        files += fileName + ";";
                    }
                }
            }
            if (TYPE_ARTICLE.equals(type)) {
                JSONObject imageMap = body.getJSONObject("imageMap");
                imageMap.forEach((s, o) -> {
                    JSONObject item = (JSONObject) o;
                    String originalUrl = item.getString("originalUrl");
                    String fileName = originalUrl.substring(originalUrl.lastIndexOf("/") + 1);
                    images += fileName + ";";
                });

                JSONObject fileMap = body.getJSONObject("fileMap");
                fileMap.forEach((s, o) -> {
                    JSONObject item = (JSONObject) o;
                    String originalUrl = item.getString("url");
                    String fileName = originalUrl.substring(originalUrl.lastIndexOf("/") + 1);
                    files += fileName + ";";
                });

            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FanboxItem that = (FanboxItem) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
