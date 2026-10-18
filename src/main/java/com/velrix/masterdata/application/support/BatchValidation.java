package com.velrix.masterdata.application.support;

import com.velrix.shared.api.ApiCodes;
import com.velrix.shared.exception.BizException;

import java.util.ArrayList;
import java.util.List;

public final class BatchValidation {

    private final List<String> errors = new ArrayList<>();

    public void add(String message) {
        if (message != null && !message.isBlank()) {
            errors.add(message.trim());
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void throwIfAny() {
        if (hasErrors()) {
            throw new BizException(ApiCodes.BIZ_ERROR, String.join("；", errors));
        }
    }
}
