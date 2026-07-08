package com.streamcell.platform.pipeline.domain.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform._common.enums.TopicPermissionType;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.vo.TopicPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.regex.Pattern;

@Component("customJobConfigValidator")
@RequiredArgsConstructor
public class CustomJobConfigValidator implements PipelineValidator<CustomJobConfig>{

    private static final String PKG_REGEX = "^[a-zA-Z_$][a-zA-Z0-9_$]*(\\.[a-zA-Z_$][a-zA-Z0-9_$]*)*$";
    private static final Pattern PKG_PATTERN = Pattern.compile(PKG_REGEX);

    private final TopicRepository topicRepository;

    @Override
    public void validate(CustomJobConfig customJobConfig) {
        // entryClass 패키지경로 패턴인지 확인
        // com.example.MyJob -> 이런형태여야함.
        String entryClass = customJobConfig.getEntryClass();
        boolean matches = PKG_PATTERN.matcher(entryClass).matches();
        if (!matches) {
            throw new BaseAPIException(ErrorCode.INVALID_ENTRY_CLASS);
        }

        // Topic 유효한지 검증
        List<Long> inputTopicIds = customJobConfig.getInputTopicIds()
                .stream()
                .distinct()
                .toList();

        inputTopicIds.forEach(topicId -> {
            topicRepository.findById(topicId)
                    .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_TOPIC));
        });

        // parallelism 검증
        Integer parallelism = customJobConfig.getParallelism();
        if (parallelism < 1 || parallelism > 8) {
            throw new BaseAPIException(ErrorCode.INVALID_PARALLELISM);
        }

        // Topic Permission 검증
        Long userId = customJobConfig.getUserId();

        List<TopicPermission> topicPermissions = topicRepository.findTopicPermissionByUserId(userId);
        List<Long> topics = topicPermissions
                .stream()
                .map(TopicPermission::getTopicId)
                .distinct()
                .toList();
        // 분석가능한 토픽에 요청 토픽이 포함되어있는지
        if (!topics.containsAll(inputTopicIds)) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_TOPICS);
        }

        // TopicPermissionType이 DEPLOY || ADMIN 일때만
        boolean match = topicPermissions
                .stream()
                .map(TopicPermission::getTopicPermissionType)
                .distinct()
                .anyMatch(type -> TopicPermissionType.DEPLOY == type || TopicPermissionType.ADMIN == type);
        if (!match) {
            throw new BaseAPIException(ErrorCode.FORBIDDEN_TOPICS);
        }


    }
}
