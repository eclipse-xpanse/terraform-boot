/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 *
 */

package org.eclipse.xpanse.terraform.boot.terraform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.terraform.boot.models.request.TerraformDeployRequest;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.models.validation.TerraformValidationResult;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmd;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmdResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * An executor for terraform.
 */
@Slf4j
@Component
public class TerraformExecutor {

    private final String moduleParentDirectoryPath;

    private final SystemCmd systemCmd;

    private final boolean isStdoutStdErrLoggingEnabled;

    private final String customTerraformBinary;

    private final String terraformLogLevel;

    /**
     * Constructor for the TerraformExecutor bean.
     *
     * @param systemCmd SystemCmd bean
     * @param moduleParentDirectoryPath value of `terraform.root.module.directory` property
     * @param isStdoutStdErrLoggingEnabled value of `log.terraform.stdout.stderr` property
     * @param customTerraformBinary value of `terraform.binary.location` property
     * @param terraformLogLevel value of `terraform.log.level` property
     */
    @Autowired
    public TerraformExecutor(SystemCmd systemCmd,
                             @Value("${terraform.root.module.directory}")
                             String moduleParentDirectoryPath,
                             @Value("${log.terraform.stdout.stderr:true}")
                             boolean isStdoutStdErrLoggingEnabled,
                             @Value("${terraform.binary.location}")
                             String customTerraformBinary,
                             @Value("${terraform.log.level}")
                             String terraformLogLevel
    ) {
        if (moduleParentDirectoryPath.isBlank() || moduleParentDirectoryPath.isEmpty()) {
            this.moduleParentDirectoryPath =
                    System.getProperty("java.io.tmpdir");
        } else {
            this.moduleParentDirectoryPath = moduleParentDirectoryPath;
        }
        this.systemCmd = systemCmd;
        this.customTerraformBinary = customTerraformBinary;
        this.isStdoutStdErrLoggingEnabled = isStdoutStdErrLoggingEnabled;
        this.terraformLogLevel = terraformLogLevel;
    }

    /**
     * Executes terraform init command.
     *
     * @return Returns result of SystemCmd executes.
     */
    private SystemCmdResult tfInitCommand(String workspace) {
        return execute(getTerraformCommand("init -no-color"), workspace);
    }

    /**
     * Executes terraform plan command.
     *
     * @return Returns result of SystemCmd executes.
     */
    private SystemCmdResult tfPlanCommand(Map<String, String> variables, String workspace) {
        return executeWithVariables(
                new StringBuilder(getTerraformCommand("plan -input=false -no-color ")),
                variables, workspace);
    }

    /**
     * Executes terraform apply command.
     *
     * @return Returns result of SystemCmd executes.
     */
    private SystemCmdResult tfApplyCommand(Map<String, String> variables, String workspace) {
        return executeWithVariables(
                new StringBuilder(
                        getTerraformCommand("apply -auto-approve -input=false -no-color ")),
                variables, workspace);
    }

    /**
     * Executes terraform destroy command.
     *
     * @return Returns result of SystemCmd executes.
     */
    private SystemCmdResult tfDestroyCommand(Map<String, String> variables, String workspace) {
        return executeWithVariables(
                new StringBuilder("terraform destroy -auto-approve -input=false -no-color "),
                variables, workspace);
    }

