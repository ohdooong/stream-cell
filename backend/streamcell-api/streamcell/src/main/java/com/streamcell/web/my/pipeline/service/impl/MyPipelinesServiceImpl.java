package com.streamcell.web.my.pipeline.service.impl;

import com.streamcell.web.my.pipeline.converter.MyPipelineConverter;
import com.streamcell.web.my.pipeline.dto.MyPipelineResponse;
import com.streamcell.web.my.pipeline.repository.MyPipelinesRepository;
import com.streamcell.web.my.pipeline.service.MyPipelinesService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MyPipelinesServiceImpl implements MyPipelinesService {

    private final MyPipelinesRepository repository;

    @Override
    public List<MyPipelineResponse.Pipeline> findPipelinesByUserId(Long userId) {
        return repository.findPipelinesByUserId(userId)
                .stream()
                .map(MyPipelineConverter::toDTO)
                .toList();
    }
}
