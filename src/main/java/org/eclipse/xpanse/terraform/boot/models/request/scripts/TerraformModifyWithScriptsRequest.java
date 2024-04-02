/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request.scripts;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformModifyFromDirectoryRequest;

/**
 * Terraform uses the request object modify by the script.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformModifyWithScriptsRequest extends TerraformModifyFromDirectoryRequest {
    @NotNull
    @Schema(description = "List of script files for modify requests deployed via scripts")
    private List<String> scripts;

    @NotNull
    @Schema(description = "The .tfState file content after deployment")
    private String tfState;
}
