/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.plan;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformScriptGitRepoDetails;

/** Data model for the generating terraform plan using Terraform scripts from a GIT repo. */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformPlanFromGitRepoRequest extends TerraformPlanFromDirectoryRequest {

    @Schema(description = "GIT Repo details from where the scripts can be fetched.")
    private TerraformScriptGitRepoDetails gitRepoDetails;
}
