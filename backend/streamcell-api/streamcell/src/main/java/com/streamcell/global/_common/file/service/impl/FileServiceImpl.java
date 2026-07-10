package com.streamcell.global._common.file.service.impl;

import com.streamcell.global._common.file.FileUploadValidator;
import com.streamcell.global._common.file.dto.FileResponse;
import com.streamcell.global._common.file.service.FileService;
import com.streamcell.global._common.file.storage.FileStorage;
import com.streamcell.global._common.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileStorage fileStorage;
    private final FileUploadValidator fileUploadValidator;

    @Value("${file.upload.dir}")
    private String uploadDir;

    @Value("${file.upload.custom-jar-path}")
    private String customJarPath;

    @Override
    public FileResponse.FileUpload save(MultipartFile file) {
        // 파일검증
        fileUploadValidator.validateFile(file);

        // 파일명 변경
        String originalFilename = file.getOriginalFilename();
        String convertedFileName = FileUtils.convertNameToUUID(originalFilename);

        Path rootPath = Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize();
        Path storedPath = rootPath.resolve(convertedFileName)
                .normalize();

        String savedPath = fileStorage.save(file, convertedFileName, storedPath);

        return FileResponse.FileUpload.from(
                originalFilename,
                convertedFileName,
                savedPath,
                file.getSize()
        );
    }

    @Override
    public FileResponse.FileUpload saveCustomJar(MultipartFile file, Long pipelineId) {
        // 파일검증
        fileUploadValidator.validateJarFile(file);

        // 파일명 변경
        String originalFilename = file.getOriginalFilename();
        String convertedFileName = FileUtils.convertNameToUUID(originalFilename);

        Path rootPath = Paths.get(customJarPath, pipelineId.toString())
                .toAbsolutePath()
                .normalize();
        Path storedPath = rootPath.resolve(convertedFileName)
                .normalize();

        String savedPath = fileStorage.save(file, convertedFileName, storedPath);

        return FileResponse.FileUpload.from(
                originalFilename,
                convertedFileName,
                savedPath,
                file.getSize()
        );
    }


}
