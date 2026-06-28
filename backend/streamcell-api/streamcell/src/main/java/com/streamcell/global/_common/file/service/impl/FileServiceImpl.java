package com.streamcell.global._common.file.service.impl;

import com.streamcell.global._common.file.FileUploadValidator;
import com.streamcell.global._common.file.dto.FileResponse;
import com.streamcell.global._common.file.service.FileService;
import com.streamcell.global._common.file.storage.FileStorage;
import com.streamcell.global._common.util.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileStorage fileStorage;
    private final FileUploadValidator fileUploadValidator;

    @Override
    public FileResponse.FileUpload save(MultipartFile file) {
        // 파일검증
        fileUploadValidator.validateFile(file);

        // 파일명 변경
        String originalFilename = file.getOriginalFilename();
        String convertedFileName = FileUtils.convertNameToUUID(originalFilename);
        String savedPath = fileStorage.save(file, convertedFileName);

        return FileResponse.FileUpload.from(
                originalFilename,
                convertedFileName,
                savedPath,
                file.getSize()
        );
    }


}
