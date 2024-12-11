/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/** Defines the Terraform validation result. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TerraformValidationResult {

    @NotNull
    @Schema(description = "Defines if the Terraform scripts is valid.")
    private boolean valid;

    @Schema(description = "The version of the Terraform binary used to execute scripts.")
    private String terraformVersionUsed;

    @Schema(description = "List of validation errors.")
    private List<TerraformValidateDiagnostics> diagnostics;
}
