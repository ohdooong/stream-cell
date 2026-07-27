package com.streamcell.platform.flink.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class FlinkUtils {

    /**
     * Flink jar id는 return받은 filename에서 가장 마지막 .jar로 끝나는 문자열이 jar id이다.
     *
     * @param filename
     * @return flink jar id
     */
    public static String extractFlinkJarId(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Flink JAR filename이 없습니다.");
        }

        Path path = Paths.get(filename);
        Path fileName = path.getFileName();

        if (fileName == null) {
            throw new IllegalArgumentException("Flink JAR ID를 추출할 수 없습니다.");
        }
        return fileName.toString();
    }
}
