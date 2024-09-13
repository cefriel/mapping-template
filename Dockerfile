FROM eclipse-temurin:11

# Copy the built JAR from the previous stage
RUN mkdir mapping-template

COPY ./target/mapping-template-?.?.?-SNAPSHOT.jar /mapping-template/mapping-template.jar
# If there is a release JAR, overwrite the JAR
COPY ./target/mapping-template-?.?.?.jar /mapping-template/mapping-template.jar

RUN mkdir /data

# Silent
CMD ["tail", "-f", "/dev/null"]
