package com.gin.pixivmanager2.Aria2;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

/**
 * Aria2返回的文件状态
 *
 * @author bx002
 * @date 2020/11/30 10:05
 */
@Data
@NoArgsConstructor
public class Aria2File {
    Integer completedLength;
    Integer totalLength;
    String status;
    String path;
    String url;
    String gid;

    public String getFileName() {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public Aria2File(JSONObject json) {
        this.completedLength = json.getInteger("completedLength");
        this.totalLength = json.getInteger("totalLength");
        this.status = json.getString("status");
        this.gid = json.getString("gid");
        JSONObject file = json.getJSONArray("files").getJSONObject(0);
        this.path = file.getString("path");
        this.url = file.getJSONArray("uris").getJSONObject(0).getString("uri");
    }

    public Aria2File(String jsonString) {
        this(JSONObject.parseObject(jsonString));
    }

    public static ArrayList<Aria2File> parseArray(JSONArray jsonArray) {
        ArrayList<Aria2File> list = new ArrayList<>();
        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(new Aria2File(jsonArray.getJSONObject(i)));
        }
        return list;
    }
}
