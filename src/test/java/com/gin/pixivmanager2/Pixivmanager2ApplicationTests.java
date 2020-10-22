package com.gin.pixivmanager2;

import com.gin.pixivmanager2.dao.IllustrationDAO;
import com.gin.pixivmanager2.entity.Illustration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class Pixivmanager2ApplicationTests {
    @Autowired
    private IllustrationDAO illustrationDAO;

    @Test
    void contextLoads() {
        Illustration illustration = illustrationDAO.selectById(85157892);
        System.err.println(illustration);

    }

}
