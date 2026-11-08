#!/usr/bin/env python3
"""Validate chart structure and short gaps that require an immediate re-press."""

import math
from pathlib import Path
import sys


def validate(root):
    errors = []
    charts = sorted((root / "resources" / "songs").glob("*/chart_*.txt"))
    if not charts:
        return ["No song charts found."]
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
    print("All song charts passed validation.")
