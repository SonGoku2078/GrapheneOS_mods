#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
sdk_root=${ANDROID_SDK_ROOT:-${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK directory}}
tools="$sdk_root/build-tools/36.0.0"
platform="$sdk_root/platforms/android-36/android.jar"
out="$(pwd)/build"
mkdir -p "$out"
work="$(mktemp -d "$out/compile.XXXXXX")"
trap 'rm -rf -- "$work"' EXIT
mkdir "$work/classes" "$work/dex"

javac --release 11 -classpath "$platform" -encoding UTF-8 -d "$work/classes" \
    src/io/github/graphenelauncherplus/mini/HomeActivity.java
jar cf "$work/classes.jar" -C "$work/classes" .
"$tools/d8" --min-api 31 --lib "$platform" --output "$work/dex" "$work/classes.jar"
"$tools/aapt2" link -I "$platform" --manifest AndroidManifest.xml -o "$work/unsigned.apk"
(cd "$work/dex" && zip -q "$work/unsigned.apk" classes.dex)
"$tools/zipalign" -f 4 "$work/unsigned.apk" "$work/aligned.apk"

keystore="${MINI_KEYSTORE:-$out/mini-debug.jks}"
if [[ ! -f "$keystore" ]]; then
    if [[ -n "${MINI_KEYSTORE:-}" ]]; then
        echo 'The supplied signing keystore does not exist.' >&2
        exit 1
    fi
    keytool -genkeypair -keystore "$keystore" -storepass android -keypass android \
        -alias launcher-mini -keyalg RSA -keysize 2048 -validity 10000 \
        -dname 'CN=Launcher Plus Mini Test'
fi
MINI_STORE_PASSWORD="${MINI_STORE_PASSWORD:-android}" \
    "$tools/apksigner" sign --ks "$keystore" --ks-pass env:MINI_STORE_PASSWORD \
    --out "$out/LauncherPlusMini.apk" "$work/aligned.apk"
"$tools/apksigner" verify "$out/LauncherPlusMini.apk"
