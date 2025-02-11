/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request.directory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import org.eclipse.xpanse.terraform.boot.terraform.tool.TerraformVersionsHelper;

/** Data model for the terraform destroy requests. */
@Data
public class TerraformDestroyFromDirectoryRequest {

    @Schema(description = "Id of the request")
    private UUID requestId;

    @NotNull
    @NotBlank
    @Pattern(regexp = TerraformVersionsHelper.TERRAFORM_REQUIRED_VERSION_REGEX)
    @Schema(description = "The required version of terraform which will execute the scripts.")
    private String terraformVersion;

    @NotNull
    @Schema(
            description =
                    "Key-value pairs of regular variables that must be used to execute the "
                            + "Terraform request.")
    private Map<String, Object> variables;

    @Schema(
            description =
                    "Key-value pairs of variables that must be injected as environment "
                            + "variables to terraform process.")
    private Map<String, String> envVariables = new HashMap<>();
}
