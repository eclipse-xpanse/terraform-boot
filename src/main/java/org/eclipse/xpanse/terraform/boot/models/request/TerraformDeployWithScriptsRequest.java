/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 *
 */

package org.eclipse.xpanse.terraform.boot.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

/**
 * Terraform uses the request object deployed by the script.
 */
@Data
public class TerraformDeployWithScriptsRequest extends TerraformDeployFromDirectoryRequest {

    @NotNull
    @Schema(description = "List of script files for deployment requests deployed via scripts")
    private List<String> scripts;

}
