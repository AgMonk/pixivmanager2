package com.gin.pixivmanager2.util;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.gin.pixivmanager2.util.JsonUtil.printJson;

/**
 * Nga发帖工具类
 *
 * @author bx002
 */
@Slf4j
public class NgaPost {
    final static String NBSP = "\r\n";
    public final static String ACTION_NEW = "new";
    public final static String ACTION_REPLY = "reply";
    final static String NGA_ROOT_URL = "https://bbs.nga.cn";
    final static String NGA_POST_URL = NGA_ROOT_URL + "/post.php";
    final static String NGA_READ_URL = NGA_ROOT_URL + "/read.php";
    static String BBS_CODE_TAG_IMG = "[img]./{url}[/img]";
    static String BBS_CODE_TAG_FLASH = "[flash=video]./{url}[/flash]";
    static String BBS_CODE_TAG_COLLAPSE = "[collapse={title}]{content}[/collapse]";
    static String BBS_CODE_TAG_QUOTE = "[quote]" + NBSP + "{content}" + NBSP + "[/quote]";

    final static Pattern PATTERN_PID = Pattern.compile("pid\\d+");
    final static Pattern PATTERN_TID = Pattern.compile("tid=\\d+");
    /**
     * 发帖账号的cookie
     */
    final String cookie;
    /**
     * 发帖版面id
     */
    final String fid;
    /**
     * 回复主题id
     */
    final String tid;
    /**
     * 动作 new = 发帖  reply = 回复
     */
    final String action;
    /**
     * 帖子正文
     */
    StringBuilder contentBuilder = new StringBuilder();
    /**
     * 帖子标题
     */
    StringBuilder titleBuilder = new StringBuilder();

    /**
     * 上传附件验证码
     */
    String auth;
    /**
     * 上传附件的接口地址
     */
    String attachUrl;
    /**
     * 附件
     */
    StringBuffer attachmentsBuffer = new StringBuffer();
    /**
     * 附件验证码
     */
    StringBuffer attachmentsCheckBuffer = new StringBuffer();
    /**
     * 已上传附件的 name 对应的url
     */
    TreeMap<String, String> attachmentsMap = new TreeMap<>(Comparator.naturalOrder());

    public static NgaPost create(String cookie, String fid, String tid, String action) {
        return new NgaPost(cookie, fid, tid, action);
    }

    /**
     * 发帖
     *
     * @return 发帖/回复的地址
     */
    public String send() {
        String url = null;

        String result = Request.create(NGA_POST_URL)
                .setEncodeEnc("gbk")
                .setDecodeEnc("gbk")
                .setCookie(cookie)
                .addParam("action", action)
                .addParam("fid", fid)
                .addParam("tid", tid)
                .addParam("lite", "htmljs")
                .addParam("step", "2")
                .addParam("post_subject", titleBuilder.toString())
                .addParam("post_content", contentBuilder.toString())
                .addParam("tpic_misc_bit1", "40")
                .addParam("attachments", attachmentsBuffer.toString())
                .addParam("attachments_check", attachmentsCheckBuffer.toString())
                .post()
                .getResult();


        if (result != null && result.contains("发贴完毕")) {
            if (action.equals(ACTION_REPLY)) {
                Matcher matcher = PATTERN_PID.matcher(result);
                if (matcher.find()) {
                    String pid = matcher.group().substring(3);
                    url = NGA_READ_URL + "?pid=" + pid;
                }
            }
            if (action.equals(ACTION_NEW)) {
                Matcher matcher = PATTERN_TID.matcher(result);
                if (matcher.find()) {
                    String tid = matcher.group();
                    url = NGA_READ_URL + "?" + tid;
                }
            }
            log.info("发帖成功 :{}", url);
        } else {
            result = result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1);
            JSONObject json = JSONObject.parseObject(result);
            printJson(json);
        }

