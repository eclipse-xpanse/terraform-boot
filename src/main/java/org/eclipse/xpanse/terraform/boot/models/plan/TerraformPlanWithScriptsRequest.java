/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** Data model for the generating terraform plan. */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformPlanWithScriptsRequest extends TerraformPlanFromDirectoryRequest {

    @NotNull
    @NotEmpty
    @Schema(
            description =
                    "Map stores file name and content of all script files for generating terraform"
                            + " plan.")
    private Map<String, String> scriptFiles;
}
