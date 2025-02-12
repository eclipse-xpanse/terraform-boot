/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: Huawei Inc.
 */

package org.eclipse.xpanse.terraform.boot.security.oauth2.config;

import static org.eclipse.xpanse.terraform.boot.security.oauth2.config.Oauth2Constants.OPENID_SCOPE;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Configuration springdoc security OAuth2. */
@Profile("oauth")
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Terra-Boot API",
                        description = "RESTful Services to interact with terraform CLI",
                        version = "${app.version}"),
        security =
                @SecurityRequirement(
                        name = "OAuth2Flow",
                        scopes = {OPENID_SCOPE}))
@SecurityScheme(
        name = "OAuth2Flow",
        type = SecuritySchemeType.OAUTH2,
        flows =
                @OAuthFlows(
                        authorizationCode =
                                @OAuthFlow(
                                        authorizationUrl =
                                                "${springdoc.oAuthFlow.authorizationUrl}",
                                        tokenUrl = "${springdoc.oAuthFlow.tokenUrl}",
                                        scopes = {
                                            @OAuthScope(
                                                    name = OPENID_SCOPE,
                                                    description = "mandatory must be selected.")
                                        })))
@Configuration
public class Oauth2OpenApiConfig {}
