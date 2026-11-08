# Packaging LastStandinNote

This document is for maintainers. Player information remains in the root README.

## Version and outputs

`resources/version.txt` is the single version source. It currently contains `1.0.0`.
The game bytecode targets Java 17; portable packages include Eclipse Temurin Java 21.
Builds use a full x64 Eclipse Temurin 21.0.13+9 JDK and Python 3.9 or newer.
The JDK is pinned so the bundled runtime and its distributed source archive match.

```text
python tools/package_game.py
```

The native build uses `jpackage --type app-image` on Windows or Linux. It generates
a portable archive in `dist/`, a matching project source archive, build metadata
and SHA-256 checksums. It does not install the application, create a tag or publish
a GitHub Release. Keep `dist/` and `build/` outside version control.

On Windows, extract the entire ZIP and open `LastStandinNote.exe`.
On Linux, extract the entire tarball and run `Start.sh` or `bin/LastStandinNote`.
The complete runtime folder must stay beside the game. Linux requires a graphical
desktop and its usual native desktop libraries. Verify real sound on the target PC.

## Application icons

The window loads its seven dedicated PNG sizes from `resources/images/icon/`.
Missing window icons are logged without preventing startup. Native builds and
Windows cross-builds use the supplied `packaging/icon.ico` on Windows and
`packaging/icon.png` on Linux, without regenerating them from sprites.
`packaging/icon.icns` is also supplied for a future macOS packaging workflow;
the current build script supports Windows and Linux x64.
Keep `packaging/` in version control and in source archives. Its icons are
adaptations of the project's Tux artwork under CC BY-SA 4.0; see `NOTICE.md`.

## Windows cross-build from Linux

This route builds the small GPLv3 Windows GUI launcher in `tools/windows_launcher.c`
with the x64 MinGW-w64 C compiler and `windres`. It bundles the official Windows
Temurin JRE pinned in `tools/windows-runtime.json`, checking its SHA-256 first.
The launcher uses only Windows system DLLs and starts the bundled `javaw.exe`.
It handles paths containing spaces and Unicode without using the system Java.

```text
python tools/package_game.py --windows-portable
```

An already downloaded runtime can be passed with `--runtime-zip PATH`.
`Debug.cmd` opens the bundled console Java for troubleshooting this portable build.
This route produces a real Windows x64 EXE, but Linux cannot validate its native
Windows execution. Test the extracted archive on Windows before publishing it.

## GitHub Actions

The manual **Build game packages** workflow uses native Windows and Linux runners.
After committing the packaging files, run it through **Actions > Build game
packages > Run workflow**. Download the corresponding artifacts when it finishes.
It does not publish a release automatically. Its Windows route uses `jpackage`.

## Verification and corresponding sources

Every build validates all charts before compilation, then loads charts, recorded
vocals, menu audio, sprites, images, credits and licenses from the actual JAR.
The JAR contains its resources, so it does not depend on a separate `resources/`
directory or IntelliJ's output folder.

The source ZIP contains the exact working game sources, resources, launchers,
build scripts and notices used for that build. Metadata names the base commit
and lists uncommitted changes rather than attributing them to that commit.

Preserve `LICENSE` for the game's GPLv3 code, `LICENSE-ASSETS` for original
CC BY-SA artwork/vocals/charts, and individual third-party credits in `CREDITS.txt`.
All Java runtime legal notices remain in the bundled runtime. Include the matching
Temurin source archive linked and hashed in `tools/windows-runtime.json` alongside
the binary downloads when publishing. The workflow downloads this source archive
automatically. Keep the project-source archive and runtime-source archive with
the binaries; they cover different components.

For a standalone executable JAR, use `--jar-only`. It requires Java 17 or later
on the player's machine and is not the recommended download for ordinary players.
