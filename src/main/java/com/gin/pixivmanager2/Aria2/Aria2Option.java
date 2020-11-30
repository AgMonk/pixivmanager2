package com.gin.pixivmanager2.Aria2;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Aria2下载参数对象
 *
 * @author bx002
 * @date 2020/11/28 17:32
 */
@Data
@Accessors(chain = true)
public class Aria2Option {
    /**
     * 下载根目录
     */
    String dir;
    /**
     * 文件名
     */
    String out;
    String referer;

}
