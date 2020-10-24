package com.gin.pixivmanager2.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gin.pixivmanager2.dao.DownloadingFileDAO;
import com.gin.pixivmanager2.entity.DownloadingFile;
import com.gin.pixivmanager2.entity.Illustration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Transactional
@Service
public class FileServiceImpl extends ServiceImpl<DownloadingFileDAO, DownloadingFile> implements FileService {
    private final DownloadingFileDAO downloadingFileDAO;

    public FileServiceImpl(DownloadingFileDAO downloadingFileDAO) {
        this.downloadingFileDAO = downloadingFileDAO;
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
}
