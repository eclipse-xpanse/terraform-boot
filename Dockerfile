FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S terraform-boot && adduser -S -G terraform-boot terraform-boot
RUN apk update && \
    apk add --no-cache unzip wget
ENV TERRAFORM_VERSION=1.4.4
RUN wget https://releases.hashicorp.com/terraform/${TERRAFORM_VERSION}/terraform_${TERRAFORM_VERSION}_linux_amd64.zip
RUN unzip terraform_${TERRAFORM_VERSION}_linux_amd64.zip
RUN mv terraform /usr/bin/terraform
COPY target/terraform-boot-*.jar terraform-boot.jar
USER terraform-boot
ENTRYPOINT ["java","-jar","terraform-boot.jar"]