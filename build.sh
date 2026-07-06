#!/usr/bin/env bash
#
# Builds a self-contained, runnable JAR for JTextEditor.
#
# Usage:  ./build.sh [version]
# Output: dist/JTextEditor-<version>.jar   (run with: java -jar <that file>)
#
# No build tool required — just a JDK (17+). The RSyntaxTextArea dependency
# vendored in lib/ is unpacked into the JAR so the result is a single file
# that runs anywhere with a JRE.
set -euo pipefail

VERSION="${1:-2.0}"
ROOT="$(cd "$(dirname "$0")" && pwd)"
LIB="$ROOT/lib/rsyntaxtextarea-3.3.4.jar"
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
DIST="$ROOT/dist"
JAR="$DIST/JTextEditor-$VERSION.jar"
MAIN_CLASS="TextEditor"

echo "==> Cleaning"
rm -rf "$BUILD" "$DIST"
mkdir -p "$CLASSES" "$DIST"

echo "==> Compiling sources (release 17)"
javac -encoding UTF-8 --release 17 -cp "$LIB" -d "$CLASSES" "$ROOT"/src/*.java

echo "==> Bundling dependency: $(basename "$LIB")"
( cd "$CLASSES" && jar xf "$LIB" )
# Replace the dependency's manifest with our own (added by 'jar --create' below),
# but keep its LICENSE alongside ours.
rm -f "$CLASSES/META-INF/MANIFEST.MF"

echo "==> Packaging $JAR"
jar --create --file "$JAR" \
    --main-class "$MAIN_CLASS" \
    -C "$CLASSES" .

echo "==> Built: $JAR"
