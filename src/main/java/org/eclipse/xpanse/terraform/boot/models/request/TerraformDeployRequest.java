/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

/**
 * Data model for the terraform deploy requests.
 */
@Data
public class TerraformDeployRequest {

    @NotNull
    @Schema(description = "Flag to control if the deployment must only generate the terraform "
                + "or it must also apply the changes.")
    Boolean isPlanOnly;

    @NotNull
    @Schema(description = "Key-value pairs of variables that must be used to execute the "
            + "Terraform request.")
    Map<String, String> variables;
}
