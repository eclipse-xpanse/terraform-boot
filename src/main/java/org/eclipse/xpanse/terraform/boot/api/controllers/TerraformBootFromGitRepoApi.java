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
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformGitRepoService;
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
 * REST controller for running terraform modules from a GIT repo.
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terraform-boot/git")
public class TerraformBootFromGitRepoApi {

    private final TerraformGitRepoService terraformGitRepoService;

    public TerraformBootFromGitRepoApi(TerraformGitRepoService terraformGitRepoService) {
        this.terraformGitRepoService = terraformGitRepoService;
    }

    /**
     * Method to validate resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/validate", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateScriptsFromGitRepo(
            @Valid @RequestBody TerraformDeployFromGitRepoRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformGitRepoService.validateWithScripts(request);
    }

    /**
     * Method to get Terraform plan as a JSON string from the GIT repo provided.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description =
            "Get Terraform Plan as JSON string from the list of script files provided")
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan planFromGitRepo(
            @Valid @RequestBody TerraformPlanFromGitRepoRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformGitRepoService.getTerraformPlanFromGitRepo(request, uuid);
    }

    /**
     * Method to deploy resources using scripts from the GIT Repo provided.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployFromGitRepo(
            @Valid @RequestBody TerraformDeployFromGitRepoRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformGitRepoService.deployFromGitRepo(request, uuid);
    }

    /**
     * MMethod to deploy resources using scripts from the GIT Repo provided.
     *
     * @return Returns the status of to Destroy.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Destroy resources via Terraform")
    @PostMapping(value = "/destroy", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyFromGitRepo(
            @Valid @RequestBody TerraformDestroyFromGitRepoRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        return terraformGitRepoService.destroyFromGitRepo(request, uuid);
    }

    /**
     * Method to async deploy resources from the provided GIT Repo.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/deploy/async", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployFromGitRepo(
            @Valid @RequestBody TerraformAsyncDeployFromGitRepoRequest asyncDeployRequest,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        terraformGitRepoService.asyncDeployFromGitRepo(asyncDeployRequest, uuid);
    }

    /**
     * Method to async destroy resources by scripts.
     */
    @Tag(name = "TerraformFromGitRepo", description =
            "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Async destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/async",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyFromGitRepo(
            @Valid @RequestBody TerraformAsyncDestroyFromGitRepoRequest asyncDestroyRequest,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        if (Objects.isNull(uuid)) {
            uuid = UUID.randomUUID();
        }
        MDC.put("TASK_ID", uuid.toString());
        terraformGitRepoService.asyncDestroyFromGitRepo(asyncDestroyRequest, uuid);
    }
}
