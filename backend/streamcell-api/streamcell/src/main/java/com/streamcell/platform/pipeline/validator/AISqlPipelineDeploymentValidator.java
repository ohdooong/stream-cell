package com.streamcell.platform.pipeline.validator;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform._common.enums.TopicPermissionType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.topic.repository.TopicRepository;
import com.streamcell.platform.topic.vo.TopicPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AISqlPipelineDeploymentValidator implements PipelineValidator<Long, Void>{

    private final TopicRepository topicRepository;
    private final PipelineRepository pipelineRepository;

    @Override
    public void validate(Long pipelineId) {

    }
}
