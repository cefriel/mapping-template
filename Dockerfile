FROM eclipse-temurin:11

ARG MAPPING_TEMPLATE_VERSION=2.5.2-SNAPSHOT

# Copy the built JAR from the previous stage
RUN mkdir mapping-template

COPY ./target/mapping-template-${MAPPING_TEMPLATE_VERSION}.jar /mapping-template/mapping-template.jar

RUN mkdir /data

# Silent
CMD ["tail", "-f", "/dev/null"]
