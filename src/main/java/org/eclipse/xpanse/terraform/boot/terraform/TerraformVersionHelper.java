/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmd;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmdResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.semver4j.Semver;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Defines methods for handling terraform with required version.
 */
@Slf4j
@Component
public class TerraformVersionHelper {

    /**
     * Terraform version required version regex.
     */
    public static final String TERRAFORM_REQUIRED_VERSION_REGEX =
            "^(=|>=|<=)\\s*[vV]?\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$";
    private static final Pattern TERRAFORM_REQUIRED_VERSION_PATTERN =
            Pattern.compile(TERRAFORM_REQUIRED_VERSION_REGEX);
    private static final Pattern TERRAFORM_VERSION_OUTPUT_PATTERN =
            Pattern.compile("^Terraform\\s+v(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
    private static final Pattern TERRAFORM_BINARY_HREF_PATTERN =
            Pattern.compile("^/terraform/(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/");
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String OS_ARCH = System.getProperty("os.arch").toLowerCase();

    @Resource
    private SystemCmd systemCmd;

    /**
     * Get terraform executor path which matches the required version.
     *
     * @param installationDir  terraform installation directory
     * @param requiredOperator operator in required version
     * @param requiredNumber   number in required version
     * @return return the version of terraform which is matched required, otherwise return null.
     */
    public String getExecutorPathMatchedRequiredVersion(String installationDir,
                                                        String requiredOperator,
                                                        String requiredNumber) {
        // Get path of terraform executor matched required version in the installation dir.
        File installDir = new File(installationDir);
        if (!installDir.exists() || !installDir.isDirectory()) {
            return null;
        }
        Map<String, File> executorVersionFileMap = new HashMap<>();
        Arrays.stream(installDir.listFiles())
                .filter(f -> f.isFile() && f.canExecute() && f.getName().startsWith("terraform-"))
                .forEach(f -> {
                    String versionNumber = getVersionFromExecutorPath(f.getAbsolutePath());
                    executorVersionFileMap.put(versionNumber, f);
                });
        if (CollectionUtils.isEmpty(executorVersionFileMap)) {
            return null;
        }
        String findBestVersion = findBestVersion(executorVersionFileMap.keySet().stream().toList(),
                requiredOperator, requiredNumber);
        if (StringUtils.isNotBlank(findBestVersion)) {
            File executorFile = executorVersionFileMap.get(findBestVersion);
            if (checkIfExecutorVersionIsValid(executorFile, requiredOperator, requiredNumber)) {
                return executorFile.getAbsolutePath();
            }
        }
        return null;
    }

    /**
     * Get the operator and number from the required version.
     *
     * @param requiredVersion required version
     * @return string array, the first element is operator, the second element is number.
     */
    public String[] getOperatorAndNumberFromRequiredVersion(String requiredVersion) {

        String version = requiredVersion.replaceAll("\\s+", "").toLowerCase()
                .replaceAll("v", "");
        if (StringUtils.isNotBlank(version)) {
            Matcher matcher = TERRAFORM_REQUIRED_VERSION_PATTERN.matcher(version);
            if (matcher.find()) {
                String[] operatorAndNumber = new String[2];
                operatorAndNumber[0] = matcher.group(1);
                operatorAndNumber[1] = matcher.group(0).replaceAll("^(=|>=|<=)", "");
                return operatorAndNumber;
            }
        }
        String errorMsg = String.format(
                "Invalid terraform required version format:%s", requiredVersion);
        throw new InvalidTerraformToolException(errorMsg);
    }


    /**
     * Get the best available version in download url.
     *
     * @param downloadBaseUrl  the base url of download
     * @param requiredOperator operator in required version
     * @param requiredNumber   number in required version
     * @return the best available version existed in download url.
     */
    public String getBestAvailableVersionFromTerraformWebsite(String downloadBaseUrl,
                                                              String requiredOperator,
                                                              String requiredNumber) {
        List<String> allVersionsCanBeDownloaded = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(downloadBaseUrl).get();
            Elements elements = doc.select("a[href]");
            for (Element element : elements) {
                String href = element.attr("href");
                Matcher matcher = TERRAFORM_BINARY_HREF_PATTERN.matcher(href);
                if (matcher.find()) {
                    allVersionsCanBeDownloaded.add(matcher.group(1));
                }
            }
        } catch (IOException e) {
            String errorMsg = String.format("Query all versions of Terraform from the download url"
                    + " %s error.", downloadBaseUrl);
            log.error(errorMsg, e);
        }
        if (CollectionUtils.isEmpty(allVersionsCanBeDownloaded)) {
            String errorMsg = String.format("Not found any versions of Terraform from the "
                    + "download url %s.", downloadBaseUrl);
            log.error(errorMsg);
            throw new InvalidTerraformToolException(errorMsg);
        }
        return findBestVersion(allVersionsCanBeDownloaded, requiredOperator, requiredNumber);
    }

    private String findBestVersion(List<String> allAvailableVersions,
                                   String requiredOperator, String requiredNumber) {
        if (CollectionUtils.isEmpty(allAvailableVersions)
                || StringUtils.isBlank(requiredOperator) || StringUtils.isBlank(requiredNumber)) {
            return null;
        }
        Semver requiredSemver = new Semver(requiredNumber);
        return switch (requiredOperator) {
            case "=" -> allAvailableVersions.stream()
                    .filter(v -> new Semver(v).isEqualTo(requiredSemver))
                    .findAny().orElse(null);
            case ">=" -> allAvailableVersions.stream()
                    .filter(v -> new Semver(v).isGreaterThanOrEqualTo(requiredSemver))
                    .min(Comparator.naturalOrder()).orElse(null);
            case "<=" -> allAvailableVersions.stream()
                    .filter(v -> new Semver(v).isLowerThanOrEqualTo(requiredSemver))
                    .max(Comparator.naturalOrder()).orElse(null);
            default -> null;
        };
    }

