/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.terraform.boot.models.exceptions.TerraformExecutorException;
import org.eclipse.xpanse.terraform.boot.terraform.TerraformExecutor;
import org.springframework.stereotype.Component;

/**
 * bean to host all generic methods shared from different types of Terraform deployers.
 */
@Slf4j
@Component
public class TerraformScriptsHelper {

    private static final String STATE_FILE_NAME = "terraform.tfstate";

    private final TerraformExecutor terraformExecutor;

    public TerraformScriptsHelper(TerraformExecutor terraformExecutor) {
        this.terraformExecutor = terraformExecutor;
    }

    /**
     * Creates the tfstate file in the directory of the Terraform module.
     *
     * @param tfState        state file contents as string.
     * @param moduleLocation module location where the file must be created.
     */
    public void createTfStateFile(String tfState, String moduleLocation) {
        if (StringUtils.isBlank(tfState)) {
            throw new TerraformExecutorException("terraform .tfState file create error");
        }
        String fileName =
                terraformExecutor.getModuleFullPath(moduleLocation)
                        + File.separator
                        + STATE_FILE_NAME;
        try (FileWriter scriptWriter = new FileWriter(fileName)) {
            scriptWriter.write(tfState);
            log.info("terraform .tfState file create success, fileName: {}", fileName);
        } catch (IOException ex) {
            log.error("terraform .tfState file create failed.", ex);
            throw new TerraformExecutorException("terraform .tfState file create failed.", ex);
        }
    }
}