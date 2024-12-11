/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.tool;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/** Bean help to install terraform with specific version. */
@Slf4j
@Component
public class TerraformInstaller {

    @Value("${terraform.download.base.url:https://releases.hashicorp.com/terraform}")
    private String terraformDownloadBaseUrl;

    @Value("${terraform.install.dir:/opt/terraform}")
    private String terraformInstallDir;

    @Resource private TerraformVersionsCache versionsCache;
    @Resource private TerraformVersionsHelper versionHelper;

    /**
     * Find the executable binary path of the Terraform tool that matches the required version. If
     * no matching executable binary is found, install the Terraform tool with the required version
     * and then return the path.
     *
     * @param requiredVersion The required version of Terraform tool.
     * @return The path of the executable binary.
     */
    @Retryable(
            retryFor = InvalidTerraformToolException.class,
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${spring.retry.delay-millions}"))
    public String getExecutorPathThatMatchesRequiredVersion(String requiredVersion) {
        if (StringUtils.isBlank(requiredVersion)) {
            log.info("No required version of terraform is specified, use the default terraform.");
            return "terraform";
        }
        String[] operatorAndNumber =
                this.versionHelper.getOperatorAndNumberFromRequiredVersion(requiredVersion);
        String requiredOperator = operatorAndNumber[0];
        String requiredNumber = operatorAndNumber[1];
        // Get path of the executor matched required version in the environment.
        String matchedVersionExecutorPath =
                this.versionHelper.getExecutorPathMatchedRequiredVersion(
                        this.terraformInstallDir, requiredOperator, requiredNumber);
        if (StringUtils.isBlank(matchedVersionExecutorPath)) {
            log.info(
                    "Not found any terraform executor matched the required version {} from the "
                            + "terraform installation dir {}, start to download and install one.",
                    requiredVersion,
                    this.terraformInstallDir);
            return installTerraformByRequiredVersion(requiredOperator, requiredNumber);
        }
        return matchedVersionExecutorPath;
    }

    private String installTerraformByRequiredVersion(
            String requiredOperator, String requiredNumber) {
        String bestVersionNumber =
                getBestAvailableVersionMatchingRequiredVersion(requiredOperator, requiredNumber);
        File installedExecutorFile =
                this.versionHelper.installTerraformWithVersion(
                        bestVersionNumber, this.terraformDownloadBaseUrl, this.terraformInstallDir);
        if (this.versionHelper.checkIfExecutorCanBeExecuted(installedExecutorFile)) {
            log.info("Terraform with version {}  installed successfully.", installedExecutorFile);
            return installedExecutorFile.getAbsolutePath();
        }
        String errorMsg =
                String.format(
                        "Installing terraform with version %s into the dir %s " + "failed. ",
                        bestVersionNumber, this.terraformInstallDir);
        log.error(errorMsg);
        throw new InvalidTerraformToolException(errorMsg);
    }

    /**
     * Get the best available version in download url.
     *
     * @param requiredOperator operator in required version
     * @param requiredNumber number in required version
     * @return the best available version existed in download url.
     */
    private String getBestAvailableVersionMatchingRequiredVersion(
            String requiredOperator, String requiredNumber) {
        Set<String> availableVersions = this.versionsCache.getAvailableVersions();
        String bestAvailableVersion =
                this.versionHelper.findBestVersionFromAllAvailableVersions(
                        availableVersions, requiredOperator, requiredNumber);
        if (StringUtils.isNotBlank(bestAvailableVersion)) {
            log.info(
                    "Found the best available version {} for terraform by the required version "
                            + "{}.",
                    bestAvailableVersion,
                    requiredOperator + requiredNumber);
            return bestAvailableVersion;
        }
        String errorMsg =
                String.format(
                        "Failed to find available versions for terraform by the "
                                + "required version %s.",
                        requiredOperator + requiredNumber);
        log.error(errorMsg);
        throw new InvalidTerraformToolException(errorMsg);
    }
}
