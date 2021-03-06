package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.Aria2.Aria2File;
import com.gin.pixivmanager2.Aria2.Aria2Json;
import com.gin.pixivmanager2.Aria2.Aria2Option;
import com.gin.pixivmanager2.dao.DownloadingFileDAO;
import com.gin.pixivmanager2.dao.TwitterImageDAO;
import com.gin.pixivmanager2.entity.*;
import com.gin.pixivmanager2.util.FilesUtils;
import com.gin.pixivmanager2.util.GifUtil;
import com.gin.pixivmanager2.util.TasksUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<DownloadingFileDAO, DownloadingFile> implements FileService {
    public final static Pattern ILLUSTRATED_PATTERN = Pattern.compile("\\d+_p\\d+");
    private final static Pattern TWITTER_PATTERN = Pattern.compile("\\d+_\\d+");
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final String rootPath;
    private final String archivePath;
    private final String fanboxCookie;
    private final IllustrationService illustrationService;
    private final ThreadPoolTaskExecutor fileExecutor = TasksUtil.getExecutor("file", 2);
    private final ThreadPoolTaskExecutor gifExecutor = TasksUtil.getExecutor("gif", 1);
    private final ThreadPoolTaskExecutor requestExecutor;
    private final ThreadPoolTaskExecutor queueExecutor;
    private final ProgressService progressService;
    private final TwitterImageDAO twitterImageDAO;


    private final static int MAX_CONCURRENT_DOWNLOADS = 30;

    private final List<DownloadingFile> downloadingFileList = new ArrayList<>();

    public FileServiceImpl(ThreadPoolTaskExecutor downloadExecutor, ConfigService configService, IllustrationService illustrationService, ThreadPoolTaskExecutor requestExecutor, ThreadPoolTaskExecutor queueExecutor, ProgressService progressService, TwitterImageDAO twitterImageDAO) {
        this.downloadExecutor = downloadExecutor;

        this.rootPath = configService.getPath("rootPath").getValue();
        this.illustrationService = illustrationService;
        this.requestExecutor = requestExecutor;
        this.queueExecutor = queueExecutor;
        this.progressService = progressService;
        this.twitterImageDAO = twitterImageDAO;
        this.archivePath = rootPath + "/archive";
        this.fanboxCookie = configService.getCookie("fanbox").getValue();
    }

    @Override
    public void download(Illustration illustration, String type) {
        log.info("添加下载队列 {}个", illustration.getPageCount());

        saveBatch(getDownloadingList(illustration, type).collect(Collectors.toList()));
    }

    @Override
    public void download(Collection<Illustration> illustrations, String type) {
        int sum = illustrations.stream().mapToInt(Illustration::getPageCount).sum();
        if (sum == 0) {
            return;
        }
        log.info("添加下载队列 {}个", sum);
        List<String> downloadingPath = list().stream().map(DownloadingFile::getPath).collect(Collectors.toList());
        saveBatch(illustrations.stream()
                .flatMap(i -> getDownloadingList(i, type))
                .filter(d -> !downloadingPath.contains(d.getPath()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public void download(FanboxItem fanboxItem) {
        TreeMap<String, String> urlMap = fanboxItem.getUrlMap();
        String type = fanboxItem.getParentPath();
        List<DownloadingFile> list = new ArrayList<>();
        urlMap.forEach((path, url) -> {
            list.add(new DownloadingFile(url, path, type));
        });
        saveBatch(list);
    }

    @Override
    public void download(List<FanboxItem> fanboxItemList) {
        List<DownloadingFile> list = new ArrayList<>();
        fanboxItemList.forEach(fanboxItem -> {
            TreeMap<String, String> urlMap = fanboxItem.getUrlMap();
            String type = fanboxItem.getParentPath();
            urlMap.forEach((path, url) -> {
                list.add(new DownloadingFile(url, path, type));
            });
        });
        saveBatch(list);
    }

    @Override
    public Map<String, File> getFileMap(String type) {
        HashMap<String, File> map = new HashMap<>();
        map.size();
        log.info("获取文件列表 {}", type);
        listFiles(new File(rootPath + "/" + type), map);
        return map;
    }


    /**
     * 删除文件
     *
     * @param pidCollection
     * @param type
     */
    @Override
    public void del(Collection<String> pidCollection, String type) {
        Map<String, File> fileMap = getFileMap(type);
        pidCollection.forEach(pid -> log.info("删除 {} {}", pid, fileMap.get(pid).delete() ? "成功" : "失败"));
    }

    /**
     * 归档文件
     *
     * @param pidCollection
     * @param type
     */
    @Override
    public void archivePixiv(Collection<String> pidCollection, String type) {
        Map<String, File> fileMap = getFileMap(type);
        if (fileMap.size() == 0) {
            return;
        }
        //如果为null则归档全部
        pidCollection = pidCollection == null ? fileMap.keySet() : pidCollection;
        //带_的pid转为不带_的pid并去重
        List<String> pidList = pidCollection.stream()
                .filter(s -> s.contains("_p"))
                .map(s -> s.substring(0, s.indexOf("_p")))
                .distinct().collect(Collectors.toList());

        TaskProgress taskProgress = progressService.add("归档", pidCollection.size());
        Collection<String> finalPidCollection = pidCollection;
        pidList.forEach(pid -> {
            queueExecutor.execute(() -> {
                List<Illustration> details = illustrationService.findList(Collections.singleton(pid), 0);
                if (details != null && details.size() > 0) {
                    Illustration ill = details.get(0);
                    finalPidCollection.stream().filter(p -> p.contains(pid + "_p")).forEach(p -> {
                        File srcFile = fileMap.get(p);
                        String srcFilePath = srcFile.getPath();
                        String srcSuffix = srcFilePath.substring(srcFilePath.lastIndexOf("."));
                        String count = p.substring(p.indexOf("_p") + 2);
                        String destPath = archivePath + "/" + ill.getIllustType()
                                + ill.getAuthorPath() + ill.getFilePathWithBmkCount(Integer.valueOf(count));
                        destPath = destPath.substring(0, destPath.lastIndexOf(".")) + srcSuffix;
                        if (FilesUtils.rename(srcFile, destPath)) {
                            taskProgress.addCount(1);
                        }
                    });

                }
            });
        });
    }

    /**
     * 把图片加入转发队列（复制到转发目录）
     *
     * @param pidCollection
     * @param type
     */
    @Override
    public void addRepostQueue(Collection<String> pidCollection, String type) {
        List<Callable<Void>> tasks = new ArrayList<>();
        Map<String, File> fileMap = getFileMap(type);
        fileMap.keySet().stream().filter(pidCollection::contains).forEach(k -> {
            File srcFile = fileMap.get(k);
            String srcPath = srcFile.getPath();
            String destPath = rootPath + "/转发/" + k + srcPath.substring(srcPath.lastIndexOf("."));
            File destFile = new File(destPath);
            if (destFile.exists()) {
                log.info("文件已存在 {}", destFile);
            } else {
                tasks.add(() -> {
                    try {
                        FilesUtils.copyFile(srcFile, destFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                });
            }
        });

        TasksUtil.executeTasks(tasks, 60, fileExecutor, "file", 2);

        archivePixiv(pidCollection, type);
    }


    private static void listFiles(File file, Map<String, File> map) {
        if (file.isDirectory()) {
            //目录继续往下递归
            for (File f : Objects.requireNonNull(file.listFiles())) {
                listFiles(f, map);
            }
        } else {
            //文件 通过文件名判断来源和处理方式

            //绕过未下载完成的文件和aria临时文件
            String filePath = file.getPath();
            String ariaSuffix = ".aria2";
            File ariaFile = new File(filePath + ariaSuffix);
            if (filePath.endsWith(ariaSuffix) || ariaFile.exists()) {
                return;
            }

            //pixiv文件
            Matcher illustMatcher = ILLUSTRATED_PATTERN.matcher(file.getName());
            if (illustMatcher.find()) {
                String group = illustMatcher.group();
                if (map.containsKey(group)) {
                    File file2 = map.get(group);
                    if (file.length() == file2.length()) {
                        File deleteFile = file2.lastModified() < file.lastModified() ? file2 : file;
                        if (deleteFile.delete()) {
                            log.info("文件大小相同删除旧文件  {}", deleteFile);
                        }
                    }
                }
//                if (!file.getPath().endsWith("zip")) {
                map.put(group, file);
//                }
            } else {
                //twitter文件
                Matcher twitterMatcher = TwitterImage.PATTERN_STATUS_ID.matcher(file.getName());
                if (twitterMatcher.find()) {
                    String group = twitterMatcher.group();
                    map.put(group, file);
                }
            }
        }
    }

    @Override
    public List<DownloadingFile> getDownloadingFileList() {
        return downloadingFileList;
    }

    private static Stream<DownloadingFile> getDownloadingList(Illustration ill, String type) {
        List<DownloadingFile> list = new ArrayList<>();
        List<String> urlList = ill.getUrlList();
        List<String> filePathList = ill.getFilePathList();
        for (int i = 0; i < urlList.size(); i++) {
            list.add(new DownloadingFile(urlList.get(i), filePathList.get(i), type));
        }
        return list.stream();
    }


    @Override
    public void archiveTwitter(TwitterImage image) {
        File srcFile = getFileMap("twitter").get(image.getStatusId());
        String destPath = archivePath + "/twitter/" + image.getAuthor() + "/" + image.getFileName();
        TwitterImage twitterImage = twitterImageDAO.selectById(image.getStatusId());
        if (twitterImage == null) {
            twitterImageDAO.insert(image);
        } else {
            twitterImageDAO.updateById(image);
        }
        log.info("已保存推特图片数据");

        if (!image.getTags().contains("R-18")) {
            addRepostQueue(Collections.singleton(image.getStatusId()), "twitter");
        }

        FilesUtils.rename(srcFile, destPath);
    }

    /**
     * 自行下载或交给Aria2下载
     */
    @Override
    @Scheduled(cron = "0/30 * * * * ?")
    public void download() {
        String activeString = Aria2Json.tellActive();
        if (!StringUtils.isEmpty(activeString)) {
            removeComplete();

            //Aria2服务有效 使用Aria2下载
            String waitingString = Aria2Json.tellWaiting();

            //正在下载列表
            JSONArray activeArray = JSONObject.parseObject(activeString).getJSONArray("result");
            ArrayList<Aria2File> activeList = Aria2File.parseArray(activeArray);
            //等待队列列表
            JSONArray waitingArray = JSONObject.parseObject(waitingString).getJSONArray("result");
            ArrayList<Aria2File> waitingList = Aria2File.parseArray(waitingArray);

            activeList.addAll(waitingList);


            int numberOfAdd = MAX_CONCURRENT_DOWNLOADS - activeList.size();
            if (numberOfAdd > 0) {
                List<String> downloadingList = activeList.stream().map(Aria2File::getFileName).collect(Collectors.toList());
                downloadingFileList.forEach(f -> downloadingList.add(f.getFileName()));

                QueryWrapper<DownloadingFile> qw = new QueryWrapper<>();
                if (downloadingList.size() > 0) {
                    qw.notIn("path", downloadingList);
                }
                List<DownloadingFile> list = list(qw);
                if (list.size() == 0) {
                    return;
                }
                log.info("下载队列中有 {} 个文件", list.size());
                Collections.sort(list);
                list = list.subList(0, Math.min(numberOfAdd, list.size()));
                list.forEach(f -> downloadWithAria2(f));
            }
        }

    }

    /**
     * 重新下载出错文件
     */
//    @Scheduled(cron = "0/30 * * * * ?")
    public void fixError() {
        Map<String, File> errorMap = getFileMap("error");
        if (errorMap.size() == 0) {
            return;
        }
        List<String> pidList = errorMap.keySet().stream()
                .map(p -> p.substring(0, p.indexOf("_"))).distinct()
                .collect(Collectors.toList());

        List<Illustration> details = illustrationService.findList(pidList, 0);

        download(details, "未分类/修复");

        List<String> list = details.stream().map(Illustration::getId).collect(Collectors.toList());

        errorMap.forEach((k, v) -> {
            String pid = k.substring(0, k.indexOf("_"));
            if (list.contains(pid)) {
                if (v.delete()) {
                    log.info("删除错误文件 {}", v);
                }
            }
        });
    }

    /**
     * 保证未分类作品的详情存在
     */
//    @Scheduled(cron = "2/30 * * * * ?")
    public void getDetailsOfUntagged() {
        Map<String, File> fileMap = getFileMap("待查");
        if (fileMap.size() == 0) {
            return;
        }
        List<String> list = fileMap.keySet().stream()
                .map(k -> k.substring(0, k.indexOf("_")))
                .distinct().limit(30)
                .collect(Collectors.toList());
        List<String> details = illustrationService.findList(list, 0).stream()
                .map(Illustration::getId).collect(Collectors.toList());
        fileMap.keySet().stream()
                .filter(k -> {
                    String s = k.substring(0, k.indexOf("_"));
                    return details.contains(s);
                })
                .forEach(k -> {
                    File file = fileMap.get(k);
                    String destPath = file.getPath().replace("待查", "未分类");
                    FilesUtils.rename(file, destPath);
                });
        fileMap.keySet().stream()
                .filter(k -> {
                    String s = k.substring(0, k.indexOf("_"));
                    return !details.contains(s);
                })
                .forEach(k -> {
                    File file = fileMap.get(k);
                    String destPath = file.getPath().replace("待查", "404");
                    FilesUtils.rename(file, destPath);
                });
    }

    /**
     * 从数据库和Aria2删除已完成任务
     */
    private void removeComplete() {
        String stoppedString = Aria2Json.tellStopped();
        if (StringUtils.isEmpty(stoppedString)) {
            return;
        }
        JSONArray stoppedArray = JSONObject.parseObject(stoppedString).getJSONArray("result");
        ArrayList<Aria2File> stoppedList = Aria2File.parseArray(stoppedArray);

        //筛选出已完成且由本程序添加的任务
        List<Aria2File> completeList = stoppedList.stream()
                .filter(f -> "complete".equals(f.getStatus()))
                .filter(f -> {
                    QueryWrapper<DownloadingFile> qw = new QueryWrapper<>();
                    qw.eq("url", f.getUrl());
                    return count(qw) == 1;
                })
                .collect(Collectors.toList());
        //从数据库和Aria2删除已完成任务
        if (completeList.size() > 0) {
            QueryWrapper<DownloadingFile> qw = new QueryWrapper<>();
            qw.in("url", completeList.stream().map(Aria2File::getUrl).collect(Collectors.toList()));
            if (remove(qw)) {
                completeList.forEach(f -> {
                    String url = f.getUrl();
                    // 把zip文件打包为gif
                    if (url.endsWith(".zip") && !url.contains("fanbox")) {
                        gifExecutor.execute(() -> GifUtil.zip2Gif(f.getPath()));
                    }

                    String s = Aria2Json.removeDownloadResult(f.getGid());
                    JSONObject json = JSONObject.parseObject(s);
                    String result = json.getString("result");
                    log.info("删除已完成任务 {} -> {}", result, f.getFileName().substring(0, Math.min(20, f.getFileName().length())));
                });
            }
        }

        List<Aria2File> errorList = stoppedList.stream()
                .filter(f -> "error".equals(f.getStatus()))
                .collect(Collectors.toList());

        //有出错的任务
        if (errorList.size() > 0) {
            errorList.forEach(f -> {
                Integer errorCode = f.getErrorCode();
                if (errorCode == 3) {
                    //404错误
                    String url = f.getUrl();
                    Aria2Json.removeDownloadResult(f.getGid());
                    if (url.contains("_p")) {
                        //删除相关url
                        String pid = url.substring(url.lastIndexOf("/") + 1, url.indexOf("_p"));

                        //更新详情
                        log.info("尝试更新出错任务详情 {}", pid);
                        Illustration detail = illustrationService.getDetail(pid, 0);
                        //下载
                        if (detail.getId() != null) {
                            QueryWrapper<DownloadingFile> qw = new QueryWrapper<>();
                            qw.like("url", pid);
                            remove(qw);

                            String type = f.getPath().replace(rootPath, "");
                            type = type.substring(type.startsWith("/") ? 1 : 0, type.lastIndexOf("/"));
                            download(detail, type);
                        } else {
                            log.info("未找到任务详情 {}", pid);
                        }
                    }
                } else if (errorCode == 1 || errorCode == 2) {
                    if (f.getFileName().contains("_p")) {
                        log.info("删除出错任务并重试 {}", f.getFileName());
                        Aria2Json.removeDownloadResult(f.getGid());
                    }
                }
            });
        }
    }

    /**
     * 将文件提交Aria2下载
     *
     * @param downloadingFile 下载队列中的文件
     * @return 是否提交成功
     */
    private boolean downloadWithAria2(DownloadingFile downloadingFile) {
        Aria2Option aria2Option = new Aria2Option();
        aria2Option.setDir(rootPath + "/" + downloadingFile.getType())
                .setOut(downloadingFile.getPath())
                .setReferer("*")
        ;
        if (downloadingFile.getType().contains("fanbox")) {
            aria2Option.addHeader("Cookie", fanboxCookie);
        }

        Aria2Json aria2Json = new Aria2Json(downloadingFile.getId());
        aria2Json.setMethod(Aria2Json.METHOD_ADD_URI)
                .addParam(new String[]{downloadingFile.getUrl()})
                .addParam(aria2Option);
        ;
        String send = aria2Json.send(null);
        if (StringUtils.isEmpty(send)) {
            return false;
        }
        String path = downloadingFile.getPath();
        path = path.substring(0, path.indexOf("]") + 1);
        log.info("添加下载 {} -> {}", path, send);
        return true;
    }

}
