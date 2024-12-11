/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.exceptions;

/** Defines possible exceptions returned by Terraform version invalid. */
public class InvalidTerraformToolException extends RuntimeException {

    public InvalidTerraformToolException(String message) {
        super(message);
    }
}
