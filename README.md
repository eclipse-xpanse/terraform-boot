<p align='center'>
<a href="https://github.com/eclipse-xpanse/terraform-boot/actions/workflows/ci.yml" target="_blank">
    <img src="https://github.com/eclipse-xpanse/terraform-boot/actions/workflows/ci.yml/badge.svg" alt="build">
</a>
<a href="https://opensource.org/licenses/Apache-2.0" target="_blank">
    <img src="https://img.shields.io/badge/License-Apache_2.0-blue.svg" alt="coverage">
  </a>
</p>
# terraform-boot

A spring-boot-based project which aims to provide a RESTful API for Terraform CLI.

## Application

Server can be compiled and started as below

```shell
./mvmw clean install -DskipTests
java -jar target/terraform-boot-*.jar
```

API can be accessed using the following URLs

```html
http://localhost:9090
http://localhost:9090/swagger-ui/index.html
```

## Supported API Methods

* deploy - This wraps the Terraform `plan` and `apply` methods.
* destroy - This wraps the Terraform `destroy` method.
* validate - This wraps the Terraform `validate` method.

## Terraform Root module Folder

The `terraform-boot` application must have access to the root module folder under which each sub-folder is a module
used per API request.

The default root folder where all module sub-folders will exist is the **temp** folder of the user running the server.

This configuration can be changed by updating the `terraform.root.module.directory` property.

# Available Configurations

The below property names can be changed in the following ways

1. passing the property values to the server startup command as ``--${property-name}=${property-value}``
2. Setting corresponding environment variables before starting the server.

| property name                   | environment variable            | default value                                    | description                                                                                                           |
|---------------------------------|---------------------------------|--------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| terraform_binary_path           | TERRAFORM_BINARY_PATH           | Terraform available on syspath                   | The path to the terraform binary                                                                                      |
| terraform.root.module.directory | TERRAFORM.ROOT.MODULE.DIRECTORY | /tmp on Linux<br/>\AppData\Local\Temp on Windows | The path to the parent directory where all terraform module directories will be stored at as subdirs                  |
| log.terraform.stdout.stderr     | LOG_TERRAFORM_STDOUT_STDERR     | true                                             | Controls if the command execution output must be logged. If disabled, the output is only returned in the API response |
| terraform.log.level             | TERRAFORM_LOG_LEVEL             | INFO                                             | Controls the log level of the terraform binary. Allowed values are INFO, DEBUG, TRACE, WARN and ERROR                 |                                                                                                                       |