package com.streamcell.platform.ai.domain.validator;

public interface Validator<T> {

    void validate(T target);

}
