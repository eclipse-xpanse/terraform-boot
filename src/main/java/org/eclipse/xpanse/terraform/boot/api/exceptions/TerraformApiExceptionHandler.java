/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api.exceptions;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.terraform.boot.models.exceptions.UnsupportedEnumValueException;
import org.eclipse.xpanse.terraform.boot.models.response.Response;
import org.eclipse.xpanse.terraform.boot.models.response.ResultType;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Exception handler for exceptions thrown by the methods called by the API controller.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TerraformApiExceptionHandler {

    /**
     * Exception handler for TerraformExecutorException.
     */
    @ExceptionHandler({TerraformExecutorException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ResponseBody
    public Response handleTerraformExecutorException(
            TerraformExecutorException ex) {
        return Response.errorResponse(ResultType.TERRAFORM_EXECUTION_FAILED,
                Collections.singletonList(ex.getMessage()));
    }

    /**
     * Exception handler for UnsupportedEnumValueException.
     */
    @ExceptionHandler({UnsupportedEnumValueException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public Response handleUnsupportedEnumValueException(
            UnsupportedEnumValueException ex) {
        return Response.errorResponse(ResultType.UNSUPPORTED_ENUM_VALUE,
                Collections.singletonList(ex.getMessage()));
    }

    /**
     * Exception handler for MethodArgumentTypeMismatchException.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public Response handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        return Response.errorResponse(ResultType.UNPROCESSABLE_ENTITY,
                Collections.singletonList(ex.getMessage()));
    }
}
