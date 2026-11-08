#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Build portable game packages with bundled Java and matching source archives."""

import argparse
import hashlib
import json
import os
import platform
import re
import shutil
import subprocess
import sys
import tarfile
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = "LastStandinNote"
NOTICES = ("LICENSE", "LICENSE-ASSETS", "NOTICE.md", "README.md")


def run(*args, **kwargs):
    return subprocess.run([str(arg) for arg in args], cwd=ROOT, check=True, **kwargs)


def digest(path):
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def java_tool(name):
    suffix = ".exe" if os.name == "nt" else ""
    home = os.environ.get("JAVA_HOME")
    candidate = Path(home) / "bin" / (name + suffix) if home else None
    found = str(candidate) if candidate and candidate.is_file() else shutil.which(name)
    if not found:
        raise ValueError(f"{name} is unavailable. Set JAVA_HOME to a full OpenJDK 21 installation.")
    return found


def source_files():
    for name in (*NOTICES, ".gitignore"):
        yield ROOT / name
    for directory in ("src", "resources", "tools", "docs", "packaging", ".github/workflows"):
        for path in sorted((ROOT / directory).rglob("*")):
            if path.is_file() and "__pycache__" not in path.parts and path.suffix != ".pyc":
                yield path


def source_archive(dist, version):
    path = dist / f"{APP}-{version}-source.zip"
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as archive:
        for source in source_files():
            archive.write(source, (Path(APP) / source.relative_to(ROOT)).as_posix())
    return path


def remove_copy_temporaries(image):
    # remove abandoned partial copies only when their final sibling already exists
    for path in image.rglob(".*"):
        match = re.fullmatch(r"\.(.+)\.[A-Za-z0-9]{6}", path.name)
        if path.is_file() and match and path.with_name(match.group(1)).is_file():
            path.unlink()


def copy_notices(image, version, target):
    for name in NOTICES:
        shutil.copy2(ROOT / name, image / name)
    shutil.copy2(ROOT / "resources/CREDITS.txt", image / "CREDITS.txt")
    launcher = f"{APP}.exe" if target == "Windows" else "Start.sh"
    (image / "START_HERE.txt").write_text(
        f"{APP} {version} - {target} x64\n\n"
        f"Extrae el archivo completo y abre {launcher}.\n"
        "Conserva todas las carpetas juntas. Java está incluido.\n\n"
        f"Extract the entire archive, then run {launcher}.\n"
        "Keep all folders together. Java is included.\n\n"
        "Credits and licenses: CREDITS.txt, NOTICE.md, LICENSE and LICENSE-ASSETS.\n"
        "Java runtime notices: runtime/legal or lib/runtime/legal.\n"
        "Error log / Registro de errores: ~/.laststandinnote/log.txt\n"
        "https://github.com/MajaProjects/LastStandinNote/issues\n",
        encoding="utf-8",
    )
    if target == "Linux":
        start = image / "Start.sh"
        start.write_text('#!/bin/sh\napp_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)\n'
                         'exec "$app_dir/bin/LastStandinNote" "$@"\n', encoding="utf-8")
        start.chmod(0o755)
    elif (image / "runtime/bin/java.exe").is_file():
        (image / "Debug.cmd").write_bytes(
            b'@echo off\r\ncd /d "%~dp0"\r\n'
            b'"runtime\\bin\\java.exe" -Xmx512m -Dfile.encoding=UTF-8 -jar "app\\LastStandinNote.jar"\r\npause\r\n')


