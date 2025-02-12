/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.TerraBootSystemStatus;
import org.eclipse.xpanse.terraform.boot.terraform.service.TerraformDirectoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for admin services of terra-boot. */
@Slf4j
@CrossOrigin
@RestController
@RequestMapping("/terra-boot")
public class TerraBootAdminApi {

    private final TerraformDirectoryService terraformDirectoryService;

    @Autowired
    public TerraBootAdminApi(
            @Qualifier("terraformDirectoryService")
                    TerraformDirectoryService terraformDirectoryService) {
        this.terraformDirectoryService = terraformDirectoryService;
    }

    /**
     * Method to find out the current state of the system.
     *
     * @return Returns the current state of the system.
     */
    @Tag(name = "Admin", description = "Admin services for managing the application.")
    @Operation(description = "Check health of Terra Boot API service")
    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public TerraBootSystemStatus healthCheck() {
        return terraformDirectoryService.tfHealthCheck();
    }
}
