/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.TerraformBootSystemStatus;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDeployFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDeployWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDestroyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDestroyWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.async.TerraformAsyncDeployFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.async.TerraformAsyncDestroyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformDirectoryService;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformScriptsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * API methods implemented by terraform-boot.
 */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terraform-boot")
public class TerraformApiController {

    private final TerraformDirectoryService terraformDirectoryService;
    private final TerraformScriptsService terraformScriptsService;

    @Autowired
    public TerraformApiController(
            @Qualifier("terraformDirectoryService")
            TerraformDirectoryService terraformDirectoryService,
            TerraformScriptsService terraformScriptsService) {
        this.terraformDirectoryService = terraformDirectoryService;
        this.terraformScriptsService = terraformScriptsService;
    }

    /**
     * Method to find out the current state of the system.
     *
     * @return Returns the current state of the system.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Check health of Terraform API service")
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformBootSystemStatus healthCheck() {
        return terraformDirectoryService.tfHealthCheck();
    }

    /**
     * Method to validate Terraform modules.
     *
     * @return Returns the validation status of the Terraform module in a workspace.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Validate the Terraform modules")
    @GetMapping(value = "/validate/{module_directory}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validate(
            @Parameter(name = "module_directory",
                    description = "directory name where the Terraform module files exist.")
            @PathVariable("module_directory") String moduleDirectory) {
        return terraformDirectoryService.tfValidateFromDirectory(moduleDirectory);
    }

    /**
     * Method to deploy resources requested in a workspace.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy/{module_directory}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deploy(
            @Parameter(name = "module_directory",
                    description = "directory name where the Terraform module files exist.")
            @PathVariable("module_directory") String moduleDirectory,
            @Valid @RequestBody
            TerraformDeployFromDirectoryRequest request) {
        return terraformDirectoryService.deployFromDirectory(request, moduleDirectory);
    }

    /**
     * Method to destroy resources requested in a workspace.
     *
     * @return Returns the status of the resources destroy.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroy(
            @Parameter(name = "module_directory",
                    description = "directory name where the Terraform module files exist.")
            @PathVariable("module_directory") String moduleDirectory,
            @Valid @RequestBody
            TerraformDestroyFromDirectoryRequest request) {
        return terraformDirectoryService.destroyFromDirectory(request, moduleDirectory);
    }

    /**
     * Method to validate resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/validate/scripts", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request) {
        return terraformScriptsService.validateWithScripts(request);
    }

    /**
     * Method to deploy resources by scripts.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Deploy resources via Terraform")
    @PostMapping(value = "/deploy/scripts", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployWithScripts(
            @Valid @RequestBody TerraformDeployWithScriptsRequest request) {
        return terraformScriptsService.deployWithScripts(request);
    }

    /**
     * Method to destroy resources by scripts.
     *
     * @return Returns the status of to Destroy.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Destroy resources via Terraform")
    @PostMapping(value = "/destroy/scripts", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyWithScripts(
            @Valid @RequestBody TerraformDestroyWithScriptsRequest request) {
        return terraformScriptsService.destroyWithScripts(request);
    }

    /**
     * Method to async deploy resources by scripts.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "async deploy resources via Terraform")
    @PostMapping(value = "/deploy/scripts/async", produces =
            MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployWithScripts(
            @Valid @RequestBody TerraformAsyncDeployFromDirectoryRequest asyncDeployRequest) {
        terraformScriptsService.asyncDeployWithScripts(asyncDeployRequest);
    }

    /**
     * Method to async destroy resources by scripts.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Async destroy the Terraform modules")
    @DeleteMapping(value = "/destroy/scripts/async",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyWithScripts(
            @Valid @RequestBody TerraformAsyncDestroyFromDirectoryRequest asyncDestroyRequest) {
        terraformScriptsService.asyncDestroyWithScripts(asyncDestroyRequest);
    }

    /**
     * Method to get Terraform plan as a JSON string from a directory.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Get Terraform Plan as JSON string from a directory")
    @PostMapping(value = "/plan/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan plan(
            @Parameter(name = "module_directory",
                    description = "directory name where the Terraform module files exist.")
            @PathVariable("module_directory") String moduleDirectory,
            @Valid @RequestBody TerraformPlanFromDirectoryRequest request) {
        return terraformDirectoryService.getTerraformPlanFromDirectory(request,
                moduleDirectory);
    }

    /**
     * Method to get Terraform plan as a JSON string from the list of script files provided.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description =
            "Get Terraform Plan as JSON string from the list of script files provided")
    @PostMapping(value = "/plan", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan planWithScripts(
            @Valid @RequestBody TerraformPlanWithScriptsRequest request) {
        return terraformScriptsService.getTerraformPlanFromScripts(request);
    }
}
