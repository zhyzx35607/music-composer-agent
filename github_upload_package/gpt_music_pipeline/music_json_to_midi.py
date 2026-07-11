from __future__ import annotations

import argparse
import json
import struct
from pathlib import Path
from typing import Any

from validate_music_json import validate_music_json


DEFAULT_TICKS_PER_BEAT = 480


def write_midi_from_music_json(data: dict[str, Any], output_path: str | Path) -> Path:
    validate_music_json(data)

    ticks_per_beat = int(data.get("ticks_per_beat", DEFAULT_TICKS_PER_BEAT))
    tempo_bpm = int(data["tempo_bpm"])
    tempo_microseconds = round(60_000_000 / tempo_bpm)

    tracks = [_build_meta_track(data, tempo_microseconds)]
    for track in data["tracks"]:
        tracks.append(_build_note_track(track, ticks_per_beat, float(data["total_beats"])))

    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)

    header = b"MThd" + struct.pack(">IHHH", 6, 1, len(tracks), ticks_per_beat)
    body = b"".join(_chunk(b"MTrk", track_bytes) for track_bytes in tracks)
    output.write_bytes(header + body)
    return output


def _build_meta_track(data: dict[str, Any], tempo_microseconds: int) -> bytes:
    events: list[bytes] = []
    events.append(_delta(0) + _meta_text(0x03, data.get("title", "GPT Music")))
    events.append(_delta(0) + b"\xff\x51\x03" + tempo_microseconds.to_bytes(3, "big"))

    numerator, denominator = _parse_time_signature(data.get("time_signature", "4/4"))
    denominator_power = 0
    while 2**denominator_power < denominator:
        denominator_power += 1
    events.append(_delta(0) + b"\xff\x58\x04" + bytes([numerator, denominator_power, 24, 8]))
    events.append(_delta(0) + b"\xff\x2f\x00")
    return b"".join(events)


def _build_note_track(track: dict[str, Any], ticks_per_beat: int, total_beats: float) -> bytes:
    channel = int(track["channel"])
    events: list[tuple[int, int, bytes]] = []

    events.append((0, 0, _meta_text(0x03, track["name"])))
    events.append((0, 1, bytes([0xC0 | channel, int(track["program"])])))

    for note in track["notes"]:
        pitch = int(note["pitch"])
        velocity = int(note["velocity"])
        start_tick = _beats_to_ticks(float(note["start"]), ticks_per_beat)
        end_tick = _beats_to_ticks(float(note["start"]) + float(note["duration"]), ticks_per_beat)
        events.append((start_tick, 2, bytes([0x90 | channel, pitch, velocity])))
        events.append((end_tick, 1, bytes([0x80 | channel, pitch, 0])))

    total_ticks = _beats_to_ticks(total_beats, ticks_per_beat)
    if events:
        last_tick = max(total_ticks, max(tick for tick, _, _ in events))
    else:
        last_tick = total_ticks
    events.append((last_tick, 9, b"\xff\x2f\x00"))

    events.sort(key=lambda item: (item[0], item[1]))

    output = bytearray()
    previous_tick = 0
    for tick, _, payload in events:
        output.extend(_delta(tick - previous_tick))
        output.extend(payload)
        previous_tick = tick
    return bytes(output)


def _beats_to_ticks(beats: float, ticks_per_beat: int) -> int:
    return round(beats * ticks_per_beat)


def _chunk(chunk_type: bytes, data: bytes) -> bytes:
    return chunk_type + struct.pack(">I", len(data)) + data


def _delta(value: int) -> bytes:
    if value < 0:
        raise ValueError("MIDI delta time cannot be negative")

    buffer = value & 0x7F
    value >>= 7
    result = [buffer]
    while value:
        buffer = (value & 0x7F) | 0x80
        result.insert(0, buffer)
        value >>= 7
    return bytes(result)


def _meta_text(meta_type: int, text: str) -> bytes:
    encoded = str(text).encode("utf-8")
    return b"\xff" + bytes([meta_type]) + _delta(len(encoded)) + encoded


def _parse_time_signature(value: str) -> tuple[int, int]:
    numerator_text, denominator_text = value.split("/", maxsplit=1)
    return int(numerator_text), int(denominator_text)


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert music_json to a standard MIDI file.")
    parser.add_argument("input_json")
    parser.add_argument("output_midi")
    args = parser.parse_args()

    data = json.loads(Path(args.input_json).read_text(encoding="utf-8"))
    output = write_midi_from_music_json(data, args.output_midi)
    print(f"saved: {output}")


if __name__ == "__main__":
    main()
