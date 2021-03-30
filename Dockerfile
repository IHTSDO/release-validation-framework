FROM maven:3.3-jdk-8 AS builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean install -DskipTests=true 

FROM openjdk:8-jdk-alpine
LABEL maintainer="SNOMED International <tooling@snomed.org>"

ARG SUID=1042
ARG SGID=1042

VOLUME /tmp

RUN apk update 
RUN apk add git

# Create a working directory
RUN mkdir /app
WORKDIR /app

# Add in the necessary config files to be able to run the rvf
RUN mkdir /config
WORKDIR /app/config
ADD config/data-service.properties data-service.properties 

WORKDIR /app/config
ADD config/execution-service.properties execution-service.properties

WORKDIR /app

# Clone in the drools rules needed
RUN git clone https://github.com/IHTSDO/snomed-drools-rules.git

RUN mkdir /app/store
RUN mkdir /app/store/releases

# Copy necessary files
COPY --from=builder /usr/src/app/api/target/api*.jar api.jar

# Create the rvf user
RUN addgroup -g $SGID rvf && \
    adduser -D -u $SUID -G rvf rvf

# Change permissions.
RUN chown -R rvf:rvf /app

# Run as the rvf user.
USER rvf

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","api.jar"]
