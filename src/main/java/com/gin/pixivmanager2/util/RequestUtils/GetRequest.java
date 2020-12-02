package com.gin.pixivmanager2.util.RequestUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;

import java.util.HashMap;

/**
 * get方法
 *
 * @author bx002
 * @date 2020/12/2 10:37
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class GetRequest implements RequestBase<GetRequest> {
    private HttpGet method;
    private HashMap<String, String> headers = new HashMap<>();
    private HashMap<String, String> params = new HashMap<>();
    private MultipartEntityBuilder entityBuilder = null;

    public static GetRequest create() {
        return new GetRequest();
    }

    public String get(String url) {
        try {
            method = createMethod(HttpGet.class, url);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (String) execute();
    }
}
