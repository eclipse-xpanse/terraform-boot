/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.tool;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Objects;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Bean to update the cache of versions of Terraform.
 */
@Slf4j
@Component
public class TerraformVersionsCacheManager {

    @Resource
    private TerraformVersionsCache versionsCache;

    @Resource
    private TerraformVersionsFetcher versionsFetcher;

    /**
     * Initialize the cache of available versions of Terraform.
     */
    @PostConstruct
    public void initializeCache() {
        Set<String> versions = versionsCache.getAvailableVersions();
        log.info("Initialized Terraform versions cache with versions:{}.", versions);
    }

    /**
     * Update the cache with the versions fetched from the Terraform website.
     * This method is scheduled run once a day.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void fetchVersionsFromWebsiteAndLoadCache() {
        try {
            Set<String> availableVersionsFromWebsite =
                    versionsFetcher.fetchAvailableVersionsFromTerraformWebsite();
            versionsCache.updateCachedVersions(availableVersionsFromWebsite);
            log.info("Updated the cache with versions:{} fetched from Terraform website.",
                    availableVersionsFromWebsite);
        } catch (Exception e) {
            log.error("Failed to update the cache with versions fetched from Terraform website.",
                    e);
        }
    }

    /**
     * Update the cache when the default versions cached.
     * This method is scheduled to run every one hour.
     * But does nothing if the cache already contains versions other than the default version list.
     */
    @Scheduled(cron = "0 1 * * * ?")
    public void fetchVersionsFromWebsiteAndLoadCacheIfCacheHasOnlyDefaultVersions() {
        Set<String> cachedVersions = versionsCache.getAvailableVersions();
        Set<String> defaultVersions = versionsFetcher.getDefaultVersionsFromConfig();
        if (!CollectionUtils.isEmpty(defaultVersions)
                && Objects.equals(cachedVersions, defaultVersions)) {
            fetchVersionsFromWebsiteAndLoadCache();
        }
    }
}
