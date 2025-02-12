/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Main entry class to terra-boot. This class can be directly executed to start the server. */
@EnableRetry
@EnableAsync
@EnableCaching
@EnableScheduling
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class TerraBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(TerraBootApplication.class, args);
    }
}
