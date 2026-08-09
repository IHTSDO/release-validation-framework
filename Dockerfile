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

# Both clones are PINNED. Unpinned, the image is not reproducible: what you get
# depends on when it was built, so an image that works can stop working with no
# change to this repository. That is not hypothetical - it is what happened on
# 2026-08-07. This image had not been rebuilt since 2024-05-24; the first
# rebuild picked up two years of upstream drift and RVF failed to start with
#
#   FileNotFoundException: .../release-type-delta-previous-snapshot-validation-
#                              inferred-relationship_EDITION.sql
#
# because IHTSDO removed the _EDITION variants in 9cea111 (2024-08-19) while our
# mounted manifest.xml still references four of them.
#
# When bumping either pin, check it against the OTHER side of the pairing:
#   - assertions must satisfy every sqlFile in the mounted manifest.xml
#   - rules must not call service methods absent from ${snomed-drools.version}

# Drools rules, pinned as a matched pair with snomed-drools 5.7.0 in pom.xml.
# VERIFIED: at this commit the rules compile against 5.7.0 with zero errors.
#
# It is the LAST commit before the rules started using the domain type
# org.ihtsdo.drools.domain.Annotation (b64999b, 2026-04-09), which exists only
# from snomed-drools 6.0.0. Moving this pin forward past b64999b without also
# moving snomed-drools forward does not fail loudly - DRL resolves Annotation to
# the unrelated org.kie.api.definition.type.Annotation and reports errors about
# methods nobody wrote:
#
#   unable to resolve method: org.kie.api.definition.type.Annotation.active()
#
# and Drools then silently contributes nothing while the build goes green.
#
# Going to 6.0.0 is not currently possible: it is compiled for Java 25 (class
# file version 69) and this image runs 17. That needs upstream's PIP-1048
# amazoncorretto:17->25 bump, which is part of the IHTSDO catch-up. Until then
# these rules are deliberately four months behind current develop.
ARG DROOLS_RULES_REF=45e0d9e21f8cd8a15a89c4b417f413b108496278
RUN git clone https://github.com/IHTSDO/snomed-drools-rules.git \
    && git -C snomed-drools-rules checkout --quiet ${DROOLS_RULES_REF}

# Validation assertions. Pinned to the repository state as at 2024-05-23, which
# is what the last known-good image (build 4556, 2024-05-24) was built from.
# Verified: all four _EDITION scripts referenced by the mounted manifest.xml
# resolve at this commit.
ARG ASSERTIONS_REF=fad36466277ca633e0bc6844a3b4a83d3698ea97
RUN git clone https://github.com/IHTSDO/snomed-release-validation-assertions.git \
    && git -C snomed-release-validation-assertions checkout --quiet ${ASSERTIONS_REF}

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
