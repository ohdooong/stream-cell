package com.streamcell.platform.pipeline.validator;

public interface PipelineValidator<T> {
    void validate(T t);
}