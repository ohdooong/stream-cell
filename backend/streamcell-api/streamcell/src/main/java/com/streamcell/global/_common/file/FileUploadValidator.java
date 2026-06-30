package com.streamcell.global._common.file;

import com.streamcell.global._common.util.FileUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

@Component
public class FileUploadValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jar", "png", "jpg");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("빈 파일은 업로드할 수 없습니다.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("파일 크기를 초과했습니다.");
        }

        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .orElse("");
        String extension = FileUtils.getExtension(originalFilename);
        validateExtension(extension);
    }

    private void validateExtension(String extension) {
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("허용되지 않은 확장자입니다.");
        }
    }
}
