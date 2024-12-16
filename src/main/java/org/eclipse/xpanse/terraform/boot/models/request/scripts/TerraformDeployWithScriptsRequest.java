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
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformDeployFromDirectoryRequest;

/** Terraform uses the request object deployed by the script. */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformDeployWithScriptsRequest extends TerraformDeployFromDirectoryRequest {

    @NotNull
    @Schema(description = "List of Terraform script files to be considered for deploying changes.")
    private List<String> scripts;
}
