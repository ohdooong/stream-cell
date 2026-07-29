package com.streamcell.platform.pipeline.validator;

public interface PipelineValidator<T, R> {
    default void validate(T t) {};
    default void validate(T t, R r) {};
}