def windows_image(build_dir, game_jar, icon, version, runtime_zip):
    if platform.system() != "Linux":
        raise ValueError("--windows-portable is the Linux cross-build route; use the native build on Windows.")
    compiler = shutil.which("x86_64-w64-mingw32-gcc") or shutil.which("x86_64-w64-mingw32-gcc-posix")
    windres = shutil.which("x86_64-w64-mingw32-windres")
    if not compiler or not windres:
        raise ValueError("The Windows portable cross-build requires the x64 MinGW-w64 C compiler and windres.")
    metadata = json.loads((ROOT / "tools/windows-runtime.json").read_text(encoding="utf-8"))
    if runtime_zip is None:
        cache = ROOT / "build/runtime-cache"
        cache.mkdir(parents=True, exist_ok=True)
        runtime_zip = cache / metadata["name"]
        if not runtime_zip.is_file() or digest(runtime_zip) != metadata["sha256"]:
            with urllib.request.urlopen(metadata["url"], timeout=60) as response, runtime_zip.open("wb") as output:
                shutil.copyfileobj(response, output)
    if digest(runtime_zip) != metadata["sha256"]:
        raise ValueError("The Windows runtime SHA-256 does not match the pinned official download.")

    image = build_dir / "windows" / APP
    image.mkdir(parents=True)
    runtime = image / "runtime"
    with zipfile.ZipFile(runtime_zip) as archive:
        for entry in archive.infolist():
            relative = Path(*Path(entry.filename).parts[1:])
            if not relative.parts:
                continue
            if relative.is_absolute() or ".." in relative.parts:
                raise ValueError("Unsafe runtime archive entry.")
            destination = runtime / relative
            if entry.is_dir():
                destination.mkdir(parents=True, exist_ok=True)
            else:
                destination.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(entry) as source, destination.open("wb") as output:
                    shutil.copyfileobj(source, output)
    (image / "app").mkdir()
    shutil.copy2(game_jar, image / "app" / game_jar.name)
    resource_script = build_dir / "launcher.rc"
    resource_script.write_text(f'1 ICON "{icon.as_posix()}"\n', encoding="utf-8")
    resource_object = build_dir / "launcher-icon.o"
    run(windres, resource_script, "-O", "coff", "-o", resource_object)
    run(compiler, "-Os", "-ffreestanding", "-fno-builtin", "-fno-stack-protector",
        "-fno-tree-loop-distribute-patterns", "-nostdlib", "-mwindows",
        "-Wl,--entry,launcher_entry", "-Wl,--subsystem,windows", "-Wl,--no-insert-timestamp",
        ROOT / "tools/windows_launcher.c", resource_object, "-lkernel32", "-luser32",
        "-o", image / f"{APP}.exe")
    # the launcher imports only Windows system libraries; no compiler runtime is bundled
    (image / "JAVA_RUNTIME_SOURCE.txt").write_text(
        "Eclipse Temurin OpenJDK 21.0.13+9, Windows x64.\n"
        f"Binary: {metadata['url']}\nSHA-256: {metadata['sha256']}\n"
        f"Corresponding runtime source: {metadata['source_url']}\n"
        f"Source SHA-256: {metadata['source_sha256']}\n"
        "Preserve runtime/LICENSE, runtime/NOTICE and runtime/legal.\n", encoding="utf-8")
    return image, metadata


