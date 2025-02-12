<p align='center'>
<a href="https://github.com/eclipse-xpanse/terra-boot/actions/workflows/ci.yml" target="_blank">
	<img src="https://github.com/eclipse-xpanse/terra-boot/actions/workflows/ci.yml/badge.svg" alt="build">
</a>
<a href="https://opensource.org/licenses/Apache-2.0" target="_blank">
	<img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="coverage">
</a>
</p>

# terra-boot

A spring-boot-based project which aims to provide a RESTful API for Terraform CLI. It provides three different modes of
execution

1. Scripts in a directory
2. Scripts in REST request body
3. Scripts in a GIT repo

## Modes of Terraform Script Execution

### Scripts in a Directory

Pass the terraform root folder name in the request and terra-boot will execute the requested Terraform method on
this directory.

#### Terraform Root module Folder

If we intend to use this mode of script execution, then we must ensure the scripts root directory is
accessible to terra-boot runtime.

The `terra-boot` application must have access to the root module folder under which each sub-folder is a module
used per API request.

The default root folder where all module sub-folders will exist is the **temp** folder of the user running the server.

This configuration can be changed by updating the `terraform.root.module.directory` property.

### Scripts in the Request Body

All files needed for terraform execution can be passed as strings to API and terra-boot will automatically execute
the files and return terraform execution result.

### Scripts in GIT Repo

If the scripts are present in a GIT repo, then we can directly pass the details of the GIT repo. Terra-boot will
clone the repo and execute the scripts and then return the result.

> [!NOTE]
> Currently supports only repos that can be cloned without authentication and also with HTTP(S) only.


## Supported API Methods

* deploy - This wraps the Terraform `plan` and `apply` methods.
* destroy - This wraps the Terraform `destroy` method.
* validate - This wraps the Terraform `validate` method.
* plan - This wraps the Terraform 'plan' method.
* healthCheck - This method returns the status of the terra-boot application

> [!NOTE]
> All terraform related methods above support both modes of operation mentioned in
> section [Modes of Terraform Script Execution](#modes-of-terraform-script-execution)

## Supported Authentication Methods

1. None - By default the API methods can be accessed without any authentication.
2. oauth - This can be enabled by activating spring profile - `oauth`

## Available Configurations

The below property names can be changed in the following ways

1. passing the property values to the server startup command as ``--${property-name}=${property-value}``
2. Setting corresponding environment variables before starting the server.

| property name                                | environment variable                      | default value                                    | description                                                                                                                                   |
|----------------------------------------------|-------------------------------------------|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| terraform_binary_path                        | TERRAFORM_BINARY_PATH                     | Terraform available on syspath                   | The path to the terraform binary                                                                                                              |
| terraform.root.module.directory              | TERRAFORM_ROOT_MODULE_DIRECTORY           | /tmp on Linux<br/>\AppData\Local\Temp on Windows | The path to the parent directory where all terraform module directories will be stored at as subdirs                                          |
| log.terraform.stdout.stderr                  | LOG_TERRAFORM_STDOUT_STDERR               | true                                             | Controls if the command execution output must be logged. If disabled, the output is only returned in the API response                         |
| terraform.log.level                          | TERRAFORM_LOG_LEVEL                       | INFO                                             | Controls the log level of the terraform binary. Allowed values are INFO, DEBUG, TRACE, WARN and ERROR                                         |
| authorization.token.type                     | AUTHORIZATION_TOKEN_TYPE                  | JWT                                              | Authorization server authentication Type, allowed values: OpaqueToken or JWT                                                                  |
| authorization.server.endpoint                | AUTHORIZATION_SERVER_ENDPOINT             |                                                  | The endpoint value of the authorization server                                                                                                |
| authorization.api.client.id                  | AUTHORIZATION_API_CLIENT_ID               |                                                  | The ID value of the authorization server API client                                                                                           |
| authorization.api.client.secret              | AUTHORIZATION_API_CLIENT_SECRET           |                                                  | The secret value of the authorization server API client                                                                                       |
| authorization.swagger.ui.client.id           | AUTHORIZATION_SWAGGER_UI_CLIENT_ID        |                                                  | The ID value of the authorization server swagger-ui client                                                                                    |
| otel.exporter.otlp.endpoint                  | OTEL_EXPORTER_OTLP_ENDPOINT               | http://localhost:4317                            | URL of the OTEL collector                                                                                                                     |
| clean.workspace.after.deployment.enabled     | CLEAN_WORKSPACE_AFTER_DEPLOYMENT_ENABLED  | true                                             | Whether to clean up the workspace after deployment is done,allowed values: true or false. Default value is true                               |
| terraboot.webhook.request.signing.enabled    | TERRABOOT_WEBHOOK_REQUEST_SIGNING_ENABLED | true                                             | Whether to sign webhook requests initiated from terra-boot. The values must be agreed with the consumer application. Default value is true    |



## Run Application

### Local Development

Server can be compiled and started as below

If you have a fully configured OAuth instance running on your local system, then you can use the below way to
start the application. The variables and their descriptions that need to be passed during oauth startup are recorded in
the Available Configurations table.

### dev profile

Use the spring boot's dev profile to use default values that are just for non-production environments.

#### From Command Line

1.Start with oauth

```shell
./mvmw clean install -DskipTests
$ java -jar target/terra-boot-*.jar\
--spring.profiles.active=oauth \
--authorization.token.type=${token-type} \
--authorization.server.endpoint=${server-endpoint} \
--authorization.api.client.id=${client-id} \
--authorization.api.client.secret=${client-secret} \
--authorization.swagger.ui.client.id=${swagger-ui-cleint-id}
```

2.Start without oauth

```shell
./mvmw clean install -DskipTests
$ java -jar target/terra-boot-*.jar
```

#### From IDE

1.Start with oauth

	1.Set values for the authorization related variables in the application-oauth-properties configuration file,
	and specify the configuration file for loading oauth in the application-properties configuration file,
	start the main application.
	2.Or the oauth related variables configuration can be added to IDE and the main application can be executed directly
	to launch the application.

2.Start without oauth

	Simply start the main application.

API can be accessed using the following URLs

```html
http://localhost:9090
http://localhost:9090/swagger-ui/index.html
```

### Production

#### Docker Image

This is the recommended way. Docker images can be pulled from GitHub Packages as
mentioned [here](https://github.com/eclipse-xpanse/terra-boot/pkgs/container/terra-boot).
All configuration parameters can be passed as environment variables to the container.

#### Run Jar

We also deliver a jar for each release and can be found in the asests list of each
release [here](https://github.com/eclipse-xpanse/terra-boot/releases).
The jar can be started as mentioned in the same way we do for local development.

### Observability

`terra-boot` provides an option to enable observability using [openTelemetry](https://opentelemetry.io/). This can
be enabled by starting the runtime with profile `opentelemetry`.

By enabling this profile, the application forwards metrics, traces and logs to the `otel-collector`.

By default, the application sends all telemetry data to `http://localhost:4317` and this can be changed by updating the
value of `otel.exporter.otlp.endpoint` configuration property.

## Dependencies File

All third-party related content is listed in the [DEPENDENCIES](DEPENDENCIES) file.

## Code Formatter

The project follows [google-code-format](https://github.com/google/google-java-format).
We use the [spotless plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven#google-java-format) to format code and to validate code format.
We can automatically format the code using the command below.

```shell
mvn spotless:apply
```

To validate errors we can run the command below.

```shell
mvn spotless:check &&  mvn checkstyle:check
```
