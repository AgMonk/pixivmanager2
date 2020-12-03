package com.gin.pixivmanager2.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * @author bx002
 */
@Slf4j
public class FilesUtils {
    public static void copyFile(File source, File dest) throws IOException {
        if (source.getPath().equals(dest.getPath())) {
            return;
        }
        File parentFile = dest.getParentFile();
        if (!parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                log.error("创建文件夹失败 {}", parentFile.getPath());
            }
        }

        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            log.info("复制文件 {} >> {}", source, dest);
        } finally {
            assert inputChannel != null;
            inputChannel.close();
            assert outputChannel != null;
            outputChannel.close();
        }
    }

    public static void delete(File file) {
        File[] files = file.listFiles();
        if (file.isDirectory() && files != null) {
            for (File f : files) {
                delete(f);
            }
        }
        if (file.delete()) {
            log.debug("删除文件 {}", file);
        } else {
            log.warn("删除失败 {}", file);
        }
    }

    public static boolean rename(File srcFile, String destPath) {
        File destFile = new File(destPath);
        File parentFile = destFile.getParentFile();
        boolean b = true;
        //如果父目录不存在 创建
        if (!parentFile.exists()) {
            if ((b = parentFile.mkdirs())) {
                log.info("创建路径: {}", parentFile.getPath());
            } else {
                log.warn("创建路径失败: {}", parentFile.getPath());

            }
        }
        //如果目标文件存在
        if (destFile.exists()) {
            //如果来源和目标相同，不进行操作
            if (srcFile.getPath().equals(destPath)) {
                log.info("来源和目标指向一致 不操作");
                b = false;
            } else
                //如果已存在文件和目标名称和大小相同，删除目标
                if (srcFile.length() == destFile.length()) {
                    log.info("来源和目标名称和大小相同，删除目标 {}", (b = destFile.delete()) ? "成功" : "失败");
                } else {
                    //如果已存在文件和目标名称相同，大小不同，目标改名直到不同
                    int index = destPath.lastIndexOf(".");
                    String fileName = destPath.substring(index);
                    String suffix = destPath.substring(index);
                    int i = 2;
                    do {
                        destFile = new File(fileName + "_(" + i + ")" + suffix);
                        i++;
                    } while (destFile.exists());
                }
        }

        //移动文件
        if (b && (b = srcFile.renameTo(destFile))) {
            log.info("改名文件 {} -> {}", srcFile.getName(), destPath);
        } else {
            log.warn("改名失败 {} -> {}", srcFile.getName(), destPath);
        }
        //删除源目录
        parentFile = srcFile.getParentFile();
        File[] listFiles = parentFile.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            if (b && parentFile.delete()) {
                log.info("删除目录 {}", parentFile.getPath());
            } else {
                log.info("删除失败 {}", parentFile.getPath());
            }
        }
        return b;
    }
}
