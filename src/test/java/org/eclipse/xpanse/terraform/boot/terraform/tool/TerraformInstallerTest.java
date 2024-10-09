package org.eclipse.xpanse.terraform.boot.terraform.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.annotation.Resource;
import java.io.File;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class TerraformInstallerTest {

    @Resource
    private TerraformInstaller installer;
    @Resource
    private TerraformVersionsHelper versionHelper;

    @Test
    void testGetExecutableTerraformByVersion() {

        String requiredVersion = "";
        String terraformPath = installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion);
        assertEquals("terraform", terraformPath);

        String requiredVersion1 = "= 1.6.0";
        String[] operatorAndNumber1 = versionHelper.getOperatorAndNumberFromRequiredVersion(
                requiredVersion1);
        String terraformPath1 = installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion1);
        assertTrue(versionHelper.checkIfExecutorVersionIsValid(new File(terraformPath1),
                operatorAndNumber1[0], operatorAndNumber1[1]));

        String requiredVersion2 = "<= v1.5.9";
        String[] operatorAndNumber2 = versionHelper.getOperatorAndNumberFromRequiredVersion(
                requiredVersion2);
        String terraformPath2 = installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion2);
        assertTrue(versionHelper.checkIfExecutorVersionIsValid(new File(terraformPath2),
                operatorAndNumber2[0], operatorAndNumber2[1]));

        String requiredVersion3 = ">= v1.9.5";
        String[] operatorAndNumber3 = versionHelper.getOperatorAndNumberFromRequiredVersion(
                requiredVersion3);
        String terraformPath3 = installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion3);
        assertTrue(versionHelper.checkIfExecutorVersionIsValid(new File(terraformPath3),
                operatorAndNumber3[0], operatorAndNumber3[1]));

        String requiredVersion4 = ">= 100.0.0";
        assertThrows(InvalidTerraformToolException.class, () ->
                installer.getExecutorPathThatMatchesRequiredVersion(requiredVersion4));
    }
}