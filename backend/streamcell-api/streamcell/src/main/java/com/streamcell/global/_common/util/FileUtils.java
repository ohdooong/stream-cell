package com.streamcell.global._common.util;

import java.util.UUID;

public class FileUtils {

    private FileUtils() {}

    public static String convertNameToUUID(String fileName) {
        StringBuilder sb = new StringBuilder();
        UUID uuid = UUID.randomUUID();
        String extension = getExtension(fileName);
        sb.append(uuid)
          .append(".")
          .append(extension);

        return sb.toString();
    }

    public static String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new RuntimeException("파일명이 비어 있습니다.");
        }
        
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new RuntimeException("확장자가 없는 파일입니다.");
        }

        return fileName.substring(dotIndex + 1);
    }
}
