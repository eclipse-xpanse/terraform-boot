/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.response;

import lombok.Builder;
import lombok.Data;

/**
 * Data model for the Terraform command execution results.
 */
@Data
@Builder
public class TerraformResult {

    private boolean isCommandSuccessful;
    private String commandStdOutput;
    private String commandStdError;


}
