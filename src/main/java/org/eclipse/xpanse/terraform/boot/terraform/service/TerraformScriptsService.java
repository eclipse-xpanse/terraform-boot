/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.async.TaskConfiguration;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformAsyncDeployFromScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformAsyncDestroyFromScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDeployWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.request.scripts.TerraformDestroyWithScriptsRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.TerraformExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Terraform service classes are deployed form Scripts.
 */
@Slf4j
@Service
public class TerraformScriptsService extends TerraformDirectoryService {

    private static final String FILE_SUFFIX = ".tf";
    private static final String STATE_FILE_NAME = "terraform.tfstate";

    private final RestTemplate restTemplate;
    private final TerraformExecutor executor;
    private final TerraformScriptsHelper terraformScriptsHelper;

    /**
     * TerraformScriptsService constructor.
     */
    @Autowired
    public TerraformScriptsService(TerraformExecutor executor, RestTemplate restTemplate,
                                   TerraformScriptsHelper terraformScriptsHelper) {
        super(executor, restTemplate);
        this.executor = executor;
        this.restTemplate = restTemplate;

        this.terraformScriptsHelper = terraformScriptsHelper;
    }

    /**
     * Method of deployment a service using a script.
     */
    public TerraformValidationResult validateWithScripts(
            TerraformDeployWithScriptsRequest request) {
        UUID uuid = UUID.randomUUID();
        buildDeployEnv(request.getScripts(), uuid);
        return tfValidateFromDirectory(uuid.toString());
    }

    /**
     * Method of deployment a service using a script.
     */
    public TerraformResult deployWithScripts(TerraformDeployWithScriptsRequest request, UUID uuid) {
        buildDeployEnv(request.getScripts(), uuid);
        return deployFromDirectory(request, uuid.toString());
    }

    /**
     * Method of destroy a service using a script.
     */
    public TerraformResult destroyWithScripts(TerraformDestroyWithScriptsRequest request,
                                              UUID uuid) {
        buildDestroyEnv(request.getScripts(), request.getTfState(), uuid);
        return destroyFromDirectory(request, uuid.toString());
    }

    /**
     * Method to get terraform plan.
     */
    public TerraformPlan getTerraformPlanFromScripts(TerraformPlanWithScriptsRequest request,
                                                     UUID uuid) {
        buildDeployEnv(request.getScripts(), uuid);
        return getTerraformPlanFromDirectory(request, uuid.toString());
    }

    /**
     * Async deploy a source by terraform.
     */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDeployWithScripts(
            TerraformAsyncDeployFromScriptsRequest asyncDeployRequest, UUID uuid) {
        TerraformResult result;
        try {
            result = deployWithScripts(asyncDeployRequest, uuid);
        } catch (RuntimeException e) {
            result = TerraformResult.builder()
                    .commandStdOutput(null)
                    .commandStdError(e.getMessage())
                    .isCommandSuccessful(false)
                    .terraformState(null)
                    .importantFileContentMap(new HashMap<>())
                    .build();
        }
        String url = asyncDeployRequest.getWebhookConfig().getUrl();
        log.info("Deployment service complete, callback POST url:{}, requestBody:{}", url, result);
        restTemplate.postForLocation(url, result);
    }

    /**
     * Async destroy resource of the service.
     */
    @Async(TaskConfiguration.TASK_EXECUTOR_NAME)
    public void asyncDestroyWithScripts(TerraformAsyncDestroyFromScriptsRequest request,
                                        UUID uuid) {
        TerraformResult result;
        try {
            result = destroyWithScripts(request, uuid);
        } catch (RuntimeException e) {
            result = TerraformResult.builder()
                    .commandStdOutput(null)
                    .commandStdError(e.getMessage())
                    .isCommandSuccessful(false)
                    .terraformState(null)
                    .importantFileContentMap(new HashMap<>())
                    .build();
        }

        String url = request.getWebhookConfig().getUrl();
        log.info("Destroy service complete, callback POST url:{}, requestBody:{}", url, result);
        restTemplate.postForLocation(url, result);
    }

    private void buildDeployEnv(List<String> scripts, UUID uuid) {
        String workspace = executor.getModuleFullPath(uuid.toString());
        buildWorkspace(workspace);
        buildScriptFiles(workspace, uuid, scripts);
    }

    private void buildDestroyEnv(List<String> scripts, String tfState, UUID uuid) {
        buildDeployEnv(scripts, uuid);
        terraformScriptsHelper.createTfStateFile(tfState, uuid.toString());
    }

    private void buildWorkspace(String workspace) {
        log.info("start create workspace");
        File ws = new File(workspace);
        if (!ws.exists() && !ws.mkdirs()) {
            throw new TerraformExecutorException(
                    "Create workspace failed, File path not created: " + ws.getAbsolutePath());
        }
        log.info("workspace create success,Working directory is " + ws.getAbsolutePath());
    }

    private void buildScriptFiles(String workspace, UUID uuid, List<String> scripts) {
        log.info("start build terraform script");
        if (CollectionUtils.isEmpty(scripts)) {
            throw new TerraformExecutorException("terraform scripts create error, terraform "
                    + "scripts not exists");
        }
        StringBuilder scriptBuilder = new StringBuilder();
        for (String script : scripts) {
            scriptBuilder.append(script).append(System.lineSeparator());
        }
        String fileName = workspace + File.separator + uuid + FILE_SUFFIX;
        boolean overwrite = new File(fileName).exists();
        try (FileWriter scriptWriter = new FileWriter(fileName, overwrite)) {
            scriptWriter.write(scriptBuilder.toString());
            log.info("terraform script create success, fileName: {}", fileName);
        } catch (IOException ex) {
            log.error("terraform script create failed.", ex);
            throw new TerraformExecutorException("terraform script create failed.", ex);
        }
    }
}
