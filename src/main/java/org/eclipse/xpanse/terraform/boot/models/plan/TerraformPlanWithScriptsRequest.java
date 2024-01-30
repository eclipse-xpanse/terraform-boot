/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data model for the generating terraform plan.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformPlanWithScriptsRequest extends TerraformPlanFromDirectoryRequest {

    @NotNull
    @Schema(description =
            "List of terraform script files to be considered for generating terraform plan")
    private List<String> scripts;

}
