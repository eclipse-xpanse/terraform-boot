/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api.controllers;

import static org.eclipse.xpanse.terraform.boot.logging.CustomRequestIdGenerator.REQUEST_ID;

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
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformAsyncModifyFromScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDeployWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDestroyWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformModifyWithScriptsRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** API methods implemented by terra-boot. */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terra-boot/scripts/")
public class TerraBootFromScriptsApi {

    private final TerraformScriptsService terraformScriptsService;

    public TerraBootFromScriptsApi(TerraformScriptsService terraformScriptsService) {
        this.terraformScriptsService = terraformScriptsService;
    }

    /**
     * Method to validate resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformScriptsService.validateWithScripts(request);
    }

    /**
     * Method to deploy resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformScriptsService.deployWithScripts(request, uuid);
    }

    /**
     * Method to modify resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Modify resources via Terraform")
    @PostMapping(value = "/modify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult modifyWithScripts(
            @Valid @RequestBody TerraformModifyWithScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformScriptsService.modifyWithScripts(request, uuid);
    }

    /**
     * Method to destroy resources by scripts.
     *
     * @return Returns the status of to Destroy.
     */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Destroy resources via Terraform")
    @PostMapping(value = "/destroy", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyWithScripts(
            @Valid @RequestBody TerraformDestroyWithScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformScriptsService.destroyWithScripts(request, uuid);
    }

    /** Method to async deploy resources by scripts. */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/deploy/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployWithScripts(
            @Valid @RequestBody TerraformAsyncDeployFromScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformScriptsService.asyncDeployWithScripts(request, uuid);
    }

    /** Method to async modify resources by scripts. */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "async modify resources via Terraform")
    @PostMapping(value = "/modify/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncModifyWithScripts(
            @Valid @RequestBody TerraformAsyncModifyFromScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformScriptsService.asyncModifyWithScripts(request, uuid);
    }

    /** Method to async destroy resources by scripts. */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(description = "Async destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyWithScripts(
            @Valid @RequestBody TerraformAsyncDestroyFromScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformScriptsService.asyncDestroyWithScripts(request, uuid);
    }

    /**
     * Method to get Terraform plan as a JSON string from the list of script files provided.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(
            name = "TerraformFromScripts",
            description =
                    "APIs for running Terraform commands on the scripts sent via request body.")
    @Operation(
            description =
                    "Get Terraform Plan as JSON string from the list of script files provided")
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan planWithScripts(
            @Valid @RequestBody TerraformPlanWithScriptsRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformScriptsService.getTerraformPlanFromScripts(request, uuid);
    }
}
