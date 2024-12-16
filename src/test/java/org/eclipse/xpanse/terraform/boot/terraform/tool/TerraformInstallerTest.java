package org.eclipse.xpanse.terraform.boot.terraform.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.Set;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.eclipse.xpanse.terraform.boot.terraform.utils.SystemCmd;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = {
            TerraformInstaller.class,
            TerraformVersionsHelper.class,
            TerraformVersionsCache.class,
            TerraformVersionsFetcher.class,
            SystemCmd.class
        },
        properties = {"support.default.terraform.versions.only=false"})
class TerraformInstallerTest {

    @Value("${terraform.default.supported.versions:1.6.0,1.7.0,1.8.0,1.9.0}")
    private String terraformVersions;

    @Resource private TerraformInstaller installer;
    @Resource private TerraformVersionsHelper versionHelper;
    @Resource private TerraformVersionsCache versionsCache;

    @Test
    void testGetExecutableTerraformByVersion() {
        Set<String> defaultVersions = Set.of(terraformVersions.split(","));
        Set<String> cachedVersions = versionsCache.getAvailableVersions();
        assertTrue(cachedVersions.containsAll(defaultVersions));
        assertTrue(cachedVersions.size() >= defaultVersions.size());

        String requiredVersion = "";
        String terraformPath = installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion);
        assertEquals("terraform", terraformPath);

        String requiredVersion1 = "= 1.6.0";
        String[] operatorAndNumber1 =
                versionHelper.getOperatorAndNumberFromRequiredVersion(requiredVersion1);
        String terraformPath1 =
                installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion1);
        assertTrue(
                versionHelper.checkIfExecutorIsMatchedRequiredVersion(
                        new File(terraformPath1), operatorAndNumber1[0], operatorAndNumber1[1]));

        String requiredVersion2 = "<= v1.5.9";
        String[] operatorAndNumber2 =
                versionHelper.getOperatorAndNumberFromRequiredVersion(requiredVersion2);
        String terraformPath2 =
                installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion2);
        assertTrue(
                versionHelper.checkIfExecutorIsMatchedRequiredVersion(
                        new File(terraformPath2), operatorAndNumber2[0], operatorAndNumber2[1]));

        String requiredVersion3 = ">= v1.9.5";
        String[] operatorAndNumber3 =
                versionHelper.getOperatorAndNumberFromRequiredVersion(requiredVersion3);
        String terraformPath3 =
                installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion3);
        assertTrue(
                versionHelper.checkIfExecutorIsMatchedRequiredVersion(
                        new File(terraformPath3), operatorAndNumber3[0], operatorAndNumber3[1]));

        String requiredVersion4 = ">= 100.0.0";
        assertThrows(
                InvalidTerraformToolException.class,
                () -> installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion4));
    }
}
