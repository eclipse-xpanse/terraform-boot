/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.exceptions;

/** Defines possible exceptions returned by Terraform scripts invalid. */
public class InvalidTerraformScriptsException extends RuntimeException {

    public InvalidTerraformScriptsException(String message) {
        super(message);
    }
}
