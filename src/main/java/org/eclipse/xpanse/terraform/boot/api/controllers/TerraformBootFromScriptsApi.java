/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformAsyncDeployFromScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformAsyncDestroyFromScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDeployWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDestroyWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformScriptsService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API methods implemented by terraform-boot.
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terraform-boot/scripts/")
public class TerraformBootFromScriptsApi {

    private final TerraformScriptsService terraformScriptsService;

    public TerraformBootFromScriptsApi(TerraformScriptsService terraformScriptsService) {
        this.terraformScriptsService = terraformScriptsService;
    }

    /**
     * Method to validate resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/validate", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformScriptsService.validateWithScripts(request);
    }

    /**
     * Method to deploy resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformScriptsService.deployWithScripts(request, uuid);
    }

    /**
     * Method to destroy resources by scripts.
     *
     * @return Returns the status of to Destroy.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Destroy resources via Terraform")
    @PostMapping(value = "/destroy", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyWithScripts(
            @Valid @RequestBody TerraformDestroyWithScriptsRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformScriptsService.destroyWithScripts(request, uuid);
    }

    /**
     * Method to async deploy resources by scripts.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/deploy/async", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployWithScripts(
            @Valid @RequestBody TerraformAsyncDeployFromScriptsRequest asyncDeployRequest,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        terraformScriptsService.asyncDeployWithScripts(asyncDeployRequest, uuid);
    }

    /**
     * Method to async destroy resources by scripts.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Async destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/async",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyWithScripts(
            @Valid @RequestBody TerraformAsyncDestroyFromScriptsRequest asyncDestroyRequest,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        terraformScriptsService.asyncDestroyWithScripts(asyncDestroyRequest, uuid);
    }

    /**
     * Method to get Terraform plan as a JSON string from the list of script files provided.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(name = "TerraformFromScripts", description =
            "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description =
            "Get Terraform Plan as JSON string from the list of script files provided")
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan planWithScripts(
            @Valid @RequestBody TerraformPlanWithScriptsRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformScriptsService.getTerraformPlanFromScripts(request, uuid);
    }
}
