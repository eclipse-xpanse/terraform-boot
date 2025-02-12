FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S terra-boot && adduser -S -G terra-boot terra-boot
RUN apk update && \
    apk add --no-cache unzip wget

ENV TERRAFORM_INSTALL_PATH=/opt/terraform
ENV DEFAULT_TERRAFORM_VERSION=1.6.0
ENV TERRAFORM_VERSIONS=1.6.0,1.7.0,1.8.0,1.9.0
COPY install_terraform.sh /install_terraform.sh
RUN chmod +x /install_terraform.sh
RUN echo "Downloading and installing Terraform with multiple versions $TERRAFORM_VERSIONS into path $TERRAFORM_INSTALL_PATH"; \
    /install_terraform.sh "$TERRAFORM_INSTALL_PATH" "$DEFAULT_TERRAFORM_VERSION" "$TERRAFORM_VERSIONS"

COPY target/terra-boot-*.jar terra-boot.jar
USER terra-boot
ENTRYPOINT ["java", "-Dterraform.install.dir=${TERRAFORM_INSTALL_PATH}", "-Dterraform.default.supported.versions=${TERRAFORM_VERSIONS}", "-jar", "terra-boot.jar"]