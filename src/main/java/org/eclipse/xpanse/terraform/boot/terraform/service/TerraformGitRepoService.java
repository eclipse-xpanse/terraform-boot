/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.xpanse.terraform.boot.async.TaskConfiguration;
import org.eclipse.xpanse.terraform.boot.models.exceptions.GitRepoCloneException;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlan;
import org.eclipse.xpanse.terraform.boot.models.plan.TerraformPlanFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformAsyncDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDeployFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformDestroyFromGitRepoRequest;
import org.eclipse.xpanse.terraform.boot.models.request.git.TerraformScriptGitRepoDetails;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.TerraformExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Bean to manage all Terraform execution using scripts from a GIT Repo.
 */
@Slf4j
@Component
public class TerraformGitRepoService extends TerraformDirectoryService {

    private static final int MAX_RETRY_COUNT = 5;
    private final RestTemplate restTemplate;
    private final TerraformExecutor executor;
    private final TerraformScriptsHelper terraformScriptsHelper;

    /**
     * Constructor for TerraformGitRepoService bean.
     */
    public TerraformGitRepoService(TerraformExecutor executor, RestTemplate restTemplate,
                                   TerraformScriptsHelper terraformScriptsHelper) {
        super(executor, restTemplate);
        this.restTemplate = restTemplate;
        this.executor = executor;
        this.terraformScriptsHelper = terraformScriptsHelper;
    }

    /**
     * Method of deployment a service using a script.
     */
    public TerraformValidationResult validateWithScripts(
            TerraformDeployFromGitRepoRequest request) {
        UUID uuid = UUID.randomUUID();
        buildDeployEnv(request.getGitRepoDetails(), uuid);
        return tfValidateFromDirectory(
                getScriptsLocationInRepo(request.getGitRepoDetails(), uuid));
    }

    /**
     * Method to get terraform plan.
     */
    public TerraformPlan getTerraformPlanFromGitRepo(TerraformPlanFromGitRepoRequest request,
                                                     UUID uuid) {
        uuid = getUuidToCreateEmptyWorkspace(uuid);
        buildDeployEnv(request.getGitRepoDetails(), uuid);
        return getTerraformPlanFromDirectory(request,
                getScriptsLocationInRepo(request.getGitRepoDetails(), uuid));
    }

    /**
     * Method of deployment a service using a script.
     */
    public TerraformResult deployFromGitRepo(TerraformDeployFromGitRepoRequest request, UUID uuid) {
        uuid = getUuidToCreateEmptyWorkspace(uuid);
        buildDeployEnv(request.getGitRepoDetails(), uuid);
        return deployFromDirectory(request,
                getScriptsLocationInRepo(request.getGitRepoDetails(), uuid));
    }

    /**
     * Method of destroy a service using a script.
     */
    public TerraformResult destroyFromGitRepo(TerraformDestroyFromGitRepoRequest request,
                                              UUID uuid) {
        uuid = getUuidToCreateEmptyWorkspace(uuid);
        buildDestroyEnv(request.getGitRepoDetails(), request.getTfState(), uuid);
        return destroyFromDirectory(request, getScriptsLocationInRepo(
                request.getGitRepoDetails(), uuid));
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
        String url = asyncDeployRequest.getWebhookConfig().getUrl();
        log.info("Deployment service complete, callback POST url:{}, requestBody:{}", url, result);
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
                    .destroyScenario(request.getDestroyScenario())
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

    private void buildDeployEnv(TerraformScriptGitRepoDetails terraformScriptGitRepoDetails,
                                UUID uuid) {
        String workspace = executor.getModuleFullPath(uuid.toString());
        buildWorkspace(workspace);
        extractScripts(workspace, terraformScriptGitRepoDetails);
    }

    private void buildDestroyEnv(TerraformScriptGitRepoDetails terraformScriptGitRepoDetails,
                                 String tfState, UUID uuid) {
        buildDeployEnv(terraformScriptGitRepoDetails, uuid);
        terraformScriptsHelper.createTfStateFile(tfState,
                uuid + File.separator + terraformScriptGitRepoDetails.getScriptPath());
    }

    private UUID getUuidToCreateEmptyWorkspace(UUID uuid) {
        File ws = new File(executor.getModuleFullPath(uuid.toString()));
        if (ws.exists()) {
            if (!cleanWorkspace(uuid)) {
                uuid = UUID.randomUUID();
                log.info("Clean existed workspace failed. Create empty workspace with id:{}", uuid);
                return UUID.randomUUID();
            }
        }
        return uuid;
    }

    private void buildWorkspace(String workspace) {
        log.info("start create workspace");
        File ws = new File(workspace);
        if (!ws.exists() && !ws.mkdirs()) {
            throw new TerraformExecutorException(
                    "Create workspace failed, File path not created: " + ws.getAbsolutePath());
        }
        log.info("workspace create success, Working directory is " + ws.getAbsolutePath());
    }

    private void extractScripts(String workspace, TerraformScriptGitRepoDetails scriptsRepo) {
        log.info("Cloning scripts from GIT repo:{} to workspace:{}", scriptsRepo.getRepoUrl(),
                workspace);
        File workspaceDirectory = new File(workspace);
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        repositoryBuilder.findGitDir(workspaceDirectory);
        if (Objects.isNull(repositoryBuilder.getGitDir())) {
            CloneCommand cloneCommand = new CloneCommand();
            cloneCommand.setURI(scriptsRepo.getRepoUrl());
            cloneCommand.setProgressMonitor(null);
            cloneCommand.setDirectory(workspaceDirectory);
            cloneCommand.setBranch(scriptsRepo.getBranch());
            boolean cloneSuccess = false;
            int retryCount = 0;
            String errMsg = "";
            while (!cloneSuccess && retryCount < MAX_RETRY_COUNT) {
                try {
                    cloneCommand.call();
                    cloneSuccess = true;
                } catch (GitAPIException e) {
                    retryCount++;
                    errMsg =
                            String.format("Cloning scripts form GIT repo error:%s", e.getMessage());
                    log.error(errMsg);
                }
            }
            if (!cloneSuccess) {
                throw new GitRepoCloneException(errMsg);
            }
        }
    }

    private String getScriptsLocationInRepo(
            TerraformScriptGitRepoDetails terraformScriptGitRepoDetails, UUID uuid) {
        if (StringUtils.isNotBlank(terraformScriptGitRepoDetails.getScriptPath())) {
            return uuid + File.separator + terraformScriptGitRepoDetails.getScriptPath();
        }
        return uuid.toString();
    }

    private boolean cleanWorkspace(UUID uuid) {
        boolean cleanSuccess = true;
        try {
            String workspace = executor.getModuleFullPath(uuid.toString());
            Path path = Paths.get(workspace).toAbsolutePath().normalize();
            List<File> files =
                    Files.walk(path).sorted(Comparator.reverseOrder()).map(Path::toFile).toList();
            for (File file : files) {
                if (!file.delete()) {
                    cleanSuccess = false;
                    log.info("Failed to delete file: {}", file.getAbsolutePath());
                }
            }
        } catch (IOException e) {
            return false;
        }
        return cleanSuccess;
    }

}