    /**
     * Executes terraform commands with parameters.
     *
     * @return Returns result of SystemCmd executes.
     */
    private SystemCmdResult executeWithVariables(StringBuilder command,
                                                 Map<String, String> variables, String workspace) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            if (Objects.nonNull(entry.getKey()) && Objects.nonNull(entry.getValue())) {
                command.append("-var=")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue())
                        .append(" ");
            }
        }
        return execute(command.toString(), workspace);
    }

    /**
     * Executes terraform commands.
     *
     * @return SystemCmdResult
     */
    private SystemCmdResult execute(String cmd, String workspace) {
        return this.systemCmd.execute(cmd, workspace, this.isStdoutStdErrLoggingEnabled,
                getTerraformLogConfig());
    }

    /**
     * Deploy a source by terraform.
     */
    public TerraformResult deploy(TerraformDeployRequest terraformDeployRequest, String workspace) {
        if (Boolean.TRUE.equals(terraformDeployRequest.getIsPlanOnly())) {
            SystemCmdResult tfPlanResult =
                    tfPlan(terraformDeployRequest.getVariables(), workspace);
            return TerraformResult.builder()
                    .commandStdOutput(tfPlanResult.getCommandStdOutput())
                    .commandStdError(tfPlanResult.getCommandStdError())
                    .isCommandSuccessful(tfPlanResult.isCommandSuccessful())
                    .build();
        } else {
            SystemCmdResult applyResult = tfApplyCommand(terraformDeployRequest.getVariables(),
                    getModuleFullPath(workspace));
            if (!applyResult.isCommandSuccessful()) {
                log.error("TFExecutor.tfApply failed.");
                throw new TerraformExecutorException("TFExecutor.tfApply failed.",
                        applyResult.getCommandStdError());
            }
            return TerraformResult.builder()
                    .commandStdOutput(applyResult.getCommandStdOutput())
                    .commandStdError(applyResult.getCommandStdError())
                    .isCommandSuccessful(applyResult.isCommandSuccessful())
                    .build();
        }
    }

    /**
     * Destroy resource of the service.
     */
    public TerraformResult destroy(Map<String, String> variables, String workspace) {
        tfPlan(variables, workspace);
        SystemCmdResult destroyResult =
                tfDestroyCommand(variables, getModuleFullPath(workspace));
        if (!destroyResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfDestroy failed.");
            throw new TerraformExecutorException("TFExecutor.tfDestroy failed.",
                    destroyResult.getCommandStdError());
        }
        return TerraformResult.builder()
                .commandStdOutput(destroyResult.getCommandStdOutput())
                .commandStdError(destroyResult.getCommandStdError())
                .isCommandSuccessful(destroyResult.isCommandSuccessful())
                .build();
    }

    /**
     * Executes terraform validate command.
     *
     * @return TfValidationResult.
     */
    public TerraformValidationResult tfValidate(String moduleDirectory) {
        tfInit(moduleDirectory);
        SystemCmdResult systemCmdResult =
                execute("terraform validate -json -no-color", getModuleFullPath(moduleDirectory));
        try {
            return new ObjectMapper().readValue(systemCmdResult.getCommandStdOutput(),
                    TerraformValidationResult.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialising string to object failed.", ex);
        }
    }

    private String getModuleFullPath(String moduleDirectory) {
        return this.moduleParentDirectoryPath + File.separator + moduleDirectory;
    }

    private SystemCmdResult tfPlan(Map<String, String> variables, String moduleDirectory) {
        tfInit(moduleDirectory);
        SystemCmdResult planResult = tfPlanCommand(variables, getModuleFullPath(moduleDirectory));
        if (!planResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfPlan failed.");
            throw new TerraformExecutorException("TFExecutor.tfPlan failed.",
                    planResult.getCommandStdError());
        }
        return planResult;
    }

    private void tfInit(String moduleDirectory) {
        SystemCmdResult initResult = tfInitCommand(getModuleFullPath(moduleDirectory));
        if (!initResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfInit failed.");
            throw new TerraformExecutorException("TFExecutor.tfInit failed.",
                    initResult.getCommandStdError());
        }
    }

    private String getTerraformCommand(String terraformArguments) {
        if (Objects.isNull(this.customTerraformBinary) || this.customTerraformBinary.isEmpty()
                || this.customTerraformBinary.isBlank()) {
            return "terraform " + terraformArguments;
        }
        return this.customTerraformBinary + " " + terraformArguments;
    }


    private Map<String, String> getTerraformLogConfig() {
        return Collections.singletonMap("TF_LOG", this.terraformLogLevel);
    }

}
