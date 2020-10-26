package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.DownloadingFileDAO;
import com.gin.pixivmanager2.entity.DownloadingFile;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.Request;
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
    private final static Pattern ILLUSTRATED_PATTERN = Pattern.compile("\\d+_p\\d+");
    private final static Pattern TWITTER_PATTERN = Pattern.compile("\\d+_\\d+");
    private final DownloadingFileDAO downloadingFileDAO;
    private final ThreadPoolTaskExecutor downloadExecutor;
    private final String rootPath;

    private final List<DownloadingFile> downloadingFileList = new ArrayList<>();

    public FileServiceImpl(DownloadingFileDAO downloadingFileDAO, ThreadPoolTaskExecutor downloadExecutor, ConfigService configService) {
        this.downloadingFileDAO = downloadingFileDAO;
        this.downloadExecutor = downloadExecutor;

        this.rootPath = configService.getPath("rootPath").getValue();
    }

    @Override
    public void download(Illustration illustration, String type) {
        saveBatch(getDownloadingList(illustration, type).collect(Collectors.toList()));
    }

    @Override
    public void download(Collection<Illustration> illustrations, String type) {
        saveBatch(illustrations.stream().flatMap(i -> getDownloadingList(i, type)).collect(Collectors.toList()));
    }

    private static Stream<DownloadingFile> getDownloadingList(Illustration ill, String type) {
        List<DownloadingFile> list = new ArrayList<>();
        List<String> urlList = ill.getUrlList();
        List<String> filePathList = ill.getFilePathList();
        for (int i = 0; i < urlList.size(); i++) {
            list.add(new DownloadingFile(null, urlList.get(i), filePathList.get(i), type));
        }
        return list.stream();
    }

    @Override
    public Map<String, File> getFileMap(String type) {
        HashMap<String, File> map = new HashMap<>();
        log.info("获取文件列表 {}", type);
        listFiles(new File(rootPath + "/" + type), map);
        return map;
    }

    @Scheduled(cron = "0/3 * * * * ?")
    public void startDownload() {
        if (downloadExecutor.getActiveCount() == downloadExecutor.getMaxPoolSize()) {
            return;
        }

        QueryWrapper<DownloadingFile> queryWrapper = new QueryWrapper<>();
        queryWrapper.likeRight("type", DownloadingFile.FILE_TYPE_UNTAGGED);
        List<DownloadingFile> list = list(queryWrapper);
        list.removeIf(downloadingFileList::contains);
        if (list.size() == 0) {
            queryWrapper = new QueryWrapper<>();
            queryWrapper.likeRight("type", DownloadingFile.FILE_TYPE_SEARCH_RESULTS);
            queryWrapper.last("limit 0,3");
            list = list(queryWrapper);
        }
        if (list.size() > 0) {
            list.forEach(f -> {
                downloadExecutor.execute(() -> {
                    downloadingFileList.add(f);
                    File file = new File(rootPath + "/" + f.getType() + "/" + f.getPath());
                    Request.create(f.getUrl())
                            .setReferer(null)
                            .setFile(file)
                            .setProgressMap(null)
                            .get()
                    ;
                    downloadingFileList.remove(f);
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
                map.put(group, file);
            }
        }
    }
}
