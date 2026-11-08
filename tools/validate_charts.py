#!/usr/bin/env python3
"""Validate charts, re-press gaps and bitmap-font coverage of visible metadata."""

import math
from pathlib import Path
import sys


COMPOSED_CHARACTERS = dict(zip("ÁÉÍÓÚÜÑ¿¡", "AEIOUUN?!"))
DRAWN_PUNCTUATION = set(".,:;_")


def validate_visible_text(root):
    errors = []
    resources = root / "resources"
    config = resources / "images" / "font" / "font.txt"
    glyphs = set()
    try:
        for number, line in enumerate(config.read_text(encoding="utf-8").splitlines(), 1):
            fields = line.strip().split(";")
            if fields[0] == "CARACTER":
                try:
                    glyphs.add(chr(int(fields[1])))
                except (IndexError, ValueError):
                    errors.append(f"{config}:{number}: invalid character code")
    except (OSError, UnicodeError) as error:
        return [f"{config}: could not read font configuration: {error}"]
    if "?" not in glyphs:
        errors.append(f"{config}: missing required '?' fallback glyph")

    def check(text, path, number, field):
        # mirror the supported uppercase glyphs and composed marks in BitmapFont
        unsupported = set()
        for char in text.upper():
            if char in " \n" or char in glyphs or char in DRAWN_PUNCTUATION:
                continue
            if COMPOSED_CHARACTERS.get(char) in glyphs:
                continue
            unsupported.add(char)
        for char in sorted(unsupported):
            errors.append(f"{path.relative_to(root)}:{number}: unsupported character "
                          f"U+{ord(char):04X} {char!r} in {field}; the game would display '?'")

    for path, names, expected in (
            (resources / "songs" / "songs.txt", ("folder", "song title"), 2),
            (resources / "CREDITS.txt", ("category", "creator", "work title", "game use", "license"), 8)):
        try:
            lines = path.read_text(encoding="utf-8").splitlines()
        except (OSError, UnicodeError) as error:
            errors.append(f"{path}: could not read visible text: {error}")
            continue
        for number, line in enumerate(lines, 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            fields = [value.strip() for value in line.split(";")]
            if len(fields) != expected:
                errors.append(f"{path.relative_to(root)}:{number}: expected {expected} fields")
                continue
            # folder ids and the three credits source/license/change fields are not drawn
            for index, name in enumerate(names):
                if name != "folder":
                    check(fields[index], path, number, name)

    # charts supply the title shown in gameplay, game over and results
    for path in sorted((resources / "songs").glob("*/chart_*.txt")):
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            fields = line.strip().split(";", 1)
            if fields[0] == "CANCION" and len(fields) == 2:
                check(fields[1].strip(), path, number, "chart title")
    return errors


def validate(root):
    errors = validate_visible_text(root)
    charts = sorted((root / "resources" / "songs").glob("*/chart_*.txt"))
    if not charts:
        return errors + ["No song charts found."]
    for path in charts:
        notes = []
        declared = None
        duration = None
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            fields = [value.strip() for value in line.split(";")]
            try:
                if fields[0] == "TOTAL_NOTAS":
                    declared = int(fields[1])
                elif fields[0] == "DURACION_MS":
                    duration = int(fields[1])
                elif fields[0] == "NOTA":
                    if len(fields) != 7:
                        raise ValueError("A note needs seven fields")
                    time, voice, lane, length = map(int, fields[1:5])
                    if time < 0 or voice not in (1, 2) or not 0 <= lane <= 3 or length < 0:
                        raise ValueError("Invalid note timing, voice or lane")
                    if not fields[5] or not math.isfinite(float(fields[6])):
                        raise ValueError("Invalid vowel or pitch")
                    notes.append((time, voice, lane, length))
            except (ValueError, IndexError) as error:
                errors.append(f"{path.name}:{number}: {error}")
        if declared != len(notes) or not notes:
            errors.append(f"{path}: declared note count differs from parsed notes")
        if duration is None or duration <= 0:
            errors.append(f"{path}: missing or invalid song duration")
        else:
            for note in notes:
                if note[0] + note[3] > duration:
                    errors.append(f"{path}: note at {note[0]} extends beyond the song")
        lanes = {}
        for note in sorted(notes):
            lanes.setdefault(note[1:3], []).append(note)
        for lane_notes in lanes.values():
            for note, following in zip(lane_notes, lane_notes[1:]):
                if note[3] <= 0:
                    continue
                gap = following[0] - note[0] - note[3]
                # deeper overlaps may be intentional; adjacent heads need a release gap
                if -2 <= gap < 120:
                    errors.append(f"{path}: only {gap} ms before the next head at {following[0]}")
    return errors


if __name__ == "__main__":
    root = Path(__file__).resolve().parents[1]
    errors = validate(root)
    if errors:
        print("\n".join(errors), file=sys.stderr)
        sys.exit(1)
    print("All song charts and visible text passed validation.")
