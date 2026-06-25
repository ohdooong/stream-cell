package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.platform.pipeline.converter.PipelineConverter;
import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineService;
import com.streamcell.platform.pipeline.vo.Pipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository repository;
    private final UserLookupPort userLookupPort;

    @Override
    public PipelineResponse.Pipeline create(PipelineRequest.Create createItem) {
        // 사용자 검증
        validateUser(createItem.getOwnerUserId());

        Pipeline pipeline = PipelineConverter.toVO(createItem);
        repository.insert(pipeline);
        return PipelineConverter.toDTO(pipeline);
    }


    @Override
    public PipelineResponse.Pipeline update(PipelineRequest.Update updateItem) {
        // 사용자 검증
        validateUser(updateItem.getOwnerUserId());

        Pipeline pipeline = PipelineConverter.toVO(updateItem);
        repository.update(pipeline);
        return PipelineConverter.toDTO(pipeline);
    }

    @Override
    public PipelineResponse.Pipeline findPipelineByPipelineId(Long pipelineId) {
        return Optional.ofNullable(repository.findPipelineByPipelineId(pipelineId))
                .map(PipelineConverter::toDTO)
                .orElseThrow(() -> new BaseAPIException(ErrorCode.NOT_FOUND_PIPELINE));
    }

    private void validateUser(Long userId) {
        boolean isExistsUser =
                userLookupPort.existsByUserId(userId);
        if (!isExistsUser) {
            throw new BaseAPIException(ErrorCode.INVALID_USER);
        }
    }

}
