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
import org.eclipse.xpanse.terraform.boot.models.SystemStatus;
import org.eclipse.xpanse.terraform.boot.models.enums.HealthStatus;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDeployRequest;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDestroyRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.TerraformExecutor;
import org.springframework.beans.factory.annotation.Autowired;
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

    private final TerraformExecutor terraformExecutor;

    @Autowired
    public TerraformApiController(TerraformExecutor terraformExecutor) {
        this.terraformExecutor = terraformExecutor;
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
    public SystemStatus healthCheck() {
        SystemStatus systemStatus = new SystemStatus();
        systemStatus.setHealthStatus(HealthStatus.OK);
        return systemStatus;
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
        return this.terraformExecutor.tfValidate(moduleDirectory);
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
            @Valid @RequestBody TerraformDeployRequest terraformDeployRequest) {
        return this.terraformExecutor.deploy(terraformDeployRequest, moduleDirectory);
    }

    /**
     * Method to destroy resources requested in a workspace.
     *
     * @return Returns the status of the resources destroy.
     */
    @Tag(name = "Terraform", description = "APIs for running Terraform commands")
    @Operation(description = "Validate the Terraform modules")
    @DeleteMapping(value = "/destroy/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroy(
            @Parameter(name = "module_directory",
                    description = "directory name where the Terraform module files exist.")
            @PathVariable("module_directory") String moduleDirectory,
            @Valid @RequestBody TerraformDestroyRequest terraformDestroyRequest) {
        return this.terraformExecutor.destroy(terraformDestroyRequest.getVariables(),
                moduleDirectory);
    }
}