def build(jar_only=False, cross_windows=False, runtime_zip=None):
    version = (ROOT / "resources/version.txt").read_text(encoding="utf-8").strip()
    if not re.fullmatch(r"[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,5}", version):
        raise ValueError("resources/version.txt must contain a numeric version, for example 1.0.0.")
    major, minor, patch = map(int, version.split("."))
    if major > 255 or minor > 255 or patch > 65535:
        raise ValueError("The version exceeds Windows package limits.")
    system = platform.system()
    if system not in ("Windows", "Linux") or platform.machine().lower() not in ("amd64", "x86_64"):
        raise ValueError("Use a Windows or Linux x64 build machine.")
    if not (ROOT / "README.md").read_text(encoding="utf-8").strip():
        raise ValueError("README.md must not be empty.")
    run(sys.executable, ROOT / "tools/validate_charts.py")
    javac, jar, java = (java_tool(name) for name in ("javac", "jar", "java"))
    compiler_version = run(javac, "-version", capture_output=True, text=True).stdout.strip()
    runtime_details = run(java, "-XshowSettings:properties", "-version", capture_output=True, text=True).stderr
    if (compiler_version != "javac 21.0.13"
            or "java.vendor = Eclipse Adoptium" not in runtime_details
            or "java.runtime.version = 21.0.13+9" not in runtime_details):
        raise ValueError("Use Eclipse Temurin 21.0.13+9 so the bundled runtime matches its source archive.")
    build_dir = ROOT / "build/package"
    if build_dir.exists():
        shutil.rmtree(build_dir)
    classes, inputs, checks = (build_dir / name for name in ("classes", "input", "checks"))
    for directory in (classes, inputs, checks):
        directory.mkdir(parents=True, exist_ok=True)
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)
    sources = sorted((ROOT / "src").rglob("*.java"))
    if not sources:
        raise ValueError("No Java source files found.")
    arguments = build_dir / "sources.txt"
    arguments.write_text("\n".join('"' + p.relative_to(ROOT).as_posix() + '"' for p in sources), encoding="utf-8")
    run(javac, "--release", "17", "-encoding", "UTF-8", "-d", classes, "@" + str(arguments))
    game_jar = inputs / (APP + ".jar")
    run(jar, "--create", "--file", game_jar, "--main-class", "Main",
        "-C", classes, ".", "-C", ROOT / "resources", ".")
    with zipfile.ZipFile(game_jar, "a", zipfile.ZIP_DEFLATED) as archive:
        for name in NOTICES:
            archive.write(ROOT / name, "META-INF/" + name)
    run(javac, "--release", "17", "-encoding", "UTF-8", "-cp", game_jar,
        "-d", checks, ROOT / "tools/VerifyPackage.java")
    run(java, "-Xmx512m", "-Djava.awt.headless=true", "-cp",
        os.pathsep.join((str(checks), str(game_jar))), "VerifyPackage", version)
    source = source_archive(dist, version)
    if jar_only:
        output = dist / f"{APP}-{version}.jar"
        shutil.copy2(game_jar, output)
        print(f"Verified executable JAR: {output}")
        return
    icon = ROOT / "packaging" / ("icon.ico" if cross_windows or system == "Windows" else "icon.png")
    if not icon.is_file():
        raise ValueError(f"Native launcher icon is missing: {icon.relative_to(ROOT)}")
    runtime_metadata = None
    if cross_windows:
        image, runtime_metadata = windows_image(build_dir, game_jar, icon, version, runtime_zip)
        target = "Windows"
    else:
        target = system
        native_dir = build_dir / "native"
        run(java_tool("jpackage"), "--type", "app-image", "--name", APP,
            "--app-version", version, "--input", inputs, "--main-jar", game_jar.name,
            "--main-class", "Main", "--dest", native_dir, "--vendor", "MajaProjects",
            "--description", "A rhythm game starring Tux and Windows",
            "--add-modules", "java.desktop,java.prefs,java.logging",
            "--java-options", "-Xmx512m", "--java-options", "-Dfile.encoding=UTF-8",
            "--icon", icon)
        image = native_dir / APP
        launcher = image / (APP + ".exe") if system == "Windows" else image / "bin" / APP
        if not launcher.is_file():
            raise ValueError("Native launcher was not generated.")
    copy_notices(image, version, target)
    remove_copy_temporaries(image)
    if runtime_metadata is None:
        runtime_source = json.loads((ROOT / "tools/windows-runtime.json").read_text(encoding="utf-8"))
        (image / "JAVA_RUNTIME_SOURCE.txt").write_text(
            "This package was built with Eclipse Temurin OpenJDK 21.0.13+9.\n"
            f"Corresponding runtime source: {runtime_source['source_url']}\n"
            f"Source SHA-256: {runtime_source['source_sha256']}\n"
            "Preserve the bundled runtime legal notices.\n", encoding="utf-8")
    stem = f"{APP}-{version}-{target}-x64"
    if target == "Windows":
        binary = Path(shutil.make_archive(str(dist / stem), "zip", root_dir=image.parent, base_dir=APP))
    else:
        binary = dist / (stem + ".tar.gz")
        with tarfile.open(binary, "w:gz") as archive:
            archive.add(image, arcname=APP)
    commit = run("git", "rev-parse", "HEAD", capture_output=True, text=True).stdout.strip()
    changes = run("git", "status", "--porcelain", "--untracked-files=all", capture_output=True, text=True).stdout.splitlines()
    metadata = {"version": version, "platform": target, "architecture": "x64",
                "base_commit": commit, "working_tree_changes": changes,
                "compiler": compiler_version, "bytecode_release": 17,
                "windows_cross_build": cross_windows,
                "native_windows_execution_tested": False if cross_windows else None,
                "runtime_download": runtime_metadata,
                "source_note": "Source archive contains the exact working sources and resources used for this build."}
    info = dist / (stem + "-build-info.json")
    info.write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    (dist / (stem + "-SHA256SUMS.txt")).write_text(
        "".join(f"{digest(path)}  {path.name}\n" for path in (binary, source, info)), encoding="utf-8")
    print(f"Portable package ready: {binary}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jar-only", action="store_true")
    parser.add_argument("--windows-portable", action="store_true", help="cross-build a Windows portable EXE from Linux")
    parser.add_argument("--runtime-zip", type=Path, help="use the checksum-verified pinned Windows JRE ZIP")
    args = parser.parse_args()
    try:
        build(args.jar_only, args.windows_portable, args.runtime_zip)
    except (ValueError, OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f"Packaging failed: {error}", file=sys.stderr)
        sys.exit(1)
