package com.gin.pixivmanager2.service;

import java.util.Collection;
import java.util.List;

public interface NgaService {
    String repost(Collection<String> pidCollection,
                  String fid,
                  String action,
                  String tid, String username);

    List<String> getUser();
}
