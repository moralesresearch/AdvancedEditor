# JTextEditor

A Java-based Text Editor

Version **2.0** · © 2024–2026 Morales Research Technology Inc.

## Features

- Familiar **File / Edit / Format / View / Tools / Help** menus
- Native **macOS screen menu bar** (in-window menu bar on Windows/Linux)
- Find / Replace (regex, match-case, whole-word), Go To Line, Insert Date/Time
- Word wrap, line numbers, zoom, and a live status bar (Ln/Col, length, zoom)
- **Font picker** and unsaved-changes prompts with a Nimbus-themed glowing button
- **Syntax highlighting** (currently Java), auto-detected by file type
- **Tools → Compile and Run Java** (⌘R / Ctrl-R) — compiles the buffer and
  streams output to a console window
- Extensible via the `TextEditorExtension` interface + `ExtensionManager`

## Requirements

- A JDK/JRE **17 or newer** (the released JAR targets Java 17).

## Run the released build

Download `JTextEditor-2.0.jar` from the
[Releases](../../releases) page, then:

```sh
java -jar JTextEditor-2.0.jar            # empty document
java -jar JTextEditor-2.0.jar Demo.java  # open a file at startup
```

## Build from source

No build tool required — just a JDK:

```sh
./build.sh 2.0
java -jar dist/JTextEditor-2.0.jar
```

`build.sh` compiles `src/`, bundles the vendored
[RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) dependency from
`lib/`, and produces a single self-contained runnable JAR in `dist/`.

To compile without packaging (e.g. for development in an IDE):

```sh
javac -cp lib/rsyntaxtextarea-3.3.4.jar -d bin src/*.java
java  -cp "bin:lib/rsyntaxtextarea-3.3.4.jar" TextEditor
```

## Project layout

| Path | Purpose |
|------|---------|
| `src/` | Java sources (one class per responsibility) |
| `lib/` | Vendored dependencies |
| `samples/` | Example files (e.g. `Demo.java` for the Compile-and-Run tool) |
| `build.sh` | Toolless build script → `dist/JTextEditor-<version>.jar` |
| `.github/workflows/` | CI (build on push/PR) and Release (on `v*` tag) |

## Releasing

CI builds and validates the JAR on every push and pull request. To cut a
release, push a tag:

```sh
git tag v2.0
git push origin v2.0
```

The **Release** workflow builds `JTextEditor-2.0.jar` and publishes it to a new
GitHub Release with auto-generated notes. (It can also be triggered manually
from the Actions tab via *workflow_dispatch*.)

## License / credits

Copyright © 2024–2026 Morales Research Technology Inc.
Portions © 1992–2008 Sun Microsystems Inc.
Built on RSyntaxTextArea (BSD-3-Clause).
