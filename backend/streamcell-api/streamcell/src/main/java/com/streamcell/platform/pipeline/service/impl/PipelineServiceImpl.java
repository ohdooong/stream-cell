package com.streamcell.platform.pipeline.service.impl;

import com.streamcell.global._common.enums.ErrorCode;
import com.streamcell.global._common.exception.BaseAPIException;
import com.streamcell.global._common.file.dto.FileResponse;
import com.streamcell.global._common.file.service.FileService;
import com.streamcell.platform._common.port.UserLookupPort;
import com.streamcell.platform.pipeline.converter.PipelineConverter;
import com.streamcell.platform.pipeline.dto.PipelineRequest;
import com.streamcell.platform.pipeline.dto.PipelineResponse;
import com.streamcell.platform.pipeline.enums.ArtifactType;
import com.streamcell.platform.pipeline.repository.PipelineRepository;
import com.streamcell.platform.pipeline.service.PipelineService;
import com.streamcell.platform.pipeline.vo.CustomJobConfig;
import com.streamcell.platform.pipeline.vo.Pipeline;
import com.streamcell.platform.pipeline.vo.PipelineArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PipelineServiceImpl implements PipelineService {

    private final PipelineRepository repository;
    private final UserLookupPort userLookupPort;
    private final FileService fileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Pipeline create(PipelineRequest.Create createItem) {
        // 사용자 검증
        validateUser(createItem.getOwnerUserId());

        Pipeline pipeline = PipelineConverter.toVO(createItem);
        repository.insert(pipeline);
        return PipelineConverter.toDTO(pipeline);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PipelineResponse.Artifact createFlinkCustomJar(MultipartFile file, PipelineRequest.CreateCustomJobConfig createCustomJobConfig, Long pipelineId) {
        // 파일저장
        FileResponse.FileUpload uploaded = fileService.save(file);

        // artifact 메타데이터 저장
        PipelineArtifact artifact = insertPipelineArtifact(pipelineId, uploaded);

        CustomJobConfig customJobConfigVo = PipelineConverter.toVO(createCustomJobConfig, pipelineId);

        // artifact job config 저장
        CustomJobConfig customJobConfig = insertCustomJobConfig(customJobConfigVo);

        PipelineResponse.Artifact response = PipelineConverter.toDTO(artifact);
        return response;
    }


    private PipelineArtifact insertPipelineArtifact(Long pipelineId, FileResponse.FileUpload uploaded) {
        PipelineArtifact artifact = PipelineArtifact.builder()
                .pipelineId(pipelineId)
                .artifactType(ArtifactType.CUSTOM_JAR)
                .originalFileName(uploaded.getOriginalFileName())
                .storedFileName(uploaded.getSavedFileName())
                .storedFilePath(uploaded.getSavedPath())
                .build();
        repository.insertPipelineArtifact(artifact);
        return artifact;
    }

    private CustomJobConfig insertCustomJobConfig(CustomJobConfig customJobConfig) {
        repository.insertCustomJobConfig(customJobConfig);
        return customJobConfig;
    }


    private void validateUser(Long userId) {
        boolean isExistsUser = userLookupPort.existsByUserId(userId);
        if (!isExistsUser) {
            throw new BaseAPIException(ErrorCode.INVALID_USER);
        }
    }

}
