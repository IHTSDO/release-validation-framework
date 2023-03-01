#!/bin/sh

# This script starts the RVF containers for the M1 processor.

set -e

if [ ! -d "jobs" ]; then
  mkdir jobs
fi

./stop-rvf-containers-m1.sh
docker-compose -f docker-compose.yml -f docker-compose.m1.yml up -d
