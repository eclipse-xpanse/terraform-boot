/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.Data;

/**
 * Data model for the terraform destroy requests.
 */
@Data
public class TerraformDestroyRequest {

    @NotNull
    @Schema(description = "Key-value pairs of variables that must be used to execute the "
            + "Terraform request.")
    Map<String, String> variables;
}
