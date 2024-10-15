/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * Data model to represent terraform plan output.
 */
@Data
@Builder
public class TerraformPlan {

    @NotNull
    @Schema(description = "Terraform plan as a JSON string")
    String plan;

    @Schema(description = "The version of the Terraform binary used to execute scripts.")
    private String terraformVersionUsed;
}
