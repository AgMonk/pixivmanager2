package com.gin.pixivmanager2.service;

import com.gin.pixivmanager2.entity.Illustration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.List;

public interface SearchService {

    List<Illustration> search(String keyword, Integer p, String mode, boolean notBmkOnly, boolean searchTitle);

    List<Illustration> search(Collection<String> keywords, Integer start, Integer end, String mode, boolean notBmkOnly, boolean searchTitle);

//    @Scheduled(cron = "0 3/10 * * * ?")
    void autoSearch();
}
