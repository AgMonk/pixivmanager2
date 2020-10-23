package com.gin.pixivmanager2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

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
public class TagFromIllust {
    String id;
    String tag;
    @TableField("tagTranslated")
    String tagTranslated;

    public List<Tag> getTagList() {
        String[] a1 = tag.split(",");
        String[] a2;
        if (tagTranslated != null) {

            a2 = tagTranslated.split(",");
        } else {
            a2 = a1;
        }
        ArrayList<Tag> tags = new ArrayList<>();

        for (int i = 0; i < a1.length; i++) {
            tags.add(new Tag(a1[i], a2[i]));
        }

        return tags;
    }
}
