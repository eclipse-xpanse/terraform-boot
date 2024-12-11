/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.exceptions;

/** Used to indicate Terraform health check anomalies. */
public class TerraformHealthCheckException extends RuntimeException {

    public TerraformHealthCheckException(String message) {
        super(message);
    }
}
