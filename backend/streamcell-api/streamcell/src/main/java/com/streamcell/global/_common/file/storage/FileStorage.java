package com.streamcell.global._common.file.storage;

import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorage {
    String save(MultipartFile file, String saveFileName, Path storedPath);
}
