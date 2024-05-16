FROM eclipse-temurin:11

ARG MAPPING_TEMPLATE_VERSION=2.4.1-SNAPSHOT

# Copy the built JAR from the previous stage
RUN mkdir mapping-template

COPY ./target/mapping-template-${MAPPING_TEMPLATE_VERSION}.jar /mapping-template/mapping-template.jar

RUN mkdir /opt/rml
COPY ./rml/rml-compiler.vm /opt/rml/rml-compiler.vm
COPY ./rml/functions/RMLCompilerUtils.java /opt/rml/RMLCompilerUtils.java

RUN mkdir /data

# Silent
CMD ["tail", "-f", "/dev/null"]
