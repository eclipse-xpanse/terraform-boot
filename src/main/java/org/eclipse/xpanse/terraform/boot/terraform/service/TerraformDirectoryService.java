/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.async.TaskConfiguration;
import org.eclipse.xpanse.terraform.boot.models.TerraBootSystemStatus;
import org.eclipse.xpanse.terraform.boot.models.enums.HealthStatus;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
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
import org.eclipse.xpanse.terraform.boot.terraform.TerraformExecutor;
import org.eclipse.xpanse.terraform.boot.terraform.tool.TerraformInstaller;
import org.eclipse.xpanse.terraform.boot.terraform.tool.TerraformVersionsHelper;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmdResult;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Terraform service classes are deployed form Directory. */
@Slf4j
@Service
public class TerraformDirectoryService {
    private static final String HELLO_WORLD_TF_NAME = "hello_world.tf";
    private static final String HELLO_WORLD_TEMPLATE =
            """
            output "hello_world" {
                value = "Hello, World!"
            }
            """;
    @Resource private TerraformExecutor executor;
    @Resource private TerraformInstaller installer;
    @Resource private RestTemplate restTemplate;
    @Resource private TerraformVersionsHelper versionHelper;
    @Resource private TerraformScriptsHelper scriptsHelper;
    @Resource private TerraformResultPersistenceManage terraformResultPersistenceManage;

    /**
     * Perform Terra-Boot health checks by creating a Terraform test configuration file.
     *
     * @return TerraBootSystemStatus.
     */
    public TerraBootSystemStatus tfHealthCheck() {
        String taskWorkspace = scriptsHelper.buildTaskWorkspace(UUID.randomUUID().toString());
        scriptsHelper.prepareDeploymentFilesWithScripts(
                taskWorkspace, Map.of(HELLO_WORLD_TF_NAME, HELLO_WORLD_TEMPLATE), null);
        TerraformValidationResult terraformValidationResult =
                tfValidateFromDirectory(taskWorkspace, null);
        TerraBootSystemStatus systemStatus = new TerraBootSystemStatus();
        if (terraformValidationResult.isValid()) {
            systemStatus.setHealthStatus(HealthStatus.OK);
            return systemStatus;
        }
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        systemStatus.setHealthStatus(HealthStatus.NOK);
        return systemStatus;
    }

    /**
     * Executes terraform validate command.
     *
     * @return TfValidationResult.
     */
    public TerraformValidationResult tfValidateFromDirectory(
            String taskWorkspace, String terraformVersion) {
        try {
            String executorPath =
                    installer.getExecutorPathThatMatchesRequiredVersion(terraformVersion);
            SystemCmdResult result = executor.tfValidate(executorPath, taskWorkspace);
            TerraformValidationResult validationResult =
                    new ObjectMapper()
                            .readValue(
                                    result.getCommandStdOutput(), TerraformValidationResult.class);
            validationResult.setTerraformVersionUsed(
                    versionHelper.getExactVersionOfExecutor(executorPath));
            return validationResult;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialising string to object failed.", ex);
        }
    }

    /** Deploy a source by terraform. */
    public TerraformResult deployFromDirectory(
            TerraformDeployFromDirectoryRequest request,
            String taskWorkspace,
            List<File> scriptFiles) {
        SystemCmdResult result;
        String executorPath = null;
        try {
            executorPath =
                    installer.getExecutorPathThatMatchesRequiredVersion(
                            request.getTerraformVersion());
            if (Boolean.TRUE.equals(request.getIsPlanOnly())) {
                result =
                        executor.tfPlan(
                                executorPath,
                                request.getVariables(),
                                request.getEnvVariables(),
                                taskWorkspace);
            } else {
                result =
                        executor.tfApply(
                                executorPath,
                                request.getVariables(),
                                request.getEnvVariables(),
                                taskWorkspace);
            }
        } catch (InvalidTerraformToolException | TerraformExecutorException tfEx) {
            log.error("Terraform deploy service failed. error:{}", tfEx.getMessage());
            result = new SystemCmdResult();
            result.setCommandSuccessful(false);
            result.setCommandStdError(tfEx.getMessage());
        }
        TerraformResult terraformResult =
                transSystemCmdResultToTerraformResult(result, taskWorkspace, scriptFiles);
        terraformResult.setTerraformVersionUsed(
                versionHelper.getExactVersionOfExecutor(executorPath));
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        return terraformResult;
    }

