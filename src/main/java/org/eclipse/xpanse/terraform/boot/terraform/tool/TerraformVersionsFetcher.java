/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.terraform.tool;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.xpanse.terraform.boot.models.exceptions.InvalidTerraformToolException;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTag;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.GitHubRateLimitHandler;
import org.kohsuke.github.PagedIterable;
import org.kohsuke.github.connector.GitHubConnectorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

/**
 * Class to get available versions of Terraform.
 */
@Slf4j
@Component
public class TerraformVersionsFetcher {

    private static final Pattern OFFICIAL_VERSION_PATTERN =
            Pattern.compile("^v(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})$");
    @Value("${terraform.github.api.endpoint:https://api.github.com}")
    private String terraformGithubApiEndpoint;
    @Value("${terraform.github.repository:hashicorp/terraform}")
    private String terraformGithubRepository;
    @Value("${terraform.default.supported.versions}")
    private String defaultVersionsString;


    /**
     * Fetch all available versions from Terraform website.
     *
     * @return all available versions from Terraform website.
     */
    @Retryable(retryFor = Exception.class,
            maxAttemptsExpression = "${spring.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${spring.retry.delay-millions}"))
    public Set<String> fetchAvailableVersionsFromTerraformWebsite() throws Exception {
        int retryCount = Objects.isNull(RetrySynchronizationManager.getContext())
                ? 0 : RetrySynchronizationManager.getContext().getRetryCount();
        log.info("Start to fetch available versions from website for Terraform."
                + " Retry count: {}", retryCount);
        Set<String> allVersions = new HashSet<>();
        try {
            if (!isEndpointReachable(terraformGithubApiEndpoint)) {
                String errorMsg = "Terraform website is not reachable.";
                log.error(errorMsg);
                throw new IOException(errorMsg);
            }
            GitHub gitHub = new GitHubBuilder()
                    .withEndpoint(terraformGithubApiEndpoint)
                    .withRateLimitHandler(getGithubRateLimitHandler())
                    .build();
            GHRepository repository = gitHub.getRepository(terraformGithubRepository);
            PagedIterable<GHTag> tags = repository.listTags();
            tags.forEach(tag -> {
                String version = tag.getName();
                if (OFFICIAL_VERSION_PATTERN.matcher(version).matches()) {
                    // remove the prefix 'v'
                    allVersions.add(version.substring(1));
                }
            });
        } catch (Exception e) {
            log.error("Failed to fetch available versions from Terraform website. Retry count: {}.",
                    retryCount, e);
            throw e;
        }
        log.info("Get available versions: {} from Terraform website. Retry count: {}",
                allVersions, retryCount);
        if (allVersions.isEmpty()) {
            String errorMsg = "No available versions found from Terraform website";
            throw new InvalidTerraformToolException(errorMsg);
        }
        return allVersions;
    }

    private boolean isEndpointReachable(String endpoint) throws IOException {
        try {
            URL url = URI.create(endpoint).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setRequestMethod(HttpMethod.HEAD.name());
            return connection.getResponseCode() == HttpStatus.OK.value();
        } catch (IOException e) {
            throw new IOException("Failed to connect to the endpoint: " + endpoint);
        }
    }

    /**
     * Get default versions from config.
     *
     * @return default versions.
     */
    public Set<String> getDefaultVersionsFromConfig() {
        Set<String> defaultVersions =
                Set.of(defaultVersionsString.replaceAll("//s+", "").split(","));
        log.info("Get default versions: {} from Terraform versions config value: {}",
                defaultVersions, defaultVersionsString);
        return defaultVersions;
    }

    private GitHubRateLimitHandler getGithubRateLimitHandler() {
        return new GitHubRateLimitHandler() {
            @Override
            public void onError(@Nonnull GitHubConnectorResponse response) throws IOException {
                String limit = response.header("X-RateLimit-Limit");
                String remaining = response.header("X-RateLimit-Remaining");
                String reset = response.header("X-RateLimit-Reset");
                String errorMsg = String.format("GitHub API rate limit exceeded. "
                        + "Rate limit: %s, remaining: %s, reset time: %s", limit, remaining, reset);
                throw new IOException(errorMsg);
            }
        };
    }

}
