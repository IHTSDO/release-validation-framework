FROM openjdk:8-jdk-alpine
LABEL maintainer="SNOMED International <tooling@snomed.org>"

ARG SUID=1042
ARG SGID=1042

VOLUME /tmp

# Create a working directory
RUN mkdir /app
WORKDIR /app

RUN mkdir /config
WORKDIR /app/config
ADD config/data-service.properties data-service.properties 

WORKDIR /app

# Copy necessary files
ADD api/target/api*.jar api.jar

# Create the snowstorm user
RUN addgroup -g $SGID rvf && \
    adduser -D -u $SUID -G rvf rvf

# Change permissions.
RUN chown -R rvf:rvf /app

# Run as the snowstorm user.
USER rvf

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","api.jar"]
