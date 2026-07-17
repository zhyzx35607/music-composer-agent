from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from music21 import chord, converter, instrument, key, metadata, meter, note, stream, tempo


def score_to_context(input_path: str | Path, max_events: int | None = 4000) -> dict[str, Any]:
    path = Path(input_path)
    score = converter.parse(str(path))

    parts = list(score.parts) if getattr(score, "parts", None) else [score]
    tempo_bpm = _first_tempo(score)
    total_quarter_length = float(score.highestTime or 0)
    events_used = 0
    truncated = False
    part_contexts: list[dict[str, Any]] = []

    for part_index, part in enumerate(parts):
        events: list[dict[str, Any]] = []
        flat_part = part.flatten()
        part_events = list(flat_part.notesAndRests)

        for item in part_events:
            if max_events is not None and events_used >= max_events:
                truncated = True
                break
            event = _event_to_json(item)
            if event is not None:
                events.append(event)
                events_used += 1

        part_contexts.append(
            {
                "part_index": part_index,
                "part_id": getattr(part, "id", None),
                "name": _part_name(part, part_index),
                "instrument": _instrument_name(part),
                "midi_program": _midi_program(part),
                "midi_channel": _midi_channel(part),
                "event_count_included": len(events),
                "events": events,
            }
        )
        if truncated:
            break

    return {
        "score_format": _score_format(path),
        "parser": "music21",
        "source_file": path.name,
        "title": _title(score),
        "composer": _composer(score),
        "key": _first_key(score),
        "time_signatures": _time_signatures(score),
        "tempo_bpm": tempo_bpm,
        "duration_quarter_length": round(total_quarter_length, 4),
        "estimated_duration_seconds": _estimate_seconds(total_quarter_length, tempo_bpm),
        "part_count": len(parts),
        "event_count_included": events_used,
        "event_limit": max_events,
        "truncated": truncated,
        "parts": part_contexts,
        "instruction_for_gpt": (
            "Use this parsed score JSON as the authoritative source score. "
            "Preserve recognizable motifs, note timing, harmony, sections, and instrumentation unless the user asks to change them. "
            "When revising, produce the project's required music_json output, not MusicXML."
        ),
    }


def convert_score_to_midi(
    input_path: str | Path,
    output_path: str | Path,
    tempo_bpm: int | None = None,
) -> Path:
    score = converter.parse(str(input_path))
    if tempo_bpm is not None:
        _replace_tempo(score, tempo_bpm)
    output = Path(output_path)
    output.parent.mkdir(parents=True, exist_ok=True)
    score.write("midi", fp=str(output))
    return output


def score_to_music_json(input_path: str | Path, tempo_bpm: int | None = None) -> dict[str, Any]:
    """Convert a MIDI/MusicXML score into the pipeline's editable music_json shape."""
    path = Path(input_path)
    score = converter.parse(str(path))
    parts = list(score.parts) if getattr(score, "parts", None) else [score]
    resolved_tempo = int(tempo_bpm or _first_tempo(score) or 120)
    total_beats = round(float(score.highestTime or 0), 6)
    if total_beats <= 0:
        raise ValueError(f"Score contains no timed music events: {path}")

    tracks: list[dict[str, Any]] = []
    used_channels: set[int] = set()
    for part_index, part in enumerate(parts):
        notes: list[dict[str, Any]] = []
        for item in part.flatten().notes:
            start = round(float(item.offset), 6)
            duration = round(float(item.duration.quarterLength), 6)
            velocity = _volume_velocity(item) or 80
            if isinstance(item, note.Note):
                pitches = [int(item.pitch.midi)]
            elif isinstance(item, chord.Chord):
                pitches = [int(pitch.midi) for pitch in item.pitches]
            else:
                continue
            for pitch in pitches:
                notes.append(
                    {
                        "pitch": pitch,
                        "start": start,
                        "duration": max(0.03125, min(32.0, duration)),
                        "velocity": max(1, min(127, int(velocity))),
                    }
                )
        if not notes:
            continue

        name = _part_name(part, part_index)
        instrument_name = _instrument_name(part) or "Acoustic Grand Piano"
        is_drum = _looks_like_drum_part(part, instrument_name)
        channel = _unique_channel(_midi_channel(part), used_channels, is_drum)
        used_channels.add(channel)
        tracks.append(
            {
                "name": name,
                "role": "main melody" if not tracks else ("rhythm" if is_drum else "accompaniment"),
                "instrument": instrument_name,
                "channel": channel,
                "program": 0 if is_drum else int(_midi_program(part) or 0),
                "is_drum": is_drum,
                "notes": sorted(notes, key=lambda item: (item["start"], item["pitch"], item["duration"])),
            }
        )

    if not tracks:
        raise ValueError(f"Score contains no convertible pitched notes: {path}")

    signatures = _time_signatures(score)
    analyzed_key = _first_key(score) or "C major"
    title = _title(score)
    if title == "Untitled Score":
        title = path.stem
    return {
        "version": "1.0",
        "title": title[:80],
        "description": f"Editable symbolic score imported from {path.name}."[:240],
        "duration_seconds": round(total_beats * 60 / resolved_tempo, 3),
        "tempo_bpm": resolved_tempo,
        "key": analyzed_key[:32],
        "time_signature": signatures[0] if signatures else "4/4",
        "ticks_per_beat": 480,
        "total_beats": total_beats,
        "style": "imported score",
        "mood": ["source"],
        "chord_progression": ["Source"],
        "tracks": tracks,
    }


