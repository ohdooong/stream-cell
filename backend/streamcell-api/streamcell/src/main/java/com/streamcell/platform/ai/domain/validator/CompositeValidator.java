package com.streamcell.platform.ai.domain.validator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeValidator<T> implements Validator<T> {

    private final List<Validator<T>> validators = new ArrayList<>();

    public CompositeValidator<T> add(Validator<T> validator) {
        this.validators.add(validator);
        return this;
    }

    public CompositeValidator<T> addAll(Validator<T>... validators) {
        this.validators.addAll(Arrays.asList(validators));
        return this;
    }

    @Override
    public void validate(T target) {
        for (Validator<T> validator : validators) {
            validator.validate(target);
        }
    }
}
