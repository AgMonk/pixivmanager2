package com.gin.pixivmanager2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gin.pixivmanager2.util.PatternUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 推特图片
 *
 * @author bx002
 * @date 2020/12/3 11:17
 */
@Data
@TableName("t_twitter_image")
@NoArgsConstructor
public class TwitterImage {
    public final static Pattern PATTERN_STATUS_ID = Pattern.compile("\\d+_\\d+");
    public final static Pattern PATTERN_TAGS = Pattern.compile("\\[tags_.+?\\]");
    public final static Pattern PATTERN_AUTHOR = Pattern.compile("\\[author_.+?\\]");
    public final static Pattern PATTERN_TITLE = Pattern.compile("\\[title.+?\\]");

    @TableId(value = "statusId")
    String statusId;
    String tags;
    String suffix;
    String author;
    @TableField(exist = false)
    String path;

    public static TwitterImage parse(String filePath) {
        TwitterImage image = new TwitterImage();
        String path = filePath.replace("\\", "/");
        String fileName = path.substring(path.lastIndexOf("/"));

        image.setPath(path);
        image.setStatusId(PatternUtils.get(PATTERN_STATUS_ID, fileName));

        String tags = PatternUtils.get(PATTERN_TAGS, fileName);
        tags = tags == null ? null : tags.replace("[tags_", "").replace("]", "");
        image.setTags(tags);

        String author = PatternUtils.get(PATTERN_AUTHOR, fileName);
        author = author == null ? null : author.replace("[author_", "").replace("]", "");
        image.setAuthor(author);


        image.setSuffix(fileName.substring(fileName.lastIndexOf(".")));

        return image;
    }

    public String getFileName() {
        String id = "[" + statusId + "]";
        String tag = this.tags != null ? "[tags_" + tags + "]" : "";
        String author = this.author != null ? "[author_" + this.author + "]" : "";
        return id + author + tag + suffix;
    }

    public String getUrl() {
        String[] s = statusId.split("_");
        if (StringUtils.isEmpty(author)) {
            author = "i";
        }
        return "https://twitter.com/" + author + "/status/" + s[0] + "/photo/" + s[1];
    }

}
