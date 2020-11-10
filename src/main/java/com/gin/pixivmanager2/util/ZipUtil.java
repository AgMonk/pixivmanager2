package com.gin.pixivmanager2.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 压缩/解压文件
 *
 * @author bx002
 * @date 2020/11/10 10:22
 */
@Slf4j
public class ZipUtil {
    /**
     * 解压简单压缩包
     *
     * @param zipFilePath
     * @return java.io.File
     * @author bx002
     * @date 2020/11/10 10:26
     */
    public static File unZip(String zipFilePath, String destDirPath) throws IOException {
        long start = System.currentTimeMillis();
        ZipFile zipFile = new ZipFile(zipFilePath, StandardCharsets.UTF_8);
        File destDir = new File(destDirPath);
        if (!destDir.exists()) {
            if (destDir.mkdirs()) {
                log.info("创建文件夹 {}", destDir);
            }
        }
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry zipEntry = entries.nextElement();

            InputStream inputStream = zipFile.getInputStream(zipEntry);
            String destPath = destDirPath + "/" + zipEntry.getName();

            FileOutputStream fileOutputStream = new FileOutputStream(destPath);
            log.debug("解压文件 {} 大小 {}K", zipEntry.getName(), zipEntry.getSize());
            byte[] buffer = new byte[4096];
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, len);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
            inputStream.close();
        }
        long end = System.currentTimeMillis();
        log.info("解压完毕 用时 {}秒 {} ", String.format("%.1f", 1.0 * (end - start) / 1000), zipFilePath);
        return destDir;
    }

    /**
     * 解压简单压缩包
     *
     * @param zipFilePath
     * @return java.io.File
     * @author bx002
     * @date 2020/11/10 10:26
     */
    public static File unZip(String zipFilePath) throws IOException {
        return unZip(zipFilePath, zipFilePath.substring(0, zipFilePath.lastIndexOf(".")));
    }

}
