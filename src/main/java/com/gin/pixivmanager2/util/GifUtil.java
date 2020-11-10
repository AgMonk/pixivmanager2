package com.gin.pixivmanager2.util;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 制作GIf文件
 *
 * @author bx002
 * @date 2020/11/10 10:42
 */
@Slf4j
public class GifUtil {

    public static void images2Gif(List<File> images, String outputPath, Integer delay) throws IOException {
        long start = System.currentTimeMillis();
        delay = delay == null || delay <= 0 ? 30 : delay;
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(outputPath);
        encoder.setRepeat(1);
        encoder.setDelay(delay);
        for (File imageFile : images) {
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            BufferedImage zoomImage = new BufferedImage(width, height, 3);
            Image image = bufferedImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            Graphics gc = zoomImage.getGraphics();
            gc.setColor(Color.WHITE);
            gc.drawImage(image, 0, 0, null);
            encoder.addFrame(zoomImage);
            log.debug("添加文件 {}", imageFile);
        }
        encoder.finish();
        File outFile = new File(outputPath);
        BufferedImage image = ImageIO.read(outFile);
        ImageIO.write(image, outFile.getName(), outFile);
        long end = System.currentTimeMillis();
        log.info("gif生成完毕 用时 {}秒 {} ", String.format("%.1f", 1.0 * (end - start) / 1000), outFile);
    }

    public static void zip2Gif(String zipFilePath) {
        try {
            String gifFilePath = zipFilePath.replace(".zip", ".gif").replace("_p0", "_p1");
            File unZippedFile = ZipUtil.unZip(zipFilePath);
            GifUtil.images2Gif(Arrays.asList(Objects.requireNonNull(unZippedFile.listFiles())), gifFilePath, 30);
            FilesUtil.delete(unZippedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        File file = new File("F:\\illust\\未分类");
        List<File> zipFiles = Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.getPath().endsWith("zip")).collect(Collectors.toList());
        zipFiles.forEach(f -> zip2Gif(f.getPath()));
    }
}
