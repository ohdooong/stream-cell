package com.streamcell.global._common.file.service;


import com.streamcell.global._common.file.dto.FileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileResponse.FileUpload save(MultipartFile file);
}
