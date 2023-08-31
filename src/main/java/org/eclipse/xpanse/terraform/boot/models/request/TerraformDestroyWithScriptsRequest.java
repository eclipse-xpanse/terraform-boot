/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Terraform uses the request object destroy by the script.
 */
@Data
public class TerraformDestroyWithScriptsRequest extends TerraformDestroyFromDirectoryRequest {

    @NotNull
    @Schema(description = "List of script files for destroy requests deployed via scripts")
    private List<String> scripts;

    @NotNull
    @Schema(description = "The .tfState file content after deployment")
    private String tfState;
}
