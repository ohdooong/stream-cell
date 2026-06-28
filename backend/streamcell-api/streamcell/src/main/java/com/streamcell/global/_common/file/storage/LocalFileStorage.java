package com.streamcell.global._common.file.storage;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Component
public class LocalFileStorage implements FileStorage {

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Override
    public String save(MultipartFile file, String saveFileName) {
        Path rootPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
        Path storedPath = rootPath.resolve(saveFileName)
                .normalize();

        if (!storedPath.startsWith(rootPath)) {
            throw new RuntimeException("잘못된 파일 저장 경로입니다.");
        }

        try {
            Files.createDirectories(storedPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, storedPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new BaseAPIException(ErrorCode.FAILED_FILE_SAVE);
        }

        return storedPath.toString();
    }

}
