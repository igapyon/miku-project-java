#!/bin/sh
set -eu

verify_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
verify_first=$(mktemp -d)
verify_second=$(mktemp -d)
cleanup() {
  rm -rf "$verify_first" "$verify_second"
}
trap cleanup EXIT HUP INT TERM

cd "$verify_root"
mvn -B clean package
cp target/mikuproject.jar target/mikuproject-sources.jar target/mikuproject-dist.zip "$verify_first"
mvn -B clean package
cp target/mikuproject.jar target/mikuproject-sources.jar target/mikuproject-dist.zip "$verify_second"

for verify_file in mikuproject.jar mikuproject-sources.jar mikuproject-dist.zip; do
  cmp "$verify_first/$verify_file" "$verify_second/$verify_file"
done
