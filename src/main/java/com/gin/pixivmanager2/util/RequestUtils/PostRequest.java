package com.gin.pixivmanager2.util.RequestUtils;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.File;
import java.util.HashMap;

/**
 * POST请求
 *
 * @author bx002
 * @date 2020/12/2 14:22
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@Slf4j
public class PostRequest implements RequestBase<PostRequest> {
    private HttpPost method;
    private HashMap<String, String> headers = new HashMap<>();
    private HashMap<String, String> params = new HashMap<>();
    private MultipartEntityBuilder entityBuilder = null;
    private HttpEntity httpEntity = null;
    private int maxTimes = 10;
    private int timeout = 10;

    public static PostRequest create() {
        return new PostRequest();
    }

    /**
     * 添加entityPart
     *
     * @param k key
     * @param v entityPart
     * @return this
     */
    private PostRequest addEntityPart(String k, ContentBody v) {
        entityBuilder = entityBuilder == null ? MultipartEntityBuilder.create() : entityBuilder;
        entityBuilder.addPart(k, v);
        return this;
    }

    /**
     * 上传文件
     *
     * @param name 文件名
     * @param file 文件
     * @return this
     */
    public PostRequest upload(String name, File file) {
        return addEntityPart(name, new FileBody(file));
    }

    /**
     * 添加字符串表单
     *
     * @param k key
     * @param v value
     * @return this
     */
    public PostRequest addEntityString(String k, String v) {
        return addEntityPart(k, new StringBody(v, ContentType.create(ContentType.TEXT_PLAIN.getMimeType(), getEncodeEnc())));
    }

    /**
     * 设置entity为StringEntity
     *
     * @param s string
     * @return this
     */
    public PostRequest setStringEntity(String s) {
        this.httpEntity = new StringEntity(s, getEncodeEnc());
        return this;
    }

    public Object post(String url) {
        try {
            method = createMethod(HttpPost.class, url);
            if (entityBuilder != null && httpEntity == null) {
                httpEntity = entityBuilder.build();
            }
            method.setEntity(httpEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return execute();
    }


}