def _event_to_json(item: note.NotRest) -> dict[str, Any] | None:
    offset = float(item.offset)
    duration = float(item.duration.quarterLength)
    measure_number = getattr(item, "measureNumber", None)

    if isinstance(item, note.Rest):
        return {
            "type": "rest",
            "measure": measure_number,
            "start_beat": round(offset, 4),
            "duration_beats": round(duration, 4),
        }

    if isinstance(item, note.Note):
        return {
            "type": "note",
            "measure": measure_number,
            "start_beat": round(offset, 4),
            "duration_beats": round(duration, 4),
            "pitch": item.pitch.nameWithOctave,
            "midi": int(item.pitch.midi),
            "velocity": _volume_velocity(item),
        }

    if isinstance(item, chord.Chord):
        return {
            "type": "chord",
            "measure": measure_number,
            "start_beat": round(offset, 4),
            "duration_beats": round(duration, 4),
            "pitches": [p.nameWithOctave for p in item.pitches],
            "midis": [int(p.midi) for p in item.pitches],
            "common_name": item.commonName,
            "velocity": _volume_velocity(item),
        }

    return None


def _first_tempo(score: stream.Stream) -> int | None:
    marks = list(score.recurse().getElementsByClass(tempo.MetronomeMark))
    for mark in marks:
        if mark.number:
            return round(float(mark.number))
    return None


def _estimate_seconds(total_quarter_length: float, tempo_bpm: int | None) -> float | None:
    if not tempo_bpm:
        return None
    return round(total_quarter_length * 60 / tempo_bpm, 3)


def _first_key(score: stream.Stream) -> str | None:
    keys = list(score.recurse().getElementsByClass(key.Key))
    if keys:
        return str(keys[0])
    key_sigs = list(score.recurse().getElementsByClass(key.KeySignature))
    if key_sigs:
        return str(key_sigs[0].asKey())
    try:
        analyzed = score.analyze("key")
        return str(analyzed)
    except Exception:
        return None


def _time_signatures(score: stream.Stream) -> list[str]:
    signatures: list[str] = []
    for ts in score.recurse().getElementsByClass(meter.TimeSignature):
        ratio = ts.ratioString
        if ratio not in signatures:
            signatures.append(ratio)
    return signatures


def _title(score: stream.Stream) -> str:
    md = getattr(score, "metadata", None)
    if isinstance(md, metadata.Metadata) and md.title:
        return str(md.title)
    return "Untitled Score"


def _composer(score: stream.Stream) -> str | None:
    md = getattr(score, "metadata", None)
    if isinstance(md, metadata.Metadata) and md.composer:
        return str(md.composer)
    return None


