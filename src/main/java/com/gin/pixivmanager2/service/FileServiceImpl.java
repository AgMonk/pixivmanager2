package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.DownloadingFileDAO;
import com.gin.pixivmanager2.entity.DownloadingFile;
import com.gin.pixivmanager2.entity.FanboxItem;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.Request;
import com.gin.pixivmanager2.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Transactional
@Service
public class FileServiceImpl extends ServiceImpl<DownloadingFileDAO, DownloadingFile> implements FileService {
    public final static Pattern ILLUSTRATED_PATTERN = Pattern.compile("\\d+_p\\d+");
    private final static Pattern TWITTER_PATTERN = Pattern.compile("\\d+_\\d+");
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final String rootPath;
    private final String archivePath;
    private final String fanboxCookie;

    private final List<DownloadingFile> downloadingFileList = new ArrayList<>();

    public FileServiceImpl(ThreadPoolTaskExecutor downloadExecutor, ConfigService configService) {
        this.downloadExecutor = downloadExecutor;

        this.rootPath = configService.getPath("rootPath").getValue();
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
        log.info("添加下载队列 {}个", sum);
        saveBatch(illustrations.stream().flatMap(i -> getDownloadingList(i, type)).collect(Collectors.toList()));
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
    public void archive(Collection<String> pidCollection, String type) {
        Map<String, File> fileMap = getFileMap(type);
        //如果为null则归档全部
        pidCollection = pidCollection == null ? fileMap.keySet() : pidCollection;
        //带_p的pid转为不带_p的pid并去重
        List<String> list = pidCollection.stream().map(s -> s.substring(0, s.contains("_") ? s.indexOf("_") : s.length())).distinct().collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);
        List<Illustration> illustrationList = illustrationService.findList(list, 0, false);
        Collection<String> finalPidCollection = pidCollection;
        illustrationList.forEach(i -> {
            finalPidCollection.stream().filter(s -> s.contains(i.getId())).forEach(s -> {
                String count = s.substring(s.indexOf("_p") + 2);
                String destPath = archivePath + "/" + i.getIllustType() + i.getAuthorPath() + i.getFilePathWithBmkCount(Integer.valueOf(count));
                File destFile = new File(destPath);
                File srcFile = fileMap.get(s);

                if (destFile.exists()) {
                    if (destFile.length() == srcFile.length()) {
                        if (srcFile.delete()) {
                            log.info("目标文件存在且与源文件大小相同 删除源文件");
                            return;
                        }
                    } else {
                        while (destFile.exists()) {
                            destPath += ".bak";
                            destFile = new File(destPath);
                        }
                    }
                }
                File parentFile = destFile.getParentFile();
                if (!parentFile.exists()) {
                    if (parentFile.mkdirs()) {
                        log.info("创建路径: {}", parentFile.getPath());
                    }
                }

                if (srcFile.renameTo(destFile)) {
                    log.info("归档文件 {} {}", s, destPath);
                } else {
                    log.info("归档失败 {} {}", s, destPath);
                }

                parentFile = srcFile.getParentFile();
                File[] listFiles = parentFile.listFiles();
                if (listFiles == null || listFiles.length == 0) {
                    if (parentFile.delete()) {
                        log.info("删除目录 {}", parentFile.getPath());
                    } else {
                        log.info("删除失败 {}", parentFile.getPath());
                    }
                }
            });
        });

    }

    @Override
    @Scheduled(cron = "0/10 * * * * ?")
    public void startDownload() {
        int maxPoolSize = downloadExecutor.getMaxPoolSize();
        int activeCount = downloadExecutor.getActiveCount();
        if (activeCount >= maxPoolSize) {
            return;
        }
        List<String> downloadingId = downloadingFileList.stream().map(DownloadingFile::getId).collect(Collectors.toList());
        QueryWrapper<DownloadingFile> queryWrapper = new QueryWrapper<>();
        if (downloadingId.size() > 0) {
            queryWrapper.notIn("id", downloadingId);
        }
        List<DownloadingFile> list = list(queryWrapper);
        Collections.sort(list);
        list = list.subList(0, Math.min(list.size(), maxPoolSize - activeCount));

        if (list.size() > 0) {
            log.info("载入下载队列 {}个", list.size());
            list.forEach(f -> {
                downloadExecutor.execute(() -> {
                    synchronized (downloadingFileList) {
                        downloadingFileList.add(f);
                    }
                    File file = new File(rootPath + "/" + f.getType() + "/" + f.getPath());
                    Request.create(f.getUrl())
                            .setReferer(null)
                            .setCookie(f.getType().contains("fanbox") ? fanboxCookie : "")
                            .setFile(file)
                            .setProgressMap(f.getProgress())
                            .get()
                    ;
                    synchronized (downloadingFileList) {
                        downloadingFileList.removeIf(d -> d.getId().equals(f.getId()));
                    }
                    if (file.exists()) {
                        String path = file.getPath().replace("\\", "/").replace(rootPath, "");
                        log.info("下载完毕 {}", path.substring(0, path.contains("]") ? path.indexOf("]") + 1 : Math.min(30, path.length())));
                        removeById(f.getId());
                    } else {
                        Matcher matcher = ILLUSTRATED_PATTERN.matcher(f.getUrl());
                        if (matcher.find()) {
                            String group = matcher.group();
                            String id = group.substring(0, group.indexOf("_"));
                            log.warn("下载失败 {}", "https://www.pixiv.net/artworks/" + id);

                            log.info("尝试更新作品详情 {}", id);
                            IllustrationServiceImpl service = SpringContextUtil.getBean(IllustrationServiceImpl.class);
                            Illustration detail = service.getDetail(id, null);
                            //删除已有下载url
                            QueryWrapper<DownloadingFile> qw = new QueryWrapper<>();
                            qw.like("url", id);
                            remove(qw);
                            //重新添加下载url
                            download(detail, "未分类");
                        } else {
                            log.warn("下载失败 {}", file);
                        }
                    }
                });
            });
        }

    }


    private static void listFiles(File file, Map<String, File> map) {
        if (file.isDirectory()) {
            //目录继续往下递归
            for (File f : Objects.requireNonNull(file.listFiles())) {
                listFiles(f, map);
            }
        } else {
            //文件 通过文件名判断来源和处理方式

            //pixiv文件
            Matcher matcher = ILLUSTRATED_PATTERN.matcher(file.getName());
            if (matcher.find()) {
                String group = matcher.group();
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
}
