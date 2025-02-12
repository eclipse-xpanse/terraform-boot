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
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncModifyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformModifyFromGitRepoRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for running terraform modules from a GIT repo. */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terra-boot/git")
public class TerraBootFromGitRepoApi {

    private final TerraformGitRepoService terraformGitRepoService;

    public TerraBootFromGitRepoApi(TerraformGitRepoService terraformGitRepoService) {
        this.terraformGitRepoService = terraformGitRepoService;
    }

    /**
     * Method to validate resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/validate", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateScriptsFromGitRepo(
            @Valid @RequestBody TerraformDeployFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformGitRepoService.validateWithScripts(request);
    }

    /**
     * Method to get Terraform plan as a JSON string from the GIT repo provided.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(
            description =
                    "Get Terraform Plan as JSON string from the list of script files provided")
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan planFromGitRepo(
            @Valid @RequestBody TerraformPlanFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformGitRepoService.getTerraformPlanFromGitRepo(request, uuid);
    }

    /**
     * Method to deploy resources using scripts from the GIT Repo provided.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployFromGitRepo(
            @Valid @RequestBody TerraformDeployFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformGitRepoService.deployFromGitRepo(request, uuid);
    }

    /**
     * Method to modify resources using scripts from the GIT Repo provided.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Modify resources via Terraform")
    @PostMapping(value = "/modify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult modifyFromGitRepo(
            @Valid @RequestBody TerraformModifyFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformGitRepoService.modifyFromGitRepo(request, uuid);
    }

    /**
     * MMethod to deploy resources using scripts from the GIT Repo provided.
     *
     * @return Returns the status of to Destroy.
     */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Destroy resources via Terraform")
    @PostMapping(value = "/destroy", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyFromGitRepo(
            @Valid @RequestBody TerraformDestroyFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return terraformGitRepoService.destroyFromGitRepo(request, uuid);
    }

    /** Method to async deploy resources from the provided GIT Repo. */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/deploy/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployFromGitRepo(
            @Valid @RequestBody TerraformAsyncDeployFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformGitRepoService.asyncDeployFromGitRepo(request, uuid);
    }

    /** Method to async modify resources from the provided GIT Repo. */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/modify/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncModifyFromGitRepo(
            @Valid @RequestBody TerraformAsyncModifyFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformGitRepoService.asyncModifyFromGitRepo(request, uuid);
    }

    /** Method to async destroy resources by scripts. */
    @Tag(
            name = "TerraformFromGitRepo",
            description =
                    "APIs for running Terraform commands using Terraform scripts from a GIT Repo.")
    @Operation(description = "Async destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/async", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyFromGitRepo(
            @Valid @RequestBody TerraformAsyncDestroyFromGitRepoRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        terraformGitRepoService.asyncDestroyFromGitRepo(request, uuid);
    }
}
