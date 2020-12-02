package com.gin.pixivmanager2.util.RequestUtils;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 请求基类
 *
 * @param <T> 实现类名
 */
public interface RequestBase<T> {
    org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(RequestBase.class);

    /**
     * 请求方法
     *
     * @return 请求方法
     */
    HttpRequestBase getMethod();

    /**
     * 请求头
     *
     * @return 请求头
     */
    Map<String, String> getHeaders();

    /**
     * 请求地址栏参数
     *
     * @return 请求地址栏参数
     */
    Map<String, String> getParams();

    /**
     * 最大尝试次数
     *
     * @return 最大尝试次数
     */
    default int getMaxTimes() {
        return 10;
    }

    /**
     * 超时秒数
     *
     * @return 秒
     */
    default int getTimeout() {
        return 10;
    }

    /**
     * 创建请求方法
     *
     * @param clazz 请求方法类对象 HttpPost 或 HttpGet
     * @param url   请求url
     * @return 方法类
     * @throws NoSuchMethodException     异常
     * @throws IllegalAccessException    异常
     * @throws InvocationTargetException 异常
     * @throws InstantiationException    异常
     */
    default <T> T createMethod(Class<T> clazz,
                               String url
    )
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<T> constructor = clazz.getConstructor(String.class);
        if (getParams() != null && getParams().size() > 0) {
            StringBuilder sb = new StringBuilder();
            url += url.endsWith("?") ? "" : "?";

            getParams().forEach((k, v) -> sb.append("&").append(k).append("=").append(encode(v)));
            url += sb.toString();
        }
        return constructor.newInstance(url);
    }

    /**
     * 响应entity 编码
     *
     * @return 编码
     */
    default String getDecodeEnc() {
        return StandardCharsets.UTF_8.toString();
    }

    /**
     * 响应entity 编码
     *
     * @return 编码
     */
    default String getEncodeEnc() {
        return StandardCharsets.UTF_8.toString();
    }

    /**
     * 获得客户端
     *
     * @return 客户端
     */
    default CloseableHttpClient getClient() {
        int k = 1000;
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(getTimeout() * k * 10)
                .setConnectTimeout(getTimeout() * k)
                .setSocketTimeout(getTimeout() * k).build();

        return HttpClients.custom()
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(100)
                .setDefaultRequestConfig(config)
                .build();
    }

    /**
     * 多次尝试执行请求
     *
     * @return 请求结果
     */
    default Object execute() {
        //添加请求头
        if (getHeaders().size() > 0) {
            getHeaders().forEach((k, v) -> {
                getMethod().addHeader(k, v);
                LOG.debug("Header {} -> {}", k, v);
            });
        }
        //尝试请求
        long start = System.currentTimeMillis();
        for (int i = 1; i <= getMaxTimes(); i++) {
            Object result = execute(i);
            if (result != null) {
                if (!(result instanceof Integer)) {
                    LOG.info("请求成功 总尝试次数 {} 地址：{} 总耗时: {}", i, getMethod().getURI(), timeCost(start));
                    return result;
                } else {
                    int statusCode = (int) result;
                    //500开头的错误则等待5秒继续请求
                    if (statusCode / 100 == 5) {
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ignored) {
                        }
                    } else {
                        break;
                    }
                }
            }
        }
//        LOG.error("请求行 {}", getMethod().getRequestLine());
        for (Header header : getMethod().getAllHeaders()) {
            LOG.warn("Header {}", header);
        }
        return null;
    }

    /**
     * 执行一次请求
     *
     * @param i 执行次数
     * @return 请求结果
     */
    default Object execute(int i) {
        long start = System.currentTimeMillis();
        LOG.debug("第 {} 次请求 地址: {}", i, getMethod().getURI());

        CloseableHttpResponse response = null;
        try {
            response = getClient().execute(getMethod());
        } catch (SocketTimeoutException ignored) {
            LOG.debug("第 {} 次请求超时 地址: {}", i, getMethod().getURI());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (response == null) {
            return null;
        }
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == HttpStatus.SC_OK) {
            LOG.debug("第 {} 次请求 成功 地址: {} 耗时：{}", i, getMethod().getURI(), timeCost(start));
            return handleEntity(response.getEntity());
        } else if (statusCode == HttpStatus.SC_MOVED_TEMPORARILY) {
            Header location = response.getFirstHeader("Location");
            LOG.warn("第 {} 次请求 被重定向到 {}", i, location);
        } else {
            LOG.warn("第 {} 次请求失败 地址:{} code: {}", i, getMethod().getURI(), statusCode);
        }
        return statusCode;
    }

    /**
     * 处理响应的entity
     *
     * @param entity 处理结果
     */
    default Object handleEntity(HttpEntity entity) {
        Object result = null;
        try {
            result = EntityUtils.toString(entity, getDecodeEnc());
        } catch (SocketTimeoutException ignored) {
            LOG.debug("请求超时 地址: {}", getMethod().getURI());

        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 添加header
     *
     * @param k key
     * @param v value
     * @return this
     */
    default T addHeader(String k, String v) {
        if (!StringUtils.isEmpty(v)) {
            getHeaders().put(k, v);
        }
        return (T) this;

    }

    /**
     * 添加 Referer
     *
     * @param referrer Referer
     * @return this
     */
    default T addReferer(String referrer) {
        return addHeader("Referer", referrer);
    }

    /**
     * 添加Origin
     *
     * @param origin Origin
     * @return this
     */
    default T addOrigin(String origin) {
        return addHeader("Origin", origin);
    }

    /**
     * 添加Cookie
     *
     * @param cookie Cookie
     * @return this
     */
    default T addCookie(String cookie) {
        return addHeader("Cookie", cookie);
    }

    default T addContentType(ContentType contentType) {
        return addHeader(HttpHeaders.CONTENT_TYPE, contentType.toString());
    }

    /**
     * 添加地址栏参数
     *
     * @param k key
     * @param v valuse
     * @return this
     */
    default T addParam(String k, Object v) {
        if (!StringUtils.isEmpty(v)) {
            getParams().put(k, String.valueOf(v));
        }
        return (T) this;
    }

    /**
     * 生成耗时字符串
     *
     * @param start 开始毫秒
     * @return 耗时字符串
     */
    static String timeCost(long start) {
        return timeCost(start, System.currentTimeMillis());
    }

    /**
     * 生成耗时字符串
     *
     * @param start 开始毫秒
     * @param end   结束毫秒
     * @return 耗时字符串
     */
    static String timeCost(long start, long end) {
        long k = 1000L;
        int ten = 10;
        int minute = 60;
        long dur = end - start;

        if (dur < ten * k) {
            return dur + " 毫秒";
        }
        if (dur < minute * k) {
            return String.format("%.1f 秒", 1.0 * dur / k);
        }
        return String.format("%.1f 分", 1.0 * dur / k / minute);
    }

    /**
     * 编码
     *
     * @param s 待编码字符串
     * @return 编码完成字符串
     */
    private String encode(String s) {
        String encode = null;
        try {
            encode = URLEncoder
                    .encode(s, getEncodeEnc())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encode;
    }
}
