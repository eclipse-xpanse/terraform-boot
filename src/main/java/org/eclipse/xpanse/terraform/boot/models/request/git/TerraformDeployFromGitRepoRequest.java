/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request.git;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformDeployFromDirectoryRequest;

/**
 * Data model for terraform deploy requests using scripts from a GIT Repo.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TerraformDeployFromGitRepoRequest extends TerraformDeployFromDirectoryRequest {

    @NotNull
    @Schema(description = "GIT Repo details from where the scripts can be fetched.")
    TerraformScriptGitRepoDetails gitRepoDetails;
}
