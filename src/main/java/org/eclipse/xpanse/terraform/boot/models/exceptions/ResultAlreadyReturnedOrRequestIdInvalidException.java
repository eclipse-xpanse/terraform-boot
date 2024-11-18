/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.exceptions;

/**
 * Exception thrown when store failed callback response.
 */
public class ResultAlreadyReturnedOrRequestIdInvalidException extends RuntimeException {
    public ResultAlreadyReturnedOrRequestIdInvalidException(String message) {
        super(message);
    }
}

