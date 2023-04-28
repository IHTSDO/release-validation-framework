#!/bin/sh

# This script stops the RVF containers for the M1 processor.

set -e

docker-compose -f docker-compose.yml -f docker-compose.m1.yml down
