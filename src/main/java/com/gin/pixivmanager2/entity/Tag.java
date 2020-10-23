package com.gin.pixivmanager2.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
@TableName(value = "t_tags_translation")
public class Tag {
    public static Map<String, String> dic;

    String id;
    String name;
    String translation;
    @TableField(exist = false)
    Integer count = 0;

    public Tag(String name, String translation) {
        this.name = name;
        this.translation = translation;
    }

    public void add() {
        count++;
    }

    public String getCustomTranslation() {
        if (dic.containsKey(name)) {
            return dic.get(name);
        } else {
            final String[] n = {name};
            dic.forEach((k, v) -> {
                if (n[0].contains(k)) {
                    n[0] = n[0].replace(k, v);
                }
            });
            return n[0];
        }
    }
}
