package com.gin.pixivmanager2.service;

import com.alibaba.fastjson.JSONObject;
import com.gin.pixivmanager2.dao.IllustrationDAO;
import com.gin.pixivmanager2.entity.Illustration;
import com.gin.pixivmanager2.util.PixivPost;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author bx002
 */
@Service
public class IllustrationServiceImpl implements IllustrationService {
    private final IllustrationDAO illustrationDAO;
    private final ConfigService configService;

    private final String cookie;
    private final String  tt;

    /**
     * 作品详情缓存
     */
    private final Map<String, Illustration> illustrationMap = new HashMap<>();

    public IllustrationServiceImpl(IllustrationDAO illustrationDAO, ConfigService configService) {
        this.illustrationDAO = illustrationDAO;
        this.configService = configService;

        cookie = configService.getCookie("pixiv").getValue();;
        tt = configService.getConfig("tt").getValue();
    }

    /**
     * 根据id查询一个作品
     *
     * @param id
     * @return
     */
    @Override
    public Illustration findOneById(String id) {
        Illustration ill = illustrationMap.get(id);
        if (ill == null) {
            ill = illustrationDAO.selectById(id);
        }
        if (ill==null ) {
            JSONObject detail = PixivPost.detail(id, null);
            if (detail!=null) {
                ill = Illustration.parse(detail);
            }
        }

        illustrationMap.put(id, ill);
        return ill;
    }

    /**
     * 根据多个id查询作品
     *
     * @param idIn
     * @return
     */
    @Override
    public List<Illustration> findIDin(Collection<String> idIn) {
        return null;
    }

    /**
     * 保存一个作品
     *
     * @param collection
     * @return
     */
    @Override
    public Integer save(Collection<Illustration> collection) {
        return null;
    }
}