    /** Modify a source by terraform. */
    public TerraformResult modifyFromDirectory(
            TerraformModifyFromDirectoryRequest request,
            String taskWorkspace,
            List<File> scriptFiles) {
        SystemCmdResult result;
        String executorPath = null;
        try {
            executorPath =
                    installer.getExecutorPathThatMatchesRequiredVersion(
                            request.getTerraformVersion());
            if (Boolean.TRUE.equals(request.getIsPlanOnly())) {
                result =
                        executor.tfPlan(
                                executorPath,
                                request.getVariables(),
                                request.getEnvVariables(),
                                taskWorkspace);
            } else {
                result =
                        executor.tfApply(
                                executorPath,
                                request.getVariables(),
                                request.getEnvVariables(),
                                taskWorkspace);
            }
        } catch (InvalidTerraformToolException | TerraformExecutorException tfEx) {
            log.error("Terraform deploy service failed. error:{}", tfEx.getMessage());
            result = new SystemCmdResult();
            result.setCommandSuccessful(false);
            result.setCommandStdError(tfEx.getMessage());
        }
        TerraformResult terraformResult =
                transSystemCmdResultToTerraformResult(result, taskWorkspace, scriptFiles);
        terraformResult.setTerraformVersionUsed(
                versionHelper.getExactVersionOfExecutor(executorPath));
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        terraformResult.setRequestId(request.getRequestId());
        return terraformResult;
    }

    /** Destroy resource of the service. */
    public TerraformResult destroyFromDirectory(
            TerraformDestroyFromDirectoryRequest request,
            String taskWorkspace,
            List<File> scriptFiles) {
        SystemCmdResult result;
        String executorPath = null;
        try {
            executorPath =
                    installer.getExecutorPathThatMatchesRequiredVersion(
                            request.getTerraformVersion());
            result =
                    executor.tfDestroy(
                            executorPath,
                            request.getVariables(),
                            request.getEnvVariables(),
                            taskWorkspace);
        } catch (InvalidTerraformToolException | TerraformExecutorException tfEx) {
            log.error("Terraform destroy service failed. error:{}", tfEx.getMessage());
            result = new SystemCmdResult();
            result.setCommandSuccessful(false);
            result.setCommandStdError(tfEx.getMessage());
        }
        TerraformResult terraformResult =
                transSystemCmdResultToTerraformResult(result, taskWorkspace, scriptFiles);
        terraformResult.setTerraformVersionUsed(
                versionHelper.getExactVersionOfExecutor(executorPath));
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        terraformResult.setRequestId(request.getRequestId());
        return terraformResult;
    }

    /** Executes terraform plan command on a directory and returns the plan as a JSON string. */
    public TerraformPlan getTerraformPlanFromDirectory(
            TerraformPlanFromDirectoryRequest request, String taskWorkspace) {
        String executorPath =
                installer.getExecutorPathThatMatchesRequiredVersion(request.getTerraformVersion());
        String result =
                executor.getTerraformPlanAsJson(
                        executorPath,
                        request.getVariables(),
                        request.getEnvVariables(),
                        taskWorkspace);
        scriptsHelper.deleteTaskWorkspace(taskWorkspace);
        TerraformPlan terraformPlan = TerraformPlan.builder().plan(result).build();
        terraformPlan.setTerraformVersionUsed(
                versionHelper.getExactVersionOfExecutor(executorPath));
        return terraformPlan;
    }

    /** Async deploy a source by terraform. */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDeployWithScripts(
            TerraformAsyncDeployFromDirectoryRequest asyncDeployRequest,
            String taskWorkspace,
            List<File> scriptFiles) {
        TerraformResult result;
        try {
            result = deployFromDirectory(asyncDeployRequest, taskWorkspace, scriptFiles);
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
            TerraformAsyncModifyFromDirectoryRequest asyncModifyRequest,
            String taskWorkspace,
            List<File> scriptFiles) {
        TerraformResult result;
        try {
            result = modifyFromDirectory(asyncModifyRequest, taskWorkspace, scriptFiles);
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
        log.info("Deployment service complete, callback POST url:{}, requestBody:{}", url, result);
        sendTerraformResult(url, result);
    }

    /** Async destroy resource of the service. */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDestroyWithScripts(
            TerraformAsyncDestroyFromDirectoryRequest request,
            String taskWorkspace,
            List<File> scriptFiles) {
        TerraformResult result;
        try {
            result = destroyFromDirectory(request, taskWorkspace, scriptFiles);
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

    private TerraformResult transSystemCmdResultToTerraformResult(
            SystemCmdResult result, String taskWorkspace, List<File> scriptFiles) {
        TerraformResult terraformResult =
                TerraformResult.builder().isCommandSuccessful(result.isCommandSuccessful()).build();
        try {
            BeanUtils.copyProperties(result, terraformResult);
            terraformResult.setTerraformState(scriptsHelper.getTerraformState(taskWorkspace));
            terraformResult.setGeneratedFileContentMap(
                    scriptsHelper.getDeploymentGeneratedFilesContent(taskWorkspace, scriptFiles));
        } catch (Exception e) {
            log.error("Failed to get terraform state and generated files content.", e);
        }
        return terraformResult;
    }
}
