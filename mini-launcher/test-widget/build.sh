#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:?}}
tools="$sdk_root/build-tools/36.0.0"
out="$(realpath ../build)"
work="$(mktemp -d "$out/fixture.XXXXXX")"
trap 'rm -rf -- "$work"' EXIT
"$tools/aapt2" compile --dir res -o "$work/res.zip"
"$tools/aapt2" link -I "$sdk_root/platforms/android-36/android.jar" \
    --manifest AndroidManifest.xml "$work/res.zip" -o "$work/unsigned.apk"
"$tools/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"
"$tools/apksigner" sign --ks "$out/mini-debug.jks" --ks-pass pass:android \
    --out "$out/test-widget.apk" "$work/aligned.apk"
