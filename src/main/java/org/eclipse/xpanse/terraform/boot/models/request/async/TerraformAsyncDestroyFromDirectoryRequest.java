/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.models.request.async;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDestroyWithScriptsRequest;

/**
 * Data model for the terraform async destroy requests.
 */
@Data
public class TerraformAsyncDestroyFromDirectoryRequest extends TerraformDestroyWithScriptsRequest {

    @NotNull
    @Schema(description = "Configuration information of webhook.")
    private WebhookConfig webhookConfig;

}
