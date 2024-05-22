/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;

/**
 * Data model for the generating terraform plan.
 */
@Data
public class TerraformPlanFromDirectoryRequest {

    @Schema(description = "Id of the request.")
    UUID requestId;

    @NotNull
    @Schema(description = "Key-value pairs of variables that must be used to execute the "
            + "Terraform request.",
            additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    Map<String, Object> variables;

    @Schema(description = "Key-value pairs of variables that must be injected as environment "
            + "variables to terraform process.",
            additionalProperties = Schema.AdditionalPropertiesValue.TRUE)
    Map<String, String> envVariables = new HashMap<>();
}
