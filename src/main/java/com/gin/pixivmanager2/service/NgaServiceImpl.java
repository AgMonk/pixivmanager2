package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Config;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.NgaPost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static com.gin.pixivmanager2.util.PixivPost.URL_ARTWORK_PREFIX;

/**
 * @author bx002
 */
@Slf4j
@Service
public class NgaServiceImpl implements NgaService {
    private final ConfigService configService;
    private final List<Config> cookies;
    private final FileService fileService;
    private final IllustrationService illustrationService;

    public NgaServiceImpl(ConfigService configService, FileService fileService, IllustrationService illustrationService) {
        this.configService = configService;
        this.fileService = fileService;
        cookies = configService.getConfigList().stream().filter(c -> c.getName().contains("nga")).collect(Collectors.toList());
        this.illustrationService = illustrationService;
    }

    @Override
    public String repost(Collection<String> pidCollection, String fid, String action, String tid, String username) {
        String cookie = cookies.stream().filter(c -> c.getName().equals(username)).map(Config::getValue).findFirst().orElse(null);

        //pixiv作品
        List<String> pidList = pidCollection.stream()
                .filter(p -> p.contains("_p"))
                .map(p -> p.substring(0, p.indexOf("_p"))).distinct()
                .collect(Collectors.toList());
        //pixiv详情
        List<Illustration> details = illustrationService.findList(pidList, 0);
        details.sort(Comparator.comparing(Illustration::getId));

        Map<String, File> fileMap = fileService.getFileMap("转发");
        Map<String, File> repostMap = new HashMap<>();
        fileMap.forEach((k, v) -> {
            if (pidCollection.contains(k)) {
                repostMap.put(k, v);
            }
        });
        log.info("转发文件 {}", repostMap.keySet());

        NgaPost ngaPost = NgaPost.create(cookie, fid, tid, action);

        ngaPost.uploadFiles(repostMap);
        //附件
        TreeMap<String, String> attachmentsMap = ngaPost.getAttachmentsMap();

        StringBuilder sb = new StringBuilder();
        //pixiv卡片
        for (Illustration detail : details) {
            String pid = detail.getId();
            String urlCode = NgaPost.getUrlCode("Source Url", URL_ARTWORK_PREFIX + pid) + "\n";
            String tag = "标签:" + detail.getTagString() + "\n";
            String imgCode = attachmentsMap.keySet().stream()
                    .filter(k -> k.contains(pid)).map(ngaPost::getAttachmentsCode)
                    .collect(Collectors.joining("\n"));
//            tag="";
            String card = NgaPost.getCollapse(detail.getTitle(), urlCode + tag + imgCode, pid);
            sb.append(card);
        }
        String quote = NgaPost.getQuote(sb.toString());
        ngaPost.addContent(quote);
        //发送
        String send = ngaPost.send();

        if (send != null) {
            pidCollection.forEach(p -> {
                File file = fileMap.get(p);
                if (file.delete()) {
                    log.info("删除已成功转发文件 {}", file);
                } else {
                    log.info("文件删除失败 {}", file);
                }
            });
        }
        return send;
    }

    @Override
    public List<String> getUser() {
        return cookies.stream().map(Config::getName).collect(Collectors.toList());
    }


}
