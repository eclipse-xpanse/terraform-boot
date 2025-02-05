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
import org.eclipse.xpanse.terraform.boot.async.TaskConfiguration;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Terraform service classes are deployed form Scripts. */
@Slf4j
@Service
public class TerraformScriptsService {

    @Resource private RestTemplate restTemplate;
    @Resource private TerraformScriptsHelper scriptsHelper;
    @Resource private TerraformDirectoryService directoryService;
    @Resource private TerraformResultPersistenceManage terraformResultPersistenceManage;

    /** /** Method of deployment a service using a script. */
    public TerraformValidationResult validateWithScripts(
            TerraformDeployWithScriptsRequest request) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(UUID.randomUUID().toString());
        scriptsHelper.prepareDeploymentFilesWithScripts(
                taskWorkspace, request.getScriptFiles(), null);
        return directoryService.tfValidateFromDirectory(
                taskWorkspace, request.getTerraformVersion());
    }

    /** Method of deployment a service using a script. */
    public TerraformResult deployWithScripts(TerraformDeployWithScriptsRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> files =
                scriptsHelper.prepareDeploymentFilesWithScripts(
                        taskWorkspace, request.getScriptFiles(), null);
        return directoryService.deployFromDirectory(request, taskWorkspace, files);
    }

    /** Method of modify a service using a script. */
    public TerraformResult modifyWithScripts(TerraformModifyWithScriptsRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> files =
                scriptsHelper.prepareDeploymentFilesWithScripts(
                        taskWorkspace, request.getScriptFiles(), request.getTfState());
        return directoryService.modifyFromDirectory(request, taskWorkspace, files);
    }

    /** Method of destroy a service using a script. */
    public TerraformResult destroyWithScripts(
            TerraformDestroyWithScriptsRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        List<File> files =
                scriptsHelper.prepareDeploymentFilesWithScripts(
                        taskWorkspace, request.getScriptFiles(), request.getTfState());
        return directoryService.destroyFromDirectory(request, taskWorkspace, files);
    }

    /** Method to get terraform plan. */
    public TerraformPlan getTerraformPlanFromScripts(
            TerraformPlanWithScriptsRequest request, UUID uuid) {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(uuid.toString());
        scriptsHelper.prepareDeploymentFilesWithScripts(
                taskWorkspace, request.getScriptFiles(), null);
        return directoryService.getTerraformPlanFromDirectory(request, uuid.toString());
    }

    /** Async deploy a source by terraform. */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDeployWithScripts(
            TerraformAsyncDeployFromScriptsRequest asyncDeployRequest, UUID uuid) {
        TerraformResult result;
        try {
            result = deployWithScripts(asyncDeployRequest, uuid);
        } catch (RuntimeException e) {
            result =
                    TerraformResult.builder()
                            .commandStdOutput(null)
                            .commandStdError(e.getMessage())
                            .isCommandSuccessful(false)
                            .terraformState(null)
                            .generatedFileContentMap(new HashMap<>())
                            .build();
        }
        result.setRequestId(asyncDeployRequest.getRequestId());
        String url = asyncDeployRequest.getWebhookConfig().getUrl();
        log.info("Deployment service complete, callback POST url:{}, requestBody:{}", url, result);
        sendTerraformResult(url, result);
    }

    /** Async modify a source by terraform. */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncModifyWithScripts(
            TerraformAsyncModifyFromScriptsRequest asyncModifyRequest, UUID uuid) {
        TerraformResult result;
        try {
            result = modifyWithScripts(asyncModifyRequest, uuid);
        } catch (RuntimeException e) {
            result =
                    TerraformResult.builder()
                            .commandStdOutput(null)
                            .commandStdError(e.getMessage())
                            .isCommandSuccessful(false)
                            .terraformState(null)
                            .generatedFileContentMap(new HashMap<>())
                            .build();
        }
        result.setRequestId(asyncModifyRequest.getRequestId());
        String url = asyncModifyRequest.getWebhookConfig().getUrl();
        log.info("Modify service complete, callback POST url:{}, requestBody:{}", url, result);
        sendTerraformResult(url, result);
    }

    /** Async destroy resource of the service. */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDestroyWithScripts(
            TerraformAsyncDestroyFromScriptsRequest request, UUID uuid) {
        TerraformResult result;
        try {
            result = destroyWithScripts(request, uuid);
        } catch (RuntimeException e) {
            result =
                    TerraformResult.builder()
                            .commandStdOutput(null)
                            .commandStdError(e.getMessage())
                            .isCommandSuccessful(false)
                            .terraformState(null)
                            .generatedFileContentMap(new HashMap<>())
                            .build();
        }
        result.setRequestId(request.getRequestId());
        String url = request.getWebhookConfig().getUrl();
        log.info("Destroy service complete, callback POST url:{}, requestBody:{}", url, result);
        sendTerraformResult(url, result);
    }

    private void sendTerraformResult(String url, TerraformResult result) {
        try {
            restTemplate.postForLocation(url, result);
        } catch (RestClientException e) {
            log.error("error while sending terraform result", e);
            terraformResultPersistenceManage.persistTerraformResult(result);
        }
    }
}