        return url;
    }

    /**
     * 多线程上传文件
     *
     * @param map 标识符 - 文件
     */
    public void uploadFiles(Map<String, File> map) {
        getAuthAndAttachUrl();

        CountDownLatch latch = new CountDownLatch(map.size());
        ThreadPoolTaskExecutor executor = getExecutor();
        //一次上传多个文件
        int i = 0;
        for (Map.Entry<String, File> entry : map.entrySet()) {
            int finalI = i;
            executor.execute(() -> {
                try {
                    uploadFile(finalI, entry.getKey(), entry.getValue());
                } catch (IOException e) {
                    log.info("文件压缩失败 放弃上传 {}", entry.getValue());
                }
                latch.countDown();
            });
            i++;
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
    }

    /**
     * 换行
     *
     * @return 换行符
     */
    public static String getWrap() {
        return NBSP;
    }

    /**
     * 添加正文内容
     *
     * @return this
     */
    public NgaPost addContent(String s) {
        contentBuilder.append(s);
        return this;
    }

    /**
     * 添加标题内容
     *
     * @return this
     */
    public NgaPost addTitle(String s) {
        titleBuilder.append(s);
        return this;
    }

    public static String getAttachmentCodeFromUrl(String url) {
        //动图关键字
        String mp4 = "mp4";
        if (url.contains(mp4)) {
            return BBS_CODE_TAG_FLASH.replace("{url}", url);
        } else {
            return BBS_CODE_TAG_IMG.replace("{url}", url);
        }
    }

    /**
     * 获得折叠的bbscode
     *
     * @param title   标题
     * @param content 内容
     * @return code
     */
    public static String getCollapse(String title, String content, String defaultTitle) {
        String code = BBS_CODE_TAG_COLLAPSE;
        if (title == null || "".equals(delKorea(title))) {
            code = code.replace("{title}", defaultTitle);
        } else {
            code = code.replace("{title}", delKorea(title));
        }
        code = code.replace("{content}", content);

        return code;
    }

    /**
     * 获得引用的bbscode
     *
     * @param content 内容
     * @return code
     */
    public static String getQuote(String content) {
        return BBS_CODE_TAG_QUOTE.replace("{content}", content);
    }


    /**
     * 获得链接的bbscode
     *
     * @param title 标题
     * @param url   链接
     * @return code
     */
    public static String getUrlCode(String title, String url) {
        return "[url=" + url + "]" + title + "[/url]";
    }


    /**
     * 请求attach_url和auth
     */
    private void getAuthAndAttachUrl() {
        Request request = Request.create(NGA_POST_URL)
                .addParam("__output", "14")
                .addParam("action", action)
                .addParam("fid", fid)
                .addParam("tid", tid)
                .setDecodeEnc("gbk")
                .setCookie(cookie);

        String result = request.post().getResult();

        JSONObject json = JSONObject.parseObject(result);
        Integer code = json.getInteger("code");

        while (code == 39) {
            log.debug("发言CD中");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            result = request.post().getResult();
            json = JSONObject.parseObject(result);
            code = json.getInteger("code");
        }
        JSONObject jsonObj = json.getJSONArray("result").getJSONObject(0);
        attachUrl = jsonObj.getString("attach_url");
        auth = jsonObj.getString("auth");
    }

    /**
     * 上传文件
     *
     * @param i    序号
     * @param file 文件
     * @param name 与上传后的url唯一对应的名称
     */
    private void uploadFile(int i, String name, File file) throws IOException {
        Request request = Request.create(attachUrl)
                .addFormData("attachment_file" + i + "_watermark", "")
                .addFormData("attachment_file" + i + "_dscp", "image" + i)
                .addFormData("attachment_file" + i + "_url_utf8_name", Request.encode(file.getName(), "utf-8"))
                .addFormData("attachment_file" + i + "_img", "1")
                .addFormData("func", "upload")
                .addFormData("v2", "1")
                .addFormData("auth", auth)
                .addFormData("origin_domain", "bbs.nga.cn")
                .setDecodeEnc("gbk")
                .addFormData("fid", fid)
                .addFormData("__output", "8");

        //大于4M选择自动压缩
        int maxLength = 4;
        //大于10M先行压缩
        int largeLength = 7;
        int k = 1024;
        HashMap<String, File> fileMap;
        //压缩
        file = zipImage(file, largeLength * k * k);

        if (file.length() >= maxLength * k * k) {
            request.addFormData("attachment_file" + i + "_auto_size", "1");
        } else {
            request.addFormData("attachment_file" + i + "_auto_size", "0");
        }
        //放入文件
        request.addUploadFile("attachment_file" + i, file);

        String result = request.post().getResult();
        log.info("开始上传 {}", file);
        JSONObject json = JSONObject.parseObject(result);
        JSONObject data = json.getJSONObject("data");
        if (data == null) {
            printJson(json);
        } else {
            String attachments = data.getString("attachments");
            String attachmentsCheck = data.getString("attachments_check");
            String url = data.getString("url");

            //保存 附件 验证码 url
            attachmentsBuffer.append(attachments).append("\t");
            attachmentsCheckBuffer.append(attachmentsCheck).append("\t");
            attachmentsMap.put(name, url);

//            if (file.delete()) {
//                log.debug("删除文件 {}", file);
//            } else {
//                log.warn("删除失败 {}", file);
//            }
        }
    }

    /**
     * 上传线程池
     *
     * @return 线程池
     */
    private ThreadPoolTaskExecutor getExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(5);
        //最大线程数
        executor.setMaxPoolSize(5);
        //队列容量
        executor.setQueueCapacity(10);
        //活跃时间
        executor.setKeepAliveSeconds(300);
        //线程名字前缀
        executor.setThreadNamePrefix("upload-");

        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();

        return executor;
    }

    private NgaPost(String cookie, String fid, String tid, String action) {
        this.cookie = cookie;
        this.fid = fid;
        this.tid = tid;
        this.action = action;
    }

    public TreeMap<String, String> getAttachmentsMap() {
        return attachmentsMap;
    }

    public String getAttachmentsCode(String name) {
        return getAttachmentCodeFromUrl(attachmentsMap.get(name));
    }

    /**
     * 删除韩文
     *
     * @param str 待检查字符串
     * @return 删除完毕字符串
     */
    private static String delKorea(String str) {
        StringBuilder unicode = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            // 取出每一个字符
            char c = str.charAt(i);
            // 韩文转换为unicode
            if (!(c >= 44032 && c <= 55215)) {
                unicode.append(c);
            }
        }
        return unicode.toString();
    }

    /**
     * 压缩图片到指定大小
     *
     * @param file     图片
     * @param toLength 指定大小 单位 B
     * @return 压缩好的图片
     * @throws IOException 异常
     */
    private static File zipImage(File file, long toLength) throws IOException {
        if (file.length() > toLength) {
            log.info("文件大于 {}K 进行压缩..", toLength / 1024);

            String path = file.getPath();
            String filePath = path.substring(0, path.lastIndexOf('.'));
            String newPath = filePath + "_z.jpg";

            Thumbnails.of(path)
                    .scale(0.9f)
                    .outputQuality(0.9f)
                    .outputFormat("jpg")
                    .toFile(newPath)
            ;

            File newFile = new File(newPath);
            log.info("压缩完毕 大小 {}K", file.length() / 1024);
            if (file.delete()) {
                log.debug("删除源文件 {}", file);
            } else {
                log.warn("删除源失败 {}", file);
            }
            if (newFile.length() > toLength) {
                return zipImage(newFile, toLength);
            } else {
                return newFile;
            }
        }

        return file;
    }


}
