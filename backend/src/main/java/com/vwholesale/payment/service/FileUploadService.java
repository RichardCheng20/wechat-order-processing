package com.vwholesale.payment.service;

import com.vwholesale.common.exception.BusinessException;
import com.vwholesale.common.security.RoleChecker;
import com.vwholesale.payment.dto.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class FileUploadService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadRoot;

    public FileUploadService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public FileUploadVO upload(MultipartFile file) {
        RoleChecker.requireBoss();
        return storeImage(file);
    }

    public FileUploadVO uploadForCustomer(MultipartFile file) {
        RoleChecker.requireCustomer();
        return storeImage(file);
    }

    private FileUploadVO storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.of(400, "请选择图片");
        }
        String ext = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw BusinessException.of(400, "仅支持 jpg/png/webp/gif 图片");
        }

        try {
            Files.createDirectories(uploadRoot);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            Path target = uploadRoot.resolve(filename);
            file.transferTo(target);
            return FileUploadVO.builder().url("/uploads/" + filename).build();
        } catch (IOException ex) {
            log.error("upload failed", ex);
            throw BusinessException.of(500, "图片上传失败");
        }
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "jpg";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
