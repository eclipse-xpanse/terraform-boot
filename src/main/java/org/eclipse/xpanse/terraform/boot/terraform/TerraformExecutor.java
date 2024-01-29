/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
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

    private static final String VARS_FILE_NAME = "variables.tfvars.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private final String moduleParentDirectoryPath;

    private final SystemCmd systemCmd;

    private final boolean isStdoutStdErrLoggingEnabled;

    private final String customTerraformBinary;

    private final String terraformLogLevel;

    /**
     * Constructor for the TerraformExecutor bean.
     *
     * @param systemCmd                    SystemCmd bean
     * @param moduleParentDirectoryPath    value of `terraform.root.module.directory` property
     * @param isStdoutStdErrLoggingEnabled value of `log.terraform.stdout.stderr` property
     * @param customTerraformBinary        value of `terraform.binary.location` property
     * @param terraformLogLevel            value of `terraform.log.level` property
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
     * Terraform executes init, plan and destroy commands.
     */
    public SystemCmdResult tfDestroy(Map<String, Object> variables,
                                     Map<String, String> envVariables, String moduleDirectory) {
        tfPlan(variables, envVariables, moduleDirectory);
        SystemCmdResult applyResult =
                tfDestroyCommand(variables, envVariables, getModuleFullPath(moduleDirectory));
        if (!applyResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfDestroy failed.");
            throw new TerraformExecutorException("TFExecutor.tfDestroy failed.",
                    applyResult.getCommandStdError());
        }
        return applyResult;
    }

    /**
     * Terraform executes init, plan and apply commands.
     */
    public SystemCmdResult tfApply(Map<String, Object> variables, Map<String, String> envVariables,
                                   String moduleDirectory) {
        tfPlan(variables, envVariables, moduleDirectory);
        SystemCmdResult applyResult =
                tfApplyCommand(variables, envVariables, getModuleFullPath(moduleDirectory));
        if (!applyResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfApply failed.");
            throw new TerraformExecutorException("TFExecutor.tfApply failed.",
                    applyResult.getCommandStdError());
        }
        return applyResult;
    }

    /**
     * Terraform executes init and plan commands.
     */
    public SystemCmdResult tfPlan(Map<String, Object> variables, Map<String, String> envVariables,
                                  String moduleDirectory) {
        tfInit(moduleDirectory);
        SystemCmdResult planResult =
                tfPlanCommand(variables, envVariables, getModuleFullPath(moduleDirectory));
        if (!planResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfPlan failed.");
            throw new TerraformExecutorException("TFExecutor.tfPlan failed.",
                    planResult.getCommandStdError());
        }
        return planResult;
    }

    /**
     * Method to execute terraform plan and get the plan as a json string.
     */
    public String getTerraformPlanAsJson(Map<String, Object> variables,
                                         Map<String, String> envVariables,
                                         String moduleDirectory) {
        tfInit(moduleDirectory);
        SystemCmdResult tfPlanResult = executeWithVariables(
                new StringBuilder(
                        getTerraformCommand("plan -input=false -no-color --out tfplan.binary ")),
                variables, envVariables, getModuleFullPath(moduleDirectory));
        if (!tfPlanResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfPlan failed.");
            throw new TerraformExecutorException("TFExecutor.tfPlan failed.",
                    tfPlanResult.getCommandStdError());
        }
        SystemCmdResult planJsonResult = execute(getTerraformCommand("show -json tfplan.binary"),
                getModuleFullPath(moduleDirectory), envVariables);
        if (!planJsonResult.isCommandSuccessful()) {
            log.error("Reading Terraform plan as JSON failed.");
            throw new TerraformExecutorException("Reading Terraform plan as JSON failed.",
                    planJsonResult.getCommandStdError());
        }
        return planJsonResult.getCommandStdOutput();
    }

    /**
     * Terraform executes the init command.
     */
    public SystemCmdResult tfValidate(String moduleDirectory) {
        tfInit(moduleDirectory);
        SystemCmdResult validateResult = tfValidateCommand(getModuleFullPath(moduleDirectory));
        if (!validateResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfValidate failed.");
            throw new TerraformExecutorException("TFExecutor.tfValidate failed.",
                    validateResult.getCommandStdError());
        }
        return validateResult;
    }

    /**
     * Terraform executes the init command.
     */
    public void tfInit(String moduleDirectory) {
        SystemCmdResult initResult = tfInitCommand(getModuleFullPath(moduleDirectory));
        if (!initResult.isCommandSuccessful()) {
            log.error("TFExecutor.tfInit failed.");
            throw new TerraformExecutorException("TFExecutor.tfInit failed.",
                    initResult.getCommandStdError());
        }
    }

    /**
     * Get the full path of Module.
     */
    public String getModuleFullPath(String moduleDirectory) {
        return this.moduleParentDirectoryPath + File.separator + moduleDirectory;
    }


    /**
     * Executes terraform init command.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult tfInitCommand(String workspace) {
        return execute(getTerraformCommand("init -no-color"),
                workspace, new HashMap<>());
    }

    /**
     * Executes terraform validate command.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult tfValidateCommand(String workspace) {
        return execute(getTerraformCommand("validate -json -no-color"),
                workspace, new HashMap<>());
    }

    /**
     * Executes terraform plan command.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult tfPlanCommand(Map<String, Object> variables,
                                          Map<String, String> envVariables, String workspace) {
        return executeWithVariables(
                new StringBuilder(getTerraformCommand("plan -input=false -no-color ")),
                variables, envVariables, workspace);
    }

    /**
     * Executes terraform apply command.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult tfApplyCommand(Map<String, Object> variables,
                                           Map<String, String> envVariables, String workspace) {
        return executeWithVariables(
                new StringBuilder(
                        getTerraformCommand("apply -auto-approve -input=false -no-color ")),
                variables, envVariables, workspace);
    }

    /**
     * Executes terraform destroy command.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult tfDestroyCommand(Map<String, Object> variables,
                                             Map<String, String> envVariables, String workspace) {
        return executeWithVariables(
                new StringBuilder("terraform destroy -auto-approve -input=false -no-color "),
                variables, envVariables, workspace);
    }

    /**
     * Executes terraform commands with parameters.
     *
     * @return Returns result of SystemCmd executed.
     */
    private SystemCmdResult executeWithVariables(StringBuilder command,
                                                 Map<String, Object> variables,
                                                 Map<String, String> envVariables,
                                                 String workspace) {
        createVariablesFile(variables, workspace);
        command.append(" -var-file=");
        command.append(VARS_FILE_NAME);
        SystemCmdResult systemCmdResult = execute(command.toString(), workspace, envVariables);
        cleanUpVariablesFile(workspace);
        return systemCmdResult;
    }

    /**
     * Executes terraform commands.
     *
     * @return SystemCmdResult
     */
    private SystemCmdResult execute(String cmd, String workspace,
                                    @NonNull Map<String, String> envVariables) {
        envVariables.putAll(getTerraformLogConfig());
        return this.systemCmd.execute(cmd, workspace, this.isStdoutStdErrLoggingEnabled,
                envVariables);
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

    private void createVariablesFile(Map<String, Object> variables, String workspace) {
        try {
            log.info("creating variables file");
            OBJECT_MAPPER.writeValue(new File(getVariablesFilePath(workspace)), variables);
        } catch (IOException ioException) {
            throw new TerraformExecutorException("Creating variables file failed",
                    ioException.getMessage());
        }
    }

    private void cleanUpVariablesFile(String workspace) {
        File file = new File(getVariablesFilePath(workspace));
        try {
            log.info("cleaning up variables file");
            Files.deleteIfExists(file.toPath());
        } catch (IOException ioException) {
            log.error("Cleanup of variables file failed", ioException);
        }
    }

    private String getVariablesFilePath(String workspace) {
        return workspace + File.separator + VARS_FILE_NAME;
    }
}
