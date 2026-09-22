#!/usr/bin/env bash
# First-clone bootstrap. Gson is used only by Java lifecycle/status tools.
set -Eeuo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.."
version=2.13.2
expected=dd0ce1b55a3ed2080cb70f9c655850cda86c206862310009dcb5e5c95265a5e0
file=runtime/host-lib/gson.jar
[[ ! -L runtime && ! -L runtime/host-lib && ! -L $file ]] || { echo 'Refusing linked host library.' >&2; exit 1; }
mkdir -p runtime/host-lib
verify() { [[ $(sha256sum -- "$1" | awk '{print $1}') == "$expected" ]]; }
if [[ -f $file ]]; then
  verify "$file" || { echo 'Gson integrity mismatch; preserve the file and investigate.' >&2; exit 1; }
  exit 0
fi
tmp=$(mktemp runtime/host-lib/gson.download.XXXXXX)
trap 'rm -f -- "$tmp"' EXIT
curl --fail --location --silent --show-error --retry 3 --connect-timeout 20 --max-time 300 \
  --proto '=https' --proto-redir '=https' --tlsv1.2 \
  "https://repo.maven.apache.org/maven2/com/google/code/gson/gson/$version/gson-$version.jar" -o "$tmp"
verify "$tmp" || { echo 'Downloaded Gson did not match its pinned checksum.' >&2; exit 1; }
mv -- "$tmp" "$file"
