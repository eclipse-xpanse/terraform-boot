/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform;

import jakarta.annotation.Resource;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Bean help to install terraform with specific version.
 */
@Slf4j
@Component
public class TerraformInstaller {

    @Value("${terraform.download.base.url:https://releases.hashicorp.com/terraform}")
    private String terraformDownloadBaseUrl;
    @Value("${terraform.install.dir:/opt/terraform}")
    private String terraformInstallDir;
    @Resource
    private TerraformVersionHelper versionHelper;

    /**
     * Find the executable binary path of the Terraform tool that matches the required version.
     * If no matching executable binary is found, install the Terraform tool with the required
     * version and then return the path.
     *
     * @param requiredVersion The required version of Terraform tool.
     * @return The path of the executable binary.
     */
    @Retryable(retryFor = InvalidTerraformToolException.class,
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
            log.info("Not found any terraform executor matched the required version {} from the "
                            + "terraform installation dir {}, start to download and install one.",
                    requiredVersion, this.terraformInstallDir);
            return installTerraformByRequiredVersion(requiredOperator, requiredNumber);
        }
        return matchedVersionExecutorPath;
    }

    private String installTerraformByRequiredVersion(String requiredOperator,
                                                     String requiredNumber) {
        String matchedVersionNumber =
                this.versionHelper.getBestAvailableVersionFromTerraformWebsite(
                this.terraformDownloadBaseUrl, requiredOperator, requiredNumber);
        if (StringUtils.isBlank(matchedVersionNumber)) {
            String errorMsg = String.format("Not found any terraform executor matched the required "
                            + "version %s from the terraform download url %s.",
                    requiredOperator + requiredNumber, this.terraformDownloadBaseUrl);
            log.error(errorMsg);
            throw new InvalidTerraformToolException(errorMsg);
        }
        // Install the executor with specific version into the path.
        String terraformExecutorName =
                this.versionHelper.getTerraformExecutorName(matchedVersionNumber);
        File terraformExecutorFile = new File(this.terraformInstallDir, terraformExecutorName);
        File parentDir = terraformExecutorFile.getParentFile();
        try {
            if (!parentDir.exists()) {
                log.info("Created the installation dir {} {}.", parentDir.getAbsolutePath(),
                        parentDir.mkdirs() ? "successfully" : "failed");
            }
            // download the binary zip file into the installation directory
            File terraformZipFile = downloadTerraformBinaryZipFile(matchedVersionNumber);
            String executorName = terraformExecutorFile.getName();
            // unzip the zip file and move the executable binary to the installation directory
            unzipBinaryZipToGetExecutor(terraformZipFile, executorName);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new InvalidTerraformToolException(e.getMessage());
        }
        // delete the non-executable files
        deleteNonExecutorFiles(parentDir);
        // check the executable binary

        if (this.versionHelper.checkIfExecutorVersionIsValid(
                terraformExecutorFile, requiredOperator, requiredNumber)) {
            log.info("Installed terraform version {} into the dir {} successfully.",
                    terraformExecutorFile.getAbsolutePath(), requiredOperator + requiredNumber);
            return terraformExecutorFile.getAbsolutePath();
        }
        String errorMsg = String.format("Installing terraform version %s into the dir %s failed. ",
                requiredOperator + requiredNumber, this.terraformInstallDir);
        log.error(errorMsg);
        throw new InvalidTerraformToolException(errorMsg);
    }


    private File downloadTerraformBinaryZipFile(String versionNumber) throws IOException {
        String binaryZipFileName = this.versionHelper.getTerraformBinaryFileName(versionNumber);
        File binaryZipFile = new File(this.terraformInstallDir, binaryZipFileName);
        String binaryDownloadUrl = this.versionHelper.getTerraformBinaryDownloadUrl(
                this.terraformDownloadBaseUrl, versionNumber, binaryZipFileName);
        URL url = URI.create(binaryDownloadUrl).toURL();
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
                FileOutputStream fos = new FileOutputStream(binaryZipFile, false)) {
            log.info("Downloading terraform binary file from {} to {}", url,
                    binaryZipFile.getAbsolutePath());
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            log.info("Downloaded terraform binary file from {} to {} successfully.",
                    url, binaryZipFile.getAbsolutePath());
        }
        return binaryZipFile;
    }


    private void unzipBinaryZipToGetExecutor(File binaryZipFile, String executorName)
            throws IOException {
        if (!binaryZipFile.exists()) {
            String errorMsg = String.format("Terraform binary zip file %s not found.",
                    binaryZipFile.getAbsolutePath());
            log.error(errorMsg);
            throw new IOException(errorMsg);
        }
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(binaryZipFile))) {
            log.info("Unzipping Terraform zip package {}", binaryZipFile.getAbsolutePath());
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String entryName = entry.getName();
                    File entryDestinationFile = new File(this.terraformInstallDir, entryName);
                    if (isExecutorFileInZipForTerraform(executorName)) {
                        extractFile(zis, entryDestinationFile);
                        File executorFile = new File(this.terraformInstallDir, executorName);
                        Files.move(entryDestinationFile.toPath(), executorFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        log.info("Unzipped Terraform binary file {} to get the executor {} "
                                        + "successfully.",
                                binaryZipFile.getAbsolutePath(), executorFile.getAbsolutePath());
                    }
                }
            }
        }
    }

    private boolean isExecutorFileInZipForTerraform(String entryName) {
        return entryName.startsWith("terraform");
    }


    private void extractFile(ZipInputStream zis, File destinationFile) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(destinationFile))) {
            byte[] bytesIn = new byte[4096];
            int read;
            while ((read = zis.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        }
    }

    private void deleteNonExecutorFiles(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteNonExecutorFiles(file);
                } else {
                    if (!file.getName().startsWith("terraform-") && !file.delete()) {
                        log.warn("Failed to delete file {}.", file.getAbsolutePath());
                    }
                }
            }
        }
    }
}
