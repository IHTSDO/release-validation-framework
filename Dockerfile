FROM maven:3.6.3-openjdk-17 AS builder
COPY . /usr/src/app
WORKDIR /usr/src/app
RUN mvn clean install -DskipTests=true

FROM aehrc/jre:openjdk-17
LABEL maintainer="SNOMED International <tooling@snomed.org>"

ARG SUID=1042
ARG SGID=1042

VOLUME /tmp

RUN apk update
RUN apk add git

# Create a working directory
RUN mkdir /app
WORKDIR /app

# Clone in the drools rules needed
RUN git clone https://github.com/IHTSDO/snomed-drools-rules.git

# Clone validation assertions
RUN git clone https://github.com/IHTSDO/snomed-release-validation-assertions.git

RUN mkdir /app/store
RUN mkdir /app/store/releases

# Copy necessary files
COPY --from=builder /usr/src/app/target/release-validation-framework*.jar rvf-api.jar

# Create the rvf user
RUN addgroup -g $SGID rvf && \
    adduser -D -u $SUID -G rvf rvf

# Change permissions.
RUN chown -R rvf:rvf /app

# Run as the rvf user.
USER rvf

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","rvf-api.jar"]