def _part_name(part: stream.Stream, part_index: int) -> str:
    for attr in ("partName", "partAbbreviation"):
        value = getattr(part, attr, None)
        if value:
            return str(value)
    return f"Part {part_index + 1}"


def _instrument_name(part: stream.Stream) -> str | None:
    best = _preferred_instrument(part)
    if best is None:
        return None
    return best.instrumentName or best.bestName()


def _midi_program(part: stream.Stream) -> int | None:
    best = _preferred_instrument(part)
    value = getattr(best, "midiProgram", None) if best is not None else None
    return int(value) if value is not None else None


def _midi_channel(part: stream.Stream) -> int | None:
    best = _preferred_instrument(part)
    value = getattr(best, "midiChannel", None) if best is not None else None
    return int(value) if value is not None else None


def _preferred_instrument(part: stream.Stream) -> instrument.Instrument | None:
    instruments = list(part.recurse().getElementsByClass(instrument.Instrument))
    if not instruments:
        return None
    with_program = [inst for inst in instruments if getattr(inst, "midiProgram", None) is not None]
    if with_program:
        non_voice = [
            inst for inst in with_program
            if inst.__class__.__name__.lower() not in {"soprano", "alto", "tenor", "bass", "voice"}
            and (inst.instrumentName or inst.bestName() or "").lower() not in {"soprano", "alto", "tenor", "bass", "voice"}
        ]
        return non_voice[-1] if non_voice else with_program[-1]
    return instruments[0]


def _looks_like_drum_part(part: stream.Stream, instrument_name: str) -> bool:
    best = _preferred_instrument(part)
    class_name = best.__class__.__name__.lower() if best is not None else ""
    name = instrument_name.lower()
    return any(token in class_name or token in name for token in ("percussion", "drum", "cymbal"))


def _unique_channel(preferred: int | None, used: set[int], is_drum: bool) -> int:
    if is_drum:
        return 9
    if preferred is not None and 0 <= preferred <= 15 and preferred != 9 and preferred not in used:
        return preferred
    for channel in range(16):
        if channel != 9 and channel not in used:
            return channel
    raise ValueError("Score has more MIDI parts than available channels")


def _replace_tempo(score: stream.Stream, tempo_bpm: int) -> None:
    if tempo_bpm <= 0:
        raise ValueError("tempo_bpm must be greater than 0")
    for mark in list(score.recurse().getElementsByClass(tempo.MetronomeMark)):
        if mark.activeSite is not None:
            mark.activeSite.remove(mark)
    score.insert(0, tempo.MetronomeMark(number=tempo_bpm))


def _volume_velocity(item: note.NotRest) -> int | None:
    value = getattr(item.volume, "velocity", None)
    if value is None:
        return None
    return int(value)


def _score_format(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix == ".mxl":
        return "Compressed MusicXML"
    if suffix in {".musicxml", ".xml"}:
        return "MusicXML"
    if suffix in {".mid", ".midi"}:
        return "MIDI"
    return suffix.lstrip(".").upper() or "unknown"


def main() -> None:
    parser = argparse.ArgumentParser(description="Parse MusicXML/MXL/MIDI into GPT-readable score JSON.")
    parser.add_argument("input_score")
    parser.add_argument("--output-json")
    parser.add_argument("--output-midi")
    parser.add_argument("--max-events", type=int, default=4000)
    parser.add_argument("--full", action="store_true", help="Do not limit note/rest events in JSON output.")
    args = parser.parse_args()

    max_events = None if args.full else args.max_events
    context = score_to_context(args.input_score, max_events=max_events)
    payload = json.dumps(context, ensure_ascii=False, indent=2)

    if args.output_json:
        output_json = Path(args.output_json)
        output_json.parent.mkdir(parents=True, exist_ok=True)
        output_json.write_text(payload, encoding="utf-8")
        print(f"saved-json: {output_json}")
    else:
        print(payload)

    if args.output_midi:
        output_midi = convert_score_to_midi(args.input_score, args.output_midi)
        print(f"saved-midi: {output_midi}")


if __name__ == "__main__":
    main()
