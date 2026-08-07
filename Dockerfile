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

# Drools rules. NOTE: this pin does not fix Drools, it makes the current state
# reproducible. These rules call isInactiveConceptSameAs and
# isSemanticTagCompatibleWithinHierarchy, which do not exist in snomed-drools
# 4.0.0 - the version snomed-parent-bom 2.0.0 pins in pom.xml. Rule compilation
# therefore fails, Drools contributes nothing, and the build still reports
# success. That is true today with the clone unpinned, and stays true here.
#
# PR #13 bumps snomed-drools to 5.7.0. Once it lands, this pin and that version
# become a matched, verified pair. Until then, treat Drools as not running.
ARG DROOLS_RULES_REF=55795d5d19b1db99d2f5757e6aa397014aaaf268
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
