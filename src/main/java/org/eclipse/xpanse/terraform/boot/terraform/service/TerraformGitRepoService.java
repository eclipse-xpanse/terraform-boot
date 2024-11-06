/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.terraform.boot.async.TaskConfiguration;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncModifyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformModifyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformScriptGitRepoDetails;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Bean to manage all Terraform execution using scripts from a GIT Repo.
 */
@Slf4j
@Component
public class TerraformGitRepoService {

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private TerraformScriptsHelper scriptsHelper;
    @Resource
    private TerraformDirectoryService directoryService;

    /**
     * Method of deployment a service using a script.
     */
    public TerraformValidationResult validateWithScripts(
            TerraformDeployFromGitRepoRequest request) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(UUID.randomUUID().toString());
        scriptsHelper.prepareDeploymentFilesWithGitRepo(
                taskWorkspace, request.getGitRepoDetails(), null);
        String scriptsPath = getScriptsLocationInTaskWorkspace(
                request.getGitRepoDetails(), taskWorkspace);
        return directoryService.tfValidateFromDirectory(scriptsPath, request.getTerraformVersion());
    }

    /**
     * Method to get terraform plan.
     */
    public TerraformPlan getTerraformPlanFromGitRepo(
            TerraformPlanFromGitRepoRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        scriptsHelper.prepareDeploymentFilesWithGitRepo(
                taskWorkspace, request.getGitRepoDetails(), null);
        String scriptsPath = getScriptsLocationInTaskWorkspace(
                request.getGitRepoDetails(), taskWorkspace);
        return directoryService.getTerraformPlanFromDirectory(request, scriptsPath);
    }

    /**
     * Method of deployment a service using a script.
     */
    public TerraformResult deployFromGitRepo(TerraformDeployFromGitRepoRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> scriptFiles = scriptsHelper.prepareDeploymentFilesWithGitRepo(
                taskWorkspace, request.getGitRepoDetails(), null);
        String scriptsPath = getScriptsLocationInTaskWorkspace(
                request.getGitRepoDetails(), taskWorkspace);
        TerraformResult result =
                directoryService.deployFromDirectory(request, scriptsPath, scriptFiles);
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        return result;
    }

    /**
     * Method of modify a service using a script.
     */
    public TerraformResult modifyFromGitRepo(TerraformModifyFromGitRepoRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> scriptFiles = scriptsHelper.prepareDeploymentFilesWithGitRepo(
                taskWorkspace, request.getGitRepoDetails(), request.getTfState());
        String scriptsPath = getScriptsLocationInTaskWorkspace(
                request.getGitRepoDetails(), taskWorkspace);
        TerraformResult result =
                directoryService.modifyFromDirectory(request, scriptsPath, scriptFiles);
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        return result;
    }

    /**
     * Method of destroy a service using a script.
     */
    public TerraformResult destroyFromGitRepo(
            TerraformDestroyFromGitRepoRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> scriptFiles = scriptsHelper.prepareDeploymentFilesWithGitRepo(
                taskWorkspace, request.getGitRepoDetails(), request.getTfState());
        String scriptsPath = getScriptsLocationInTaskWorkspace(
                request.getGitRepoDetails(), taskWorkspace);
        TerraformResult result =
                directoryService.destroyFromDirectory(request, scriptsPath, scriptFiles);
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        return result;
    }

    /**
     * Async deploy a source by terraform.
     */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDeployFromGitRepo(TerraformAsyncDeployFromGitRepoRequest asyncDeployRequest,
                                       UUID uuid) {
        TerraformResult result;
        try {
            result = deployFromGitRepo(asyncDeployRequest, uuid);
        } catch (RuntimeException e) {
            result = TerraformResult.builder()
                    .commandStdOutput(null)
                    .commandStdError(e.getMessage())
                    .isCommandSuccessful(false)
                    .terraformState(null)
                    .importantFileContentMap(new HashMap<>())
                    .build();
        }
        result.setRequestId(asyncDeployRequest.getRequestId());
        String url = asyncDeployRequest.getWebhookConfig().getUrl();
        log.info("Deployment service complete, callback POST url:{}, requestBody:{}", url, result);
        restTemplate.postForLocation(url, result);
    }

    /**
     * Async modify a source by terraform.
     */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncModifyFromGitRepo(TerraformAsyncModifyFromGitRepoRequest asyncModifyRequest,
                                       UUID uuid) {
        TerraformResult result;
        try {
            result = modifyFromGitRepo(asyncModifyRequest, uuid);
        } catch (RuntimeException e) {
            result = TerraformResult.builder()
                    .commandStdOutput(null)
                    .commandStdError(e.getMessage())
                    .isCommandSuccessful(false)
                    .terraformState(null)
                    .importantFileContentMap(new HashMap<>())
                    .build();
        }
        result.setRequestId(asyncModifyRequest.getRequestId());
        String url = asyncModifyRequest.getWebhookConfig().getUrl();
        log.info("Modify service complete, callback POST url:{}, requestBody:{}", url, result);
        restTemplate.postForLocation(url, result);
    }


    /**
     * Async destroy resource of the service.
     */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDestroyFromGitRepo(TerraformAsyncDestroyFromGitRepoRequest request,
                                        UUID uuid) {
        TerraformResult result;
        try {
            result = destroyFromGitRepo(request, uuid);
        } catch (RuntimeException e) {
            result = TerraformResult.builder()
                    .commandStdOutput(null)
                    .commandStdError(e.getMessage())
                    .isCommandSuccessful(false)
                    .terraformState(null)
                    .importantFileContentMap(new HashMap<>())
                    .build();
        }
        result.setRequestId(request.getRequestId());
        String url = request.getWebhookConfig().getUrl();
        log.info("Destroy service complete, callback POST url:{}, requestBody:{}", url, result);
        restTemplate.postForLocation(url, result);
    }


    private String getScriptsLocationInTaskWorkspace(
            TerraformScriptGitRepoDetails terraformScriptGitRepoDetails, String taskWorkSpace) {
        if (StringUtils.isNotBlank(terraformScriptGitRepoDetails.getScriptPath())) {
            return taskWorkSpace + File.separator + terraformScriptGitRepoDetails.getScriptPath();
        }
        return taskWorkSpace;
    }

}
