/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.service;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.exceptions.ResultAlreadyReturnedOrRequestIdInvalidException;
import org.eclipse.xpanse.terraform.boot.models.response.TerraformResult;
import org.eclipse.xpanse.terraform.boot.utils.TerraformResultSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/** Terraform service classes are manage task result. */
@Slf4j
@Component
public class TerraformResultPersistenceManage {

    private static final String TF_RESULT_FILE_SUFFIX = ".dat";
    private static final String TF_LOCK_FILE_NAME = ".terraform.tfstate.lock.info";

    @Value("${failed.callback.response.store.location}")
    private String failedCallbackStoreLocation;

    @Value("${clean.workspace.after.deployment.enabled:true}")
    private Boolean cleanWorkspaceAfterDeployment;

    @Resource private TerraformScriptsHelper scriptsHelper;
    @Resource private TerraformResultSerializer terraformResultSerializer;

    /**
     * When the terra-boot callback fails, store the TerraformResult in the local file system.
     *
     * @param result TerraformResult.
     */
    public void persistTerraformResult(TerraformResult result) {
        String filePath = getFilePath(result.getRequestId());
        File file = new File(filePath);
        if (!file.exists() && !file.mkdirs()) {
            log.error("Failed to create directory {}", filePath);
            return;
        }
        byte[] terraformResultData = terraformResultSerializer.serialize(result);
        try (FileOutputStream fos =
                new FileOutputStream(
                        file.getPath()
                                + File.separator
                                + result.getRequestId()
                                + TF_RESULT_FILE_SUFFIX)) {
            fos.write(terraformResultData);
            log.info(
                    "terraform result successfully stored to directoryName: {}",
                    result.getRequestId());
        } catch (IOException e) {
            String errorMsg =
                    String.format(
                            "storing terraform result to " + "directoryName %s failed. %s",
                            result.getRequestId(), e);
            log.error(errorMsg);
        }
    }

    /**
     * Get the TerraformResult object stored when the terra-boot callback fails by RequestId.
     *
     * @param requestId requestId.
     * @return TerraformResult.
     */
    public ResponseEntity<TerraformResult> retrieveTerraformResultByRequestId(String requestId) {
        String filePath = getFilePath(UUID.fromString(requestId));
        File resultFile = new File(filePath + File.separator + requestId + TF_RESULT_FILE_SUFFIX);
        if (!resultFile.exists() && !resultFile.isFile()) {
            if (isDeployingInProgress(requestId)) {
                return ResponseEntity.noContent().build();
            }
            throw new ResultAlreadyReturnedOrRequestIdInvalidException(
                    "Result file does not exist: " + resultFile.getAbsolutePath());
        }
        try (FileInputStream fis = new FileInputStream(resultFile)) {
            byte[] terraformResultData = fis.readAllBytes();
            TerraformResult terraformResult =
                    terraformResultSerializer.deserialize(terraformResultData);
            fis.close();
            deleteResultFileAndDirectory(new File(filePath));
            return ResponseEntity.ok(terraformResult);
        } catch (IOException e) {
            log.error("Failed to retrieve TerraformResult for requestId: {}", requestId, e);
            throw new ResultAlreadyReturnedOrRequestIdInvalidException(
                    "Failed to retrieve TerraformResult for requestId: " + requestId);
        }
    }

    private boolean isDeployingInProgress(String requestId) {
        String workspace = scriptsHelper.buildTaskWorkspace(requestId);
        File targetFile;
        if (cleanWorkspaceAfterDeployment) {
            targetFile = new File(workspace);
            return targetFile.exists() && targetFile.isDirectory();
        } else {
            targetFile = new File(workspace, TF_LOCK_FILE_NAME);
            return targetFile.exists() && targetFile.isFile();
        }
    }

    private void deleteResultFileAndDirectory(File resultFile) {
        try {
            deleteRecursively(resultFile);
            log.info("File folder deleted successfully: " + resultFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("An error occurred while deleting files: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    private String getFilePath(UUID requestId) {
        return failedCallbackStoreLocation + File.separator + requestId.toString();
    }
}
