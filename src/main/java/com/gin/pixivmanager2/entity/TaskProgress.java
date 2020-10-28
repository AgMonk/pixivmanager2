package com.gin.pixivmanager2.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author bx002
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Accessors(chain = true)
public class TaskProgress {
    long createdTime = System.currentTimeMillis();
    Map<String, Integer> progress = new HashMap<>();
    String type;

    public String getPercent() {
        Integer count = progress.get("count");
        Integer size = progress.get("size");
        if (count != null && size != null) {
            return String.format("%.2f", 100.0 * count / size);
        }
        return null;
    }

    public String getPercentSize() {
        return progress.get("count") + "/" + progress.get("size");
    }

    public String getTime() {
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(createdTime), ZoneId.of("Asia/Shanghai"));
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(zonedDateTime);
    }

    public TaskProgress(String type) {
        this.type = type;
    }
}