    /**
     * Check if the exact version of terraform executor is valid.
     *
     * @param executorFile     executor file
     * @param requiredOperator operator in required version
     * @param requiredNumber   number in required version
     * @return true if the version is valid, otherwise return false.
     */
    public boolean checkIfExecutorVersionIsValid(File executorFile, String requiredOperator,
                                                 String requiredNumber) {
        if (!executorFile.exists() && !executorFile.isFile()) {
            return false;
        }
        if (!executorFile.canExecute()) {
            SystemCmdResult chmodResult = systemCmd.execute(
                    String.format("chmod +x %s", executorFile.getAbsolutePath()),
                    5, System.getProperty("java.io.tmpdir"), false, new HashMap<>());
            if (!chmodResult.isCommandSuccessful()) {
                log.error(chmodResult.getCommandStdError());
                return false;
            }
        }
        SystemCmdResult versionCheckResult = systemCmd.execute(
                executorFile.getAbsolutePath() + " -v",
                5, System.getProperty("java.io.tmpdir"), false, new HashMap<>());
        if (versionCheckResult.isCommandSuccessful()) {
            String actualVersion =
                    getActualVersionFromCommandOutput(versionCheckResult.getCommandStdOutput());
            return isVersionSatisfied(actualVersion, requiredOperator, requiredNumber);
        }
        return false;
    }


    /**
     * Get the exact version of terraform executor.
     *
     * @param executorPath the path of terraform executor
     * @return the exact version of terraform executor
     */
    public String getExactVersionOfExecutor(String executorPath) {
        if (StringUtils.isBlank(executorPath)) {
            return null;
        }
        String exactVersion;
        try {
            SystemCmdResult versionCheckResult = systemCmd.execute(executorPath + " -v",
                    5, System.getProperty("java.io.tmpdir"), false, new HashMap<>());
            if (versionCheckResult.isCommandSuccessful()) {
                exactVersion =
                        getActualVersionFromCommandOutput(versionCheckResult.getCommandStdOutput());
            } else {
                log.error(versionCheckResult.getCommandStdError());
                exactVersion = getVersionFromExecutorPath(executorPath);
            }
        } catch (Exception e) {
            log.error("Get exact version of the executor {} failed.", executorPath, e);
            exactVersion = executorPath;
        }
        return exactVersion;
    }


    private String getVersionFromExecutorPath(String executorPath) {
        if (executorPath.contains("-")) {
            return Arrays.asList(executorPath.split("-")).getLast();
        }
        return null;
    }

    private String getActualVersionFromCommandOutput(String commandOutput) {
        Matcher matcher = TERRAFORM_VERSION_OUTPUT_PATTERN.matcher(commandOutput);
        if (matcher.find()) {
            return matcher.group(1);
        }
        String errorMsg = String.format("Cannot find the version from command output：%s",
                commandOutput);
        throw new InvalidTerraformToolException(errorMsg);
    }

    private boolean isVersionSatisfied(String actualNumber, String requiredOperator,
                                       String requiredNumber) {
        Semver actualSemver = new Semver(actualNumber);
        Semver requiredSemver = new Semver(requiredNumber);
        if ("=".equals(requiredOperator)) {
            return actualSemver.isEqualTo(requiredSemver);
        } else if (">=".equals(requiredOperator)) {
            return actualSemver.isGreaterThanOrEqualTo(requiredSemver);
        } else if ("<=".equals(requiredOperator)) {
            return actualSemver.isLowerThanOrEqualTo(requiredSemver);
        }
        return false;
    }


    /**
     * Get terraform executor name with version.
     *
     * @param versionNumber version number
     * @return binary file name
     */
    public String getTerraformExecutorName(String versionNumber) {
        return String.format("terraform-%s", versionNumber);
    }

    /**
     * Get terraform binary file name.
     *
     * @param versionNumber version number
     * @return binary file name
     */
    public String getTerraformBinaryFileName(String versionNumber) {
        return String.format("terraform_%s_%s_%s.zip",
                versionNumber, getOperatingSystemCode(), OS_ARCH);
    }


    /**
     * Get terraform binary file download url.
     *
     * @param downloadBaseUrl download base url
     * @param versionNumber   version number
     * @param binaryFileName  binary file name
     * @return binary file name
     */
    public String getTerraformBinaryDownloadUrl(String downloadBaseUrl,
                                                String versionNumber, String binaryFileName) {
        return String.format("%s/%s/%s", downloadBaseUrl, versionNumber, binaryFileName);

    }


    private String getOperatingSystemCode() {
        if (OS_NAME.contains("windows")) {
            return "windows";
        } else if (OS_NAME.contains("linux")) {
            return "linux";
        } else if (OS_NAME.contains("mac")) {
            return "darwin";
        } else if (OS_NAME.contains("freebsd")) {
            return "freebsd";
        } else if (OS_NAME.contains("openbsd")) {
            return "openbsd";
        } else if (OS_NAME.contains("solaris") || OS_NAME.contains("sunos")) {
            return "solaris";
        }
        return "Unsupported OS";
    }
}
