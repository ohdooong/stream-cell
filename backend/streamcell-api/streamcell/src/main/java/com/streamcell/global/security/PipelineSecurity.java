package com.streamcell.global.security;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.vo.Pipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PipelineSecurity {

    private final PipelineRepository pipelineRepository;

    public boolean isOwner(Long pipelineId, Long userId) {
        Long ownerUserId = pipelineRepository.findPipelineByPipelineId(pipelineId)
                .map(Pipeline::getOwnerUserId)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
        return ownerUserId.equals(userId);
    }

}
