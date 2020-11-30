package com.gin.pixivmanager2.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import static com.gin.pixivmanager2.service.FileServiceImpl.ILLUSTRATED_PATTERN;
import static com.gin.pixivmanager2.util.PixivPost.URL_ARTWORK_PREFIX;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@TableName(value = "t_downloadlist")
public class DownloadingFile implements Comparable<DownloadingFile> {
    final public static String FILE_TYPE_UNTAGGED = "未分类";
    final public static String FILE_TYPE_FANBOX = "fanbox";
    final public static String FILE_TYPE_SEARCH_RESULTS = "搜索";
    final public static String[] TYPE_LIST = {
            FILE_TYPE_FANBOX, FILE_TYPE_UNTAGGED, FILE_TYPE_SEARCH_RESULTS
    };


    @TableId(type = IdType.AUTO)
    String id;
    String url;
    String path;
    String type;
    @TableField(exist = false)
    Map<String, Integer> progress = new HashMap<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DownloadingFile that = (DownloadingFile) o;

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    public DownloadingFile(String url, String path, String type) {
        this.url = url;
        this.path = path;
        this.type = type;
    }

    public String getPidCount() {
        Matcher matcher = ILLUSTRATED_PATTERN.matcher(path);
        if (matcher.find()) {
            return matcher.group();
        }
        return path.substring(0, path.lastIndexOf("."));
    }

    public String getPercent() {
        Integer count = progress.get("count");
        Integer size = progress.get("size");
        if (count != null && size != null) {
            return String.format("%.2f", 100.0 * count / size);
        }
        return null;
    }

    public String getPercentSize() {
        Integer count = progress.get("count");
        Integer size = progress.get("size");
        return sizeFormat(count) + "/" + sizeFormat(size);
    }

    public String getSourceUrl() {
        Matcher matcher = ILLUSTRATED_PATTERN.matcher(path);
        if (matcher.find()) {
            String pidCount = getPidCount();
            pidCount = pidCount.substring(0, pidCount.indexOf("_"));
            return URL_ARTWORK_PREFIX + pidCount;
        }
        return null;
    }

    public String getFileName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private static String sizeFormat(Integer size) {
        size = size == null ? 100 : size;
        int k = 1024;
        double d;
        if (size > k * k) {
            d = 1.0 * size / k / k;
            return String.format("%.2f", d) + "M";
        } else if (size > 100 * k) {
            d = 1.0 * size / k;
            return String.format("%.1f", d) + "K";
        } else {
            return size + "B";
        }
    }


    @Override
    public int compareTo(DownloadingFile o) {
        if (this.equals(o)) {
            return 0;
        }
        Integer numThis = null;
        Integer numThat = null;
        for (int i = 0; i < TYPE_LIST.length; i++) {
            if (this.type.startsWith(TYPE_LIST[i])) {
                numThis = i;
            }
            if (o.type.startsWith(TYPE_LIST[i])) {
                numThat = i;
            }
        }
        if (numThis == null && numThat == null) {
            return -1 * this.path.compareTo(o.path);
        }
        if (numThis != null && numThat == null) {
            return -1;
        }
        if (numThis == null) {
            return 1;
        }
        if (numThis.equals(numThat)) {
            return -1 * this.path.compareTo(o.path);
        }

        return numThis - numThat;
    }
}
