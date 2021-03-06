package com.gin.pixivmanager2.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.util.RequestUtils.GetRequest;
import com.gin.pixivmanager2.util.RequestUtils.PostRequest;
import com.gin.pixivmanager2.util.RequestUtils.RequestBase;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;


/**
 * Pixiv请求工具类
 *
 * @author bx002
 */
public class PixivPost {
    public static final String URL_ARTWORK_PREFIX = "https://www.pixiv.net/artworks/";

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PixivPost.class);
    /**
     * 作品详情接口(cookie可选)
     */
    public final static String URL_ILLUST_DETAIL = "https://www.pixiv.net/ajax/illust/";
    /**
     * 获取收藏作品接口 需要cookie
     */
    final static String URL_BOOKMARKS_GET = "https://www.pixiv.net/ajax/user/{uid}/illusts/bookmarks";
    /**
     * 给单个作品添加tag接口 需要cookie
     */
    final static String URL_TAG_ADD = "https://www.pixiv.net/bookmark_add.php?id=";
    /**
     * 批量修改tag名称接口 需要cookie
     */
    final static String URL_TAG_SET = "https://www.pixiv.net/bookmark_tag_setting.php";
    /**
     * 搜索作品接口(cookie可选)
     */
    final static String URL_ILLUST_SEARCH = "https://www.pixiv.net/ajax/search/artworks/";
    /**
     * 添加收藏接口 需要cookie
     */
    final static String URL_BOOKMARKS_ADD = "https://www.pixiv.net/rpc/index.php";
    /**
     * 搜索用户作品接口
     */
    final static String URL_USER_ILLUST = "https://www.pixiv.net/ajax/user/{uid}/profile/all";

    /*—————————— 公开方法 ——————————*/

    /**
     * 查询作品详情
     *
     * @param pid    pid
     * @param cookie cookie
     * @return 如无错误 返回body对象 否则为null
     */
    public static JSONObject detail(String pid, String cookie) {
        pid = pid.contains("_") ? pid.substring(0, pid.indexOf("_")) : pid;
        long start = System.currentTimeMillis();
        log.debug("请求作品详情{} {}", cookie == null ? "" : "(cookie)", pid);

//        Request request = Request
//                .create(URL_ILLUST_DETAIL + pid)
//                .setTimeOutSecond(10)
//                .setCookie(cookie).get();
//        String result = request.getResult();

        String result = GetRequest.create().addCookie(cookie).get(URL_ILLUST_DETAIL + pid);

        JSONObject resultJson = result == null ? null : JSONObject.parseObject(result);
        JSONObject body = resultJson == null ? null : resultJson.getJSONObject("body");

        log.debug("获取作品详情{} {} 耗时 {}", body != null ? "成功" : "失败", pid, RequestBase.timeCost(start));
        return body;
    }

    /**
     * 为一个作品添加tag
     *
     * @param pid    pid
     * @param tags   tags
     * @param cookie pixiv cookie
     * @param tt     tt
     */
    public static void addTags(String pid, String cookie, String tt, String tags) {
        tags = tags.replace(",", " ");
        log.info("给作品添加tag {} -> {}", pid, tags);

//        Request.create(URL_TAG_ADD + pid)
//                .setMaxTimes(1)
//                .setTimeOutSecond(3)
//                .setCookie(cookie)
//                .addFormData("tt", tt)
//                .addFormData("id", pid)
//                .addFormData("tag", tags)
//                .addFormData("mode", "add")
//                .addFormData("type", "illust")
//                .addFormData("from_sid", "")
//                .addFormData("original_tag", "")
//                .addFormData("original_untagged", "0")
//                .addFormData("original_p", "1")
//                .addFormData("original_rest", "")
//                .addFormData("original_order", "")
//                .addFormData("comment", "")
//                .addFormData("restrict", "0")
//                .post();
        PostRequest.create()
                .setMaxTimes(1)
                .setTimeout(3)
                .addCookie(cookie)
                .addEntityString("tt", tt)
                .addEntityString("id", pid)
                .addEntityString("tag", tags)
                .addEntityString("mode", "add")
                .addEntityString("type", "illust")
                .addEntityString("from_sid", "")
                .addEntityString("original_tag", "")
                .addEntityString("original_untagged", "0")
                .addEntityString("original_p", "1")
                .addEntityString("original_rest", "")
                .addEntityString("original_order", "")
                .addEntityString("comment", "")
                .addEntityString("restrict", "0")
                .post(URL_TAG_ADD + pid);
    }

    /**
     * 批量添加tag
     *
     * @param pidAndTags  pid  和tags
     * @param cookie      cookie
     * @param executor    线程池
     * @param progressMap 进度
     */
    public static void addTags(Map<String, String> pidAndTags, String cookie, String tt, ThreadPoolTaskExecutor executor, Map<String, Integer> progressMap) {
        List<Callable<Void>> tasks = new ArrayList<>();
        long start = System.currentTimeMillis();
        log.debug("添加Tag {} 个", pidAndTags.size());
        progressMap.put("size", pidAndTags.size());
        progressMap.put("count", 0);
        for (Map.Entry<String, String> entry : pidAndTags.entrySet()) {
            String pid = entry.getKey();
            String tags = entry.getValue();
            tasks.add(() -> {
                addTags(pid, cookie, tt, tags);
                addProgress(progressMap);
                return null;
            });
        }
        TasksUtil.executeTasks(tasks, 60, executor, "addTags", 2);
        completeProgress(progressMap);
        log.debug("添加Tag {} 个 耗时 {}", pidAndTags.size(), RequestBase.timeCost(start));
    }

    /**
     * 请求收藏中作品
     *
     * @param cookie cookie
     * @param uid    uid
     * @param limit  limit
     * @param offset offset
     * @param tag    tag
     * @return body
     */
    public static JSONObject getBookmarks(String uid, String cookie, String tag, int limit, int offset) {
        long start = System.currentTimeMillis();
        log.debug("请求收藏 UID={} 标签={} 第 {} 页", uid, tag, offset / limit + 1);

//        String result = Request.create(URL_BOOKMARKS_GET.replace("{uid}", uid))
//                .setCookie(cookie)
//                .addParam("limit", limit)
//                .addParam("offset", offset)
//                .addParam("tag", tag)
//                .addParam("lang", "zh")
//                .addParam("rest", "show")
//                .get().getResult();
        String result = GetRequest.create()
                .addCookie(cookie)
                .addParam("limit", limit)
                .addParam("offset", offset)
                .addParam("tag", tag)
                .addParam("lang", "zh")
                .addParam("rest", "show")
                .get(URL_BOOKMARKS_GET.replace("{uid}", uid));

        JSONObject body = getBody(result);

        if (body != null) {
            log.debug("获得收藏 UID={} 标签={} 第 {} 页 耗时 {} 毫秒", uid, tag, offset / limit + 1, RequestBase.timeCost(start));
        } else {
            log.warn("请求错误 UID={} 标签={} 第 {} 页", uid, tag, offset / limit + 1);
        }
        return body;
    }

    /**
     * 请求收藏中的作品
     *
     * @param cookie cookie
     * @param uid    uid
     * @param tag    tag
     * @param page   页数
     * @return 作品列表
     */
    public static List<JSONObject> getBookmarks(String uid, String cookie, String tag, Integer page, ThreadPoolTaskExecutor executor, Map<String, Integer> progressMap) {
        long start = System.currentTimeMillis();
        progressMap.put("size", page);
        progressMap.put("count", 0);
        page = (page == null || page < 1) ? 1 : page;
        int offset = 0;
        int limit = 10;
        //请求到的数量
        int total = 0;
        List<JSONObject> worksList = null;
        JSONObject body = getBookmarks(uid, cookie, tag, limit, offset);

        addProgress(progressMap);

        if (body != null) {
            total = body.getInteger("total");
            log.info("标签 [{}] 下有总计 {} 个作品", tag, total);
            total = Math.min(total, page * limit);
            log.debug("请求 {} 个作品", total);
            JSONArray works = body.getJSONArray("works");
            for (int i = 0; i < works.size(); i++) {
                worksList = worksList != null ? worksList : new ArrayList<>();
                worksList.add(works.getJSONObject(i));
            }
        }
        offset += limit;

        if (offset < total) {
            List<Callable<JSONObject>> tasks = new ArrayList<>();
            while (offset < total) {
                int finalOffset = offset;
                tasks.add(() -> {
                    JSONObject bookmarks = PixivPost.getBookmarks(uid, cookie, tag, limit, finalOffset);

                    addProgress(progressMap);

                    return bookmarks;
                });
                offset += limit;
            }
            List<JSONObject> otherBodies = TasksUtil.executeTasks(tasks, 60, executor, "bookmark", 2);
            for (JSONObject otherBody : otherBodies) {
                JSONArray works = otherBody.getJSONArray("works");
                for (int i = 0; i < works.size(); i++) {
                    worksList = worksList != null ? worksList : new ArrayList<>();
                    worksList.add(works.getJSONObject(i));
                }
            }
        }
        completeProgress(progressMap);

        log.debug("获取 {} 个作品 耗时 {} 毫秒", total, System.currentTimeMillis() - start);
        return worksList;
    }

    /**
     * 批量修改tag名称
     *
     * @param oldName 原tag名
     * @param newName 新tag名
     * @param tt      tt
     */
    public static void setTag(String cookie, String tt, String oldName, String newName) {
        log.info("修改Tag {} -> {}", oldName, newName);

//        Request.create(URL_TAG_SET)
//                .addFormData("mode", "mod")
//                .addFormData("tag", oldName)
//                .addFormData("new_tag", newName)
//                .addFormData("tt", tt)
//                .setMaxTimes(1)
//                .setTimeOutSecond(3)
//                .setCookie(cookie).post();
        PostRequest.create()
                .setMaxTimes(1)
                .setTimeout(3)
                .addCookie(cookie)
                .addEntityString("mode", "mod")
                .addEntityString("tag", oldName)
                .addEntityString("new_tag", newName)
                .addEntityString("tt", tt)
                .post(URL_TAG_SET);
    }

    /**
     * 搜索作品
     *
     * @param keyword     关键字
     * @param p           页数(每页固定上限60个)
     * @param cookie      cookie(可选 不提供时不能搜索R-18作品)
     * @param searchTitle true = 搜索标题 false =搜 索tag
     * @param mode        模式 可取值： all safe r18
     * @return 搜索结果
     */
    public static JSONObject search(String keyword, Integer p, String cookie, boolean searchTitle, String mode) {
        if (keyword == null) {
            return null;
        }
        List<String> availableMode = new ArrayList<>(Arrays.asList("all", "safe", "r18"));
        if (mode == null || !availableMode.contains(mode)) {
            mode = "all";
        }

        p = p == null || p < 0 ? 1 : p;
        log.info("搜索{} 关键字: {} 第 {} 页", searchTitle ? "标题" : "标签", keyword, p);

        String result =
//                Request.create(URL_ILLUST_SEARCH + CodeUtils.encode(keyword, null))
//                .setCookie(cookie)
//                .addParam("s_mode", searchTitle ? "s_tc" : "s_tag")
//                .addParam("mode", mode)
//                .addParam("p", p)
//                .get()
//                .getResult();

                GetRequest.create()
                        .addCookie(cookie)
                        .addParam("s_mode", searchTitle ? "s_tc" : "s_tag")
                        .addParam("mode", mode)
                        .addParam("p", p)
                        .get(URL_ILLUST_SEARCH + CodeUtils.encode(keyword, null));

        JSONObject body = getBody(result);

        if (body == null) {
            log.info("搜索失败 {}", result);
            return null;
        }

        JSONObject illustManga = body.getJSONObject("illustManga");
        Integer total = illustManga.getInteger("total");
        JSONArray data = illustManga.getJSONArray("data");
        JSONObject json = new JSONObject();
        json.put("total", total);
        json.put("data", data);
        log.info("搜索{} 关键字: {}  第 {} 页 获得结果 {} 个 总计结果 {} 个 共计 {} 页", searchTitle ? "标题" : "标签", keyword, p, data.size(), total, total / 60 + 1);
        return json;
    }

    /**
     * 批量搜索
     *
     * @param keywords    关键字
     * @param start       页数
     * @param cookie      cookie(可选 不提供时不能搜索R-18作品)
     * @param searchTitle true = 搜索标题 false =搜 索tag
     * @param mode        模式 可取值： all safe r18
     * @param executor    线程池
     * @param progressMap 进度
     * @return 搜索结果
     */
    public static List<JSONObject> search(Collection<String> keywords, Integer start, Integer end, String cookie, boolean searchTitle
            , String mode, ThreadPoolTaskExecutor executor, Map<String, Integer> progressMap) {
        List<Callable<List<JSONObject>>> tasks = new ArrayList<>();
        for (String keyword : keywords) {
            for (int i = start; i <= end; i++) {
                Integer finalI = i;
                tasks.add(() -> {
                    JSONObject json = search(keyword, finalI, cookie, searchTitle, mode);
                    List<JSONObject> list = new ArrayList<>();
                    if (json != null) {
                        JSONArray data = json.getJSONArray("data");
                        data.forEach(o -> list.add((JSONObject) o));
                    }
                    addProgress(progressMap);
                    return list;
                });
            }
        }
        List<List<JSONObject>> search = TasksUtil.executeTasks(tasks, 60, executor, "search", 3);

        return search.stream().flatMap(Collection::stream).collect(Collectors.toList());
    }

    /**
     * 添加收藏 添加tag(可选)
     *
     * @param pid    pid
     * @param cookie cookie
     * @param tt     tt
     * @param tags   tags
     * @return 如果成功返回true 失败返回其pid
     */
    public static Object bmk(String pid, String cookie, String tt, String tags) {
        tags = tags == null ? "" : tags.replace(",", " ");
        log.info("添加收藏 {} tags: {}", pid, tags);

        String result =
//                Request.create(URL_BOOKMARKS_ADD)
//                .setCookie(cookie)
//                .addFormData("mode", "save_illust_bookmark")
//                .addFormData("illust_id", pid)
//                .addFormData("restrict", "0")
//                .addFormData("comment", "")
//                .addFormData("tags", tags)
//                .addFormData("tt", tt)
//                .post()
//                .getResult();
                (String) PostRequest.create()
                        .addCookie(cookie)
                        .addEntityString("mode", "save_illust_bookmark")
                        .addEntityString("illust_id", pid)
                        .addEntityString("restrict", "0")
                        .addEntityString("comment", "")
                        .addEntityString("tags", tags)
                        .addEntityString("tt", tt)
                        .post(URL_BOOKMARKS_ADD);
        JSONObject body = getBody(result);


        return body != null ? true : pid;

    }

    /**
     * 批量添加收藏
     *
     * @param pidAndTags  pid及其对应的tag
     * @param cookie      cookie
     * @param tt          tt
     * @param executor    线程池
     * @param progressMap 进度对象
     * @return 如果有失败任务 返回其pid
     */
    public static List<Object> bmk(Map<String, String> pidAndTags, String cookie, String tt, ThreadPoolTaskExecutor executor, Map<String, Integer> progressMap) {
        long start = System.currentTimeMillis();
        Set<String> pidSet = pidAndTags.keySet();
        log.info("添加收藏任务 {}个", pidAndTags.size());

        List<Callable<Object>> tasks = new ArrayList<>();
        for (String pid : pidSet) {
            tasks.add(() -> {
                Object bmk = bmk(pid, cookie, tt, pidAndTags.get(pid));
                addProgress(progressMap);
                return bmk;
            });
        }
        //执行结果
        List<Object> bmk = TasksUtil.executeTasks(tasks, 5, executor, "bmk", 2);

        bmk.removeIf(o -> o instanceof Boolean);

        log.info("批量收藏 {} 个作品 失败 {} 个 耗时 {} 毫秒", pidAndTags.size(), bmk.size(), System.currentTimeMillis() - start);

        return bmk;
    }



    /*—————————— 基础方法 ————————————*/


    /**
     * 判断请求是否成功 如果成功返回body json对象
     *
     * @param result 自定义错误消息内容
     */
    private static JSONObject getBody(String result) {
        if (result != null) {
            JSONObject json = JSONObject.parseObject(result);
            String error = "error";
            if (json.getBoolean(error)) {
                //出错
                throw new RuntimeException(json.getString("message"));
            } else {
                return json.getJSONObject("body");
            }
        }
        return null;
    }

    /**
     * 进度+1
     *
     * @param progressMap 进度
     */
    private static void addProgress(Map<String, Integer> progressMap) {
        if (progressMap == null) {
            return;
        }
        Integer count = progressMap.get("count");
        count = count == null ? 0 : count;
        progressMap.put("count", count + 1);
    }

    /**
     * 完成进度
     *
     * @param progressMap 进度
     */
    private static void completeProgress(Map<String, Integer> progressMap) {
        if (progressMap == null) {
            return;
        }
        progressMap.put("count", progressMap.get("size"));
    }

}

