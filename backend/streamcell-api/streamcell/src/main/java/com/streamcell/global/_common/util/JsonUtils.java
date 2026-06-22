package com.streamcell.global._common.util;

import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.enums.ErrorCode;
import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static boolean isIncludedFrom(String rawJsonString, String key) {
        Map<String, Object> parseMap = parseJsonToMap(rawJsonString);
        return parseMap.containsKey(key);
    }

    public static Map<String, Object> parseJsonToMap(String rawJsonString) {
        return objectMapper.readValue(rawJsonString
            , new TypeReference<Map<String, Object>>() {
            });
    }

}
