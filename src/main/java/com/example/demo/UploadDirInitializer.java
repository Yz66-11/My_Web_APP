package com.example.demo;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class UploadDirInitializer {

    private static final Logger log = LoggerFactory.getLogger(UploadDirInitializer.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    @PostConstruct
    public void init() {
        try {
            // Resolve to absolute path
            Path absPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            log.info("上传目录配置值: {}", uploadDir);
            log.info("上传目录绝对路径: {}", absPath);

            File dir = absPath.toFile();
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                log.info("上传目录不存在，已创建: {} (结果: {})", absPath, created);
            } else {
                log.info("上传目录已存在: {}", absPath);
            }

            // Test write permission
            if (dir.exists()) {
                boolean writable = Files.isWritable(absPath);
                log.info("上传目录是否可写: {}", writable);
                if (!writable) {
                    log.error("上传目录不可写！请检查权限: {}", absPath);
                }
            }

            // Create subdirectories
            String[] subDirs = {"avatars", "checkin", "posts"};
            for (String sub : subDirs) {
                File subDir = new File(absPath.toFile(), sub);
                if (!subDir.exists()) {
                    boolean created = subDir.mkdirs();
                    log.info("子目录 {}/{} 已创建: {}", absPath, sub, created);
                }
            }

            // Store the resolved absolute path for use in upload methods
            // (this is informational only; the @Value already resolved the path)
            System.setProperty("app.upload.dir.resolved", absPath.toString());

        } catch (Exception e) {
            log.error("上传目录初始化失败: {}", e.getMessage(), e);
        }
    }
}
