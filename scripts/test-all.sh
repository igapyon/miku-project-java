#!/bin/sh

set -eu

SCRIPT_DIRECTORY=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROJECT_ROOT=$(CDPATH= cd -- "${SCRIPT_DIRECTORY}/.." && pwd)

cd "${PROJECT_ROOT}"
node --test scripts/import-miku-project-contract-snapshot.test.mjs
mvn -B test
mvn -B test -Dtest=ContractSnapshotVerifierTest
