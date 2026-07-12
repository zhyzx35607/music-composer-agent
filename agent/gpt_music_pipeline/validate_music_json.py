from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any


REQUIRED_TOP_LEVEL = {
    "version",
    "title",
    "description",
    "duration_seconds",
    "tempo_bpm",
    "key",
    "time_signature",
    "ticks_per_beat",
    "total_beats",
    "style",
    "mood",
    "chord_progression",
    "tracks",
}

REQUIRED_TRACK = {
    "name",
    "role",
    "instrument",
    "channel",
    "program",
    "is_drum",
    "notes",
}

REQUIRED_NOTE = {"pitch", "start", "duration", "velocity"}


class MusicJsonValidationError(ValueError):
    pass


def load_music_json(path: str | Path) -> dict[str, Any]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def validate_music_json(data: dict[str, Any]) -> list[str]:
    """Validate GPT music JSON and return non-fatal warnings."""
    errors: list[str] = []
    warnings: list[str] = []

    _check_missing("top level", data, REQUIRED_TOP_LEVEL, errors)

    tempo = _number(data.get("tempo_bpm"), "tempo_bpm", errors)
    duration_seconds = _number(data.get("duration_seconds"), "duration_seconds", errors)
    total_beats = _number(data.get("total_beats"), "total_beats", errors)

    if data.get("version") != "1.0":
        errors.append("version must be '1.0'")
    if data.get("ticks_per_beat") != 480:
        errors.append("ticks_per_beat must be 480")
    if tempo is not None and not 40 <= tempo <= 220:
        errors.append("tempo_bpm must be between 40 and 220")
    if duration_seconds is not None and not 5 <= duration_seconds <= 180:
        errors.append("duration_seconds must be between 5 and 180")
    if not isinstance(data.get("mood"), list) or not data.get("mood"):
        errors.append("mood must be a non-empty array")
    if not isinstance(data.get("chord_progression"), list) or not data.get("chord_progression"):
        errors.append("chord_progression must be a non-empty array")

    expected_beats = None
    if tempo is not None and duration_seconds is not None:
        expected_beats = duration_seconds * tempo / 60.0
        if total_beats is not None and abs(total_beats - expected_beats) > max(4.0, expected_beats * 0.15):
            warnings.append(
                f"total_beats {total_beats:g} is far from duration_seconds * tempo_bpm / 60 ({expected_beats:g})"
            )

    tracks = data.get("tracks")
    if not isinstance(tracks, list) or not tracks:
        errors.append("tracks must be a non-empty array")
    else:
        used_channels: set[int] = set()
        for track_index, track in enumerate(tracks):
            _validate_track(track, track_index, total_beats, used_channels, errors, warnings)

    if errors:
        raise MusicJsonValidationError("; ".join(errors))
    return warnings


def _validate_track(
    track: Any,
    track_index: int,
    total_beats: float | None,
    used_channels: set[int],
    errors: list[str],
    warnings: list[str],
) -> None:
    label = f"tracks[{track_index}]"
    if not isinstance(track, dict):
        errors.append(f"{label} must be an object")
        return

    _check_missing(label, track, REQUIRED_TRACK, errors)

    channel = _integer(track.get("channel"), f"{label}.channel", errors)
    program = _integer(track.get("program"), f"{label}.program", errors)
    is_drum = track.get("is_drum")

    if channel is not None and not 0 <= channel <= 15:
        errors.append(f"{label}.channel must be between 0 and 15")
    if program is not None and not 0 <= program <= 127:
        errors.append(f"{label}.program must be between 0 and 127")
    if not isinstance(is_drum, bool):
        errors.append(f"{label}.is_drum must be boolean")

    if channel is not None:
        if channel in used_channels and channel != 9:
            warnings.append(f"{label}.channel {channel} is reused")
        used_channels.add(channel)
    if is_drum is True and channel != 9:
        errors.append(f"{label} is marked as drum but channel is not 9")
    if is_drum is False and channel == 9:
        errors.append(f"{label} uses drum channel 9 but is_drum is false")

    notes = track.get("notes")
    if not isinstance(notes, list) or not notes:
        errors.append(f"{label}.notes must be a non-empty array")
        return

    for note_index, note in enumerate(notes):
        _validate_note(note, f"{label}.notes[{note_index}]", total_beats, errors)


def _validate_note(note: Any, label: str, total_beats: float | None, errors: list[str]) -> None:
    if not isinstance(note, dict):
        errors.append(f"{label} must be an object")
        return

    _check_missing(label, note, REQUIRED_NOTE, errors)
    pitch = _integer(note.get("pitch"), f"{label}.pitch", errors)
    start = _number(note.get("start"), f"{label}.start", errors)
    duration = _number(note.get("duration"), f"{label}.duration", errors)
    velocity = _integer(note.get("velocity"), f"{label}.velocity", errors)

    if pitch is not None and not 0 <= pitch <= 127:
        errors.append(f"{label}.pitch must be between 0 and 127")
    if start is not None and start < 0:
        errors.append(f"{label}.start must be >= 0")
    if duration is not None and duration <= 0:
        errors.append(f"{label}.duration must be > 0")
    if duration is not None and duration > 32:
        errors.append(f"{label}.duration must be <= 32")
    if velocity is not None and not 1 <= velocity <= 127:
        errors.append(f"{label}.velocity must be between 1 and 127")
    if total_beats is not None and start is not None and duration is not None:
        if start + duration > total_beats + 0.001:
            errors.append(f"{label} ends after total_beats")


def _check_missing(label: str, data: dict[str, Any], required: set[str], errors: list[str]) -> None:
    missing = sorted(required - set(data))
    if missing:
        errors.append(f"{label} missing fields: {', '.join(missing)}")


def _number(value: Any, label: str, errors: list[str]) -> float | None:
    if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(float(value)):
        errors.append(f"{label} must be a finite number")
        return None
    return float(value)


def _integer(value: Any, label: str, errors: list[str]) -> int | None:
    if isinstance(value, bool) or not isinstance(value, int):
        errors.append(f"{label} must be an integer")
        return None
    return value


def main() -> None:
    import argparse

    parser = argparse.ArgumentParser(description="Validate GPT music JSON.")
    parser.add_argument("json_path")
    args = parser.parse_args()

    data = load_music_json(args.json_path)
    warnings = validate_music_json(data)
    print("valid")
    for warning in warnings:
        print(f"warning: {warning}")


if __name__ == "__main__":
    main()

