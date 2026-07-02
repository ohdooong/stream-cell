package com.streamcell.platform.pipeline.domain.validator;

public interface PipelineValidator<T> {
    void validate(T t);
}