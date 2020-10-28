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
    private final DownloadingFileDAO downloadingFileDAO;
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final String rootPath;
    private final String archivePath;
    private final String fanboxCookie;

    private final List<DownloadingFile> downloadingFileList = new ArrayList<>();

    public FileServiceImpl(DownloadingFileDAO downloadingFileDAO, ThreadPoolTaskExecutor downloadExecutor, ConfigService configService) {
        this.downloadingFileDAO = downloadingFileDAO;
        this.downloadExecutor = downloadExecutor;

        this.rootPath = configService.getPath("rootPath").getValue();
        this.archivePath = rootPath + "/archive";
        this.fanboxCookie = configService.getCookie("fanbox").getValue();
    }

    @Override
    public void download(Illustration illustration, String type) {
        saveBatch(getDownloadingList(illustration, type).collect(Collectors.toList()));
    }

    @Override
    public void download(Collection<Illustration> illustrations, String type) {
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
        //带_p的pid转为不带_p的pid并去重
        List<String> list = pidCollection.stream().map(s -> s.substring(0, s.indexOf("_"))).distinct().collect(Collectors.toList());
        IllustrationService illustrationService = SpringContextUtil.getBean(IllustrationService.class);
        List<Illustration> illustrationList = illustrationService.findList(list, 0);
        illustrationList.forEach(i -> {
            pidCollection.stream().filter(s -> s.contains(i.getId())).forEach(s -> {
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

    @Scheduled(cron = "0/3 * * * * ?")
    public void startDownload() {
        if (downloadExecutor.getActiveCount() == downloadExecutor.getMaxPoolSize()) {
            return;
        }

        QueryWrapper<DownloadingFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("type", DownloadingFile.FILE_TYPE_UNTAGGED);
        List<DownloadingFile> list = list(queryWrapper);
        if (list.size() == 0) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("type", DownloadingFile.FILE_TYPE_FANBOX);
            queryWrapper.last("limit 0,3");
            list = list(queryWrapper);
        }
        if (list.size() == 0) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("type", DownloadingFile.FILE_TYPE_SEARCH_RESULTS);
            queryWrapper.last("limit 0,3");
            list = list(queryWrapper);
        }
        list.removeIf(downloadingFileList::contains);
        if (list.size() > 0) {
            list.forEach(f -> {
                downloadExecutor.execute(() -> {
                    downloadingFileList.add(f);
                    File file = new File(rootPath + "/" + f.getType() + "/" + f.getPath());
                    Request.create(f.getUrl())
                            .setReferer(null)
                            .setCookie(f.getType().contains("fanbox") ? fanboxCookie : "")
                            .setFile(file)
                            .setProgressMap(f.getProgress())
                            .get()
                    ;
                    downloadingFileList.removeIf(d -> d.getId().equals(f.getId()));
                    log.info("下载完毕 {}", file);
                    if (file.exists()) {
                        removeById(f.getId());
                    }
                });
            });
        } else {
            //下载列表为空 休息
            try {
                Thread.sleep(30 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
                if (!file.getPath().endsWith("zip")) {
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
}
