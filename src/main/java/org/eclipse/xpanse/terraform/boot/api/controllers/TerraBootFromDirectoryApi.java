/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api.controllers;

import static org.eclipse.xpanse.terraform.boot.logging.CustomRequestIdGenerator.REQUEST_ID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformAsyncDeployFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformAsyncDestroyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformAsyncModifyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformDeployFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformDestroyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.request.directory.TerraformModifyFromDirectoryRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformDirectoryService;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformScriptsHelper;
import org.eclipse.xpanse.terraform.boot.terraform.tool.TerraformVersionsHelper;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for running terraform modules directly on the provided directory. */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terra-boot/directory")
public class TerraBootFromDirectoryApi {

    @Resource private TerraformDirectoryService directoryService;
    @Resource private TerraformScriptsHelper scriptsHelper;

    /**
     * Method to validate Terraform modules.
     *
     * @return Returns the validation status of the Terraform module in a workspace.
     */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "Validate the Terraform modules in the given directory.")
    @GetMapping(
            value = "/validate/{module_directory}/{terraform_version}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformValidationResult validateFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Parameter(
                            name = "terraform_version",
                            description = "version of Terraform to execute the module files.")
                    @NotBlank
                    @Pattern(regexp = TerraformVersionsHelper.TERRAFORM_REQUIRED_VERSION_REGEX)
                    @PathVariable("terraform_version")
                    String terraformVersion) {
        UUID uuid = UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        return directoryService.tfValidateFromDirectory(moduleDirectory, terraformVersion);
    }

    /**
     * Method to deploy resources requested in a workspace.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "Deploy resources via Terraform from the given directory.")
    @PostMapping(value = "/deploy/{module_directory}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult deployFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformDeployFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        return directoryService.deployFromDirectory(request, moduleDirectory, scriptFiles);
    }

    /**
     * Method to modify resources requested in a workspace.
     *
     * @return Returns the status of the deployment.
     */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "Modify resources via Terraform from the given directory.")
    @PostMapping(value = "/modify/{module_directory}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult modifyFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformModifyFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        return directoryService.modifyFromDirectory(request, moduleDirectory, scriptFiles);
    }

    /**
     * Method to destroy resources requested in a workspace.
     *
     * @return Returns the status of the resources destroy.
     */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "Destroy the resources from the given directory.")
    @DeleteMapping(
            value = "/destroy/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformResult destroyFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformDestroyFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        return directoryService.destroyFromDirectory(request, moduleDirectory, scriptFiles);
    }

    /**
     * Method to get Terraform plan as a JSON string from a directory.
     *
     * @return Returns the terraform plan as a JSON string.
     */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "Get Terraform Plan as JSON string from the given directory.")
    @PostMapping(value = "/plan/{module_directory}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraformPlan plan(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformPlanFromDirectoryRequest request,
            @RequestHeader(name = "X-Custom-RequestId", required = false) UUID uuid) {
        uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : (Objects.nonNull(uuid) ? uuid : UUID.randomUUID());
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        return directoryService.getTerraformPlanFromDirectory(request, moduleDirectory);
    }

    /** Method to async deploy resources from the given directory. */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "async deploy resources via Terraform from the given directory.")
    @PostMapping(
            value = "/deploy/async/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDeployFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformAsyncDeployFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        directoryService.asyncDeployWithScripts(request, moduleDirectory, scriptFiles);
    }

    /** Method to async modify resources from the given directory. */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "async modify resources via Terraform from the given directory.")
    @PostMapping(
            value = "/modify/async/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncModifyFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformAsyncModifyFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        directoryService.asyncModifyWithScripts(request, moduleDirectory, scriptFiles);
    }

    /** Method to async destroy resources from the given directory. */
    @Tag(
            name = "TerraformFromDirectory",
            description = "APIs for running Terraform commands inside a provided directory.")
    @Operation(description = "async destroy resources via Terraform from the given directory.")
    @DeleteMapping(
            value = "/destroy/async/{module_directory}",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void asyncDestroyFromDirectory(
            @Parameter(
                            name = "module_directory",
                            description = "directory name where the Terraform module files exist.")
                    @PathVariable("module_directory")
                    String moduleDirectory,
            @Valid @RequestBody TerraformAsyncDestroyFromDirectoryRequest request) {
        UUID uuid =
                Objects.nonNull(request.getRequestId())
                        ? request.getRequestId()
                        : UUID.randomUUID();
        MDC.put(REQUEST_ID, uuid.toString());
        request.setRequestId(uuid);
        List<File> scriptFiles = scriptsHelper.getDeploymentFilesFromTaskWorkspace(moduleDirectory);
        directoryService.asyncDestroyWithScripts(request, moduleDirectory, scriptFiles);
    }
}
