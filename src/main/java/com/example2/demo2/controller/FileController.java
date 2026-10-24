package com.example2.demo2.controller;

import com.example2.demo2.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 夏辰义
 * 2026/8/2622:23
 */

@Slf4j
@RestController
@RequestMapping("/upload")
public class FileController {

    @Value("${upload.path:${user.dir}/uploads}")
    private String uploadPath;

    @Value("${file.base-url:http://localhost:8080}")
    private String baseUrl;

    @Operation(summary = "上传文件")
    @PostMapping
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("只能上传图片文件");
        }




        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                return Result.error("文件名不能为空");
            }
            int dot = originalName.lastIndexOf('.');
            if (dot < 0) {
                return Result.error("文件缺少扩展名");
            }
            String ext = originalName.substring(dot).toLowerCase(Locale.ROOT);

            if (!Set.of(".jpg",".jpeg",".png",".gif").contains(ext)) {
                return Result.error("只支持 jpg/jpeg/png/gif");
            }

            String newName = UUID.randomUUID() + ext;

            //目标路径
            Path targetPath = Paths.get(uploadPath, newName);
            //路径安全效验
            //将基础目录转化为标准化绝对路径
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            //将最终文件转化为标准化绝对路径
            Path fullPath = targetPath.toAbsolutePath().normalize();
            //判断目标路径是否以基础路径为前缀
            if (!fullPath.startsWith(baseDir)) {
                log.error("检测到非法路径穿越攻击！目标路径：{}", fullPath);
                return Result.error("上传失败：非法的文件存储路径");
            }

            //magic bytes 真解码
            byte[] bytes = file.getBytes();

// 1. 只读文件头拿尺寸，不解码像素
            int width, height;
            try (var iis = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(iis);
                if (!readers.hasNext()) {
                    return Result.error("文件内容不是有效图片");
                }
                var reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                } finally {
                    reader.dispose();
                }
            }

// 2. 超限直接拒：2500 万像素 × 4 字节 ≈ 100MB 解码内存
            if ((long) width * height > 25_000_000L) {
                return Result.error("图片尺寸过大（最大 5000×5000）");
            }

// 3. 解码 + 重新编码
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                return Result.error("文件内容不是有效图片");
            }
            String format = ext.equals(".jpeg") ? "jpg" : ext.substring(1);
            if (!ImageIO.write(img, format, fullPath.toFile())) {
                return Result.error("图片保存失败");
            }

// ⛔ 不要在这里再 Files.copy(bytes, fullPath)


            String url = baseUrl + "/uploads/" + newName;
            log.info("文件上传成功: {}", url);

            return Result.success(url);

        } catch (Exception e) {
            log.error("文件上传失败", e);
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}