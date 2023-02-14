#!/bin/sh

# This script builds the RVF images for the M1 processor.

set -e

VERSION=1.0.0

export DOCKER_BUILDKIT=1

# Build the RVF images
docker build --platform linux/amd64 -t rvf-m1:${VERSION} -f Dockerfile.m1 .

# Tag the images
docker tag rvf-m1:${VERSION} rvf-m1:latest
docker tag rvf-m1:${VERSION} rvf:latest

# Push the images
#docker push rvf-m1:${VERSION}
#docker push rvf-m1:latest
