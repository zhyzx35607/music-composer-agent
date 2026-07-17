from __future__ import annotations

import copy
import json
import math
from collections import Counter
from pathlib import Path
from typing import Any

from validate_music_json import validate_music_json


PLAN_VERSION = "1.0"
ALLOWED_OPERATION_TYPES = {
    "set_tempo",
    "scale_tempo",
    "transpose",
    "velocity_shift",
    "velocity_scale",
    "duration_scale",
    "note_edits",
    "add_notes",
    "remove_notes",
    "set_program",
    "add_track",
    "remove_track",
}
ALLOWED_METADATA_FIELDS = {
    "title",
    "description",
    "style",
    "mood",
    "key",
    "chord_progression",
}


class RevisionPlanValidationError(ValueError):
    pass


def build_revision_plan_prompt(
    revision_request: str,
    source_music_json: dict[str, Any],
    ui_parameters: dict[str, Any],
) -> str:
    indexed_source = copy.deepcopy(source_music_json)
    for track_index, track in enumerate(indexed_source.get("tracks", [])):
        track["track_index"] = track_index
        for note_index, source_note in enumerate(track.get("notes", [])):
            source_note["note_index"] = note_index
    source_payload = json.dumps(indexed_source, ensure_ascii=False, separators=(",", ":"))
    return f"""You are an AI music revision planner.

You must plan targeted edits to an existing symbolic score. Do not rewrite or return the complete music_json.
Return one revision_plan JSON object only. Python will validate and apply the plan to the source score.

USER REVISION REQUEST:
{revision_request.strip()}

UI PARAMETERS:
{json.dumps(ui_parameters, ensure_ascii=False, indent=2)}

INDEXED SOURCE MUSIC_JSON (read-only; track_index/note_index are references for this plan only):
{source_payload}

REQUIRED PLAN SHAPE:
{{
  "version": "1.0",
  "intent": "mood_adjustment",
  "summary": "用中文简要说明准备怎样修改",
  "strength": 0.35,
  "scope": {{
    "start_beat": 0,
    "end_beat": {source_music_json.get("total_beats", 1)},
    "track_indices": [0]
  }},
  "preserve": {{
    "duration": true,
    "time_signature": true,
    "instruments": true,
    "outside_scope": true
  }},
  "max_changed_note_ratio": 0.35,
  "metadata_updates": {{"mood": ["cheerful"]}},
  "operations": []
}}

ALLOWED OPERATIONS:
- {{"type":"set_tempo","bpm":120}}
- {{"type":"scale_tempo","factor":1.08}}
- {{"type":"transpose","semitones":2,"track_indices":[0],"start_beat":0,"end_beat":16}}
- {{"type":"velocity_shift","amount":6,"track_indices":[0,1]}}
- {{"type":"velocity_scale","factor":1.08,"track_indices":[0,1]}}
- {{"type":"duration_scale","factor":0.85,"track_indices":[0]}}
- {{"type":"note_edits","edits":[{{"track_index":0,"note_index":4,"pitch":72,"start":4,"duration":0.5,"velocity":88}}]}}
- {{"type":"add_notes","track_index":1,"notes":[{{"pitch":60,"start":8,"duration":0.5,"velocity":72}}]}}
- {{"type":"remove_notes","targets":[{{"track_index":1,"note_index":3}}]}}
- {{"type":"set_program","track_index":0,"program":81,"instrument":"lead synth"}}
- {{"type":"add_track","track":{{"name":"Drums","role":"rhythm","instrument":"standard drum kit","channel":9,"program":0,"is_drum":true,"notes":[{{"pitch":36,"start":0,"duration":0.1,"velocity":85}}]}}}}
- {{"type":"remove_track","track_index":2}}

STRICT RULES:
1. Output the revision plan only. Never output a music_json field or a complete rewritten score.
2. Prefer a small number of understandable operations. Use note_edits only for notes that truly need musical rewriting.
3. For vague requests such as "make it happier", preserve the recognizable melody and use low-strength changes to mood metadata, articulation, dynamics, and selected notes. Do not assume that faster alone means happier.
4. Keep preserve.instruments=true unless the user explicitly requests different instrumentation or arrangement.
5. Keep preserve.duration=true unless the user explicitly asks to change speed or duration. Do not emit set_tempo/scale_tempo while duration is preserved.
6. Changes outside scope are forbidden. Use the full beat range only when the user asks to change the whole piece or gives no local range.
7. strength and max_changed_note_ratio must be between 0 and 1. For "a little", keep strength <= 0.35 and max_changed_note_ratio <= 0.35.
8. Every referenced track_index and note_index must exist in SOURCE MUSIC_JSON.
9. Do not change instruments through metadata_updates. Use set_program/add_track/remove_track only when preserve.instruments=false.
10. Return valid JSON with no Markdown or comments.
"""


def validate_revision_plan(plan: dict[str, Any], source_music_json: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    warnings: list[str] = []
    if not isinstance(plan, dict):
        raise RevisionPlanValidationError("revision plan must be an object")

    required = {"version", "intent", "summary", "strength", "scope", "preserve", "operations"}
    missing = sorted(required - set(plan))
    if missing:
        errors.append(f"missing fields: {', '.join(missing)}")
    if plan.get("version") != PLAN_VERSION:
        errors.append(f"version must be '{PLAN_VERSION}'")
    if not isinstance(plan.get("intent"), str) or not plan.get("intent", "").strip():
        errors.append("intent must be a non-empty string")
    if not isinstance(plan.get("summary"), str) or not plan.get("summary", "").strip():
        errors.append("summary must be a non-empty string")

    strength = _finite_number(plan.get("strength"), "strength", errors)
    if strength is not None and not 0 <= strength <= 1:
        errors.append("strength must be between 0 and 1")
    max_ratio = _finite_number(plan.get("max_changed_note_ratio", 0.35), "max_changed_note_ratio", errors)
    if max_ratio is not None and not 0 <= max_ratio <= 1:
        errors.append("max_changed_note_ratio must be between 0 and 1")

    total_beats = float(source_music_json.get("total_beats", 0))
    tracks = source_music_json.get("tracks", [])
    scope = _validate_scope(plan.get("scope"), total_beats, len(tracks), "scope", errors)

    preserve = plan.get("preserve")
    if not isinstance(preserve, dict):
        errors.append("preserve must be an object")
        preserve = {}
    for field in ("duration", "time_signature", "instruments", "outside_scope"):
        if field not in preserve or not isinstance(preserve.get(field), bool):
            errors.append(f"preserve.{field} must be boolean")

    metadata_updates = plan.get("metadata_updates", {})
    if not isinstance(metadata_updates, dict):
        errors.append("metadata_updates must be an object")
    else:
        unknown = sorted(set(metadata_updates) - ALLOWED_METADATA_FIELDS)
        if unknown:
            errors.append(f"unsupported metadata_updates fields: {', '.join(unknown)}")
        if "mood" in metadata_updates and not _non_empty_string_list(metadata_updates["mood"]):
            errors.append("metadata_updates.mood must be a non-empty string array")
        if "chord_progression" in metadata_updates and not _non_empty_string_list(metadata_updates["chord_progression"]):
            errors.append("metadata_updates.chord_progression must be a non-empty string array")

    operations = plan.get("operations")
    if not isinstance(operations, list):
        errors.append("operations must be an array")
        operations = []
    if len(operations) > 64:
        errors.append("operations must contain at most 64 items")

    for operation_index, operation in enumerate(operations):
        _validate_operation(
            operation,
            operation_index,
            source_music_json,
            scope,
            preserve,
            errors,
            warnings,
        )

    if errors:
        raise RevisionPlanValidationError("; ".join(errors))
    return warnings


def apply_revision_plan(
    source_music_json: dict[str, Any],
    plan: dict[str, Any],
) -> tuple[dict[str, Any], dict[str, Any]]:
    validate_music_json(source_music_json)
    validate_revision_plan(plan, source_music_json)

    revised = copy.deepcopy(source_music_json)
    original = copy.deepcopy(source_music_json)
    scope = _resolved_scope(plan["scope"], source_music_json)
    preserve = plan["preserve"]
    metadata_changes: dict[str, Any] = {}
    operations_applied: list[dict[str, Any]] = []

    for field, value in plan.get("metadata_updates", {}).items():
        old_value = revised.get(field)
        if old_value != value:
            revised[field] = copy.deepcopy(value)
            metadata_changes[field] = {"from": old_value, "to": value}

    for operation_index, operation in enumerate(plan["operations"]):
        result = _apply_operation(revised, operation, scope, preserve)
        result["operation_index"] = operation_index
        result["type"] = operation["type"]
        operations_applied.append(result)

    if preserve["duration"]:
        revised["duration_seconds"] = original["duration_seconds"]
        revised["total_beats"] = original["total_beats"]
    if preserve["time_signature"]:
        revised["time_signature"] = original["time_signature"]
        revised["ticks_per_beat"] = original["ticks_per_beat"]
    if preserve["instruments"]:
        _restore_instruments(revised, original)

    _sort_track_notes(revised)
    validate_music_json(revised)

    note_diff = _note_diff(original, revised)
    max_ratio = float(plan.get("max_changed_note_ratio", 0.35))
    if note_diff["changed_note_ratio"] > max_ratio + 1e-9:
        raise RevisionPlanValidationError(
            f"plan changed {note_diff['changed_note_ratio']:.3f} of notes, above max_changed_note_ratio {max_ratio:.3f}"
        )
    if preserve["outside_scope"]:
        _assert_outside_scope_unchanged(original, revised, scope)

    report = {
        "mode": "revision_plan",
        "intent": plan["intent"],
        "summary": plan["summary"],
        "strength": plan["strength"],
        "scope": scope,
        "metadata_changes": metadata_changes,
        "operations_applied": operations_applied,
        **note_diff,
    }
    if original.get("tempo_bpm") != revised.get("tempo_bpm"):
        report["tempo"] = {"from": original.get("tempo_bpm"), "to": revised.get("tempo_bpm")}
    return revised, report


def deterministic_tempo_plan(source_music_json: dict[str, Any], tempo_bpm: int) -> dict[str, Any]:
    old_tempo = int(source_music_json["tempo_bpm"])
    return {
        "version": PLAN_VERSION,
        "intent": "tempo_adjustment" if old_tempo != tempo_bpm else "preserve_original",
        "summary": (
            f"仅将速度从 {old_tempo} BPM 调整为 {tempo_bpm} BPM，保留全部音符与乐器。"
            if old_tempo != tempo_bpm
            else "保持原曲速度、音符与乐器不变，仅执行格式转换。"
        ),
        "strength": 0.0 if old_tempo == tempo_bpm else min(1.0, abs(tempo_bpm - old_tempo) / max(old_tempo, 1)),
        "scope": {
            "start_beat": 0,
            "end_beat": source_music_json["total_beats"],
            "track_indices": list(range(len(source_music_json["tracks"]))),
        },
        "preserve": {
            "duration": old_tempo == tempo_bpm,
            "time_signature": True,
            "instruments": True,
            "outside_scope": True,
        },
        "max_changed_note_ratio": 0.0,
        "metadata_updates": {},
        "operations": [] if old_tempo == tempo_bpm else [{"type": "set_tempo", "bpm": int(tempo_bpm)}],
    }


def save_revision_plan(plan: dict[str, Any], path: str | Path) -> Path:
    output = Path(path)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(plan, ensure_ascii=False, indent=2), encoding="utf-8")
    return output


def _validate_scope(
    value: Any,
    total_beats: float,
    track_count: int,
    label: str,
    errors: list[str],
) -> dict[str, Any]:
    if not isinstance(value, dict):
        errors.append(f"{label} must be an object")
        return {"start_beat": 0.0, "end_beat": total_beats, "track_indices": list(range(track_count))}
    start = _finite_number(value.get("start_beat"), f"{label}.start_beat", errors)
    end = _finite_number(value.get("end_beat"), f"{label}.end_beat", errors)
    indices = value.get("track_indices")
    if start is not None and not 0 <= start < total_beats:
        errors.append(f"{label}.start_beat must be within the score")
    if end is not None and not 0 < end <= total_beats:
        errors.append(f"{label}.end_beat must be within the score")
    if start is not None and end is not None and start >= end:
        errors.append(f"{label}.start_beat must be less than end_beat")
    if not isinstance(indices, list) or not indices:
        errors.append(f"{label}.track_indices must be a non-empty array")
        indices = []
    for index in indices:
        if isinstance(index, bool) or not isinstance(index, int) or not 0 <= index < track_count:
            errors.append(f"{label}.track_indices contains invalid track index {index!r}")
    return {
        "start_beat": start if start is not None else 0.0,
        "end_beat": end if end is not None else total_beats,
        "track_indices": indices,
    }


def _validate_operation(
    operation: Any,
    operation_index: int,
    source: dict[str, Any],
    plan_scope: dict[str, Any],
    preserve: dict[str, Any],
    errors: list[str],
    warnings: list[str],
) -> None:
    label = f"operations[{operation_index}]"
    if not isinstance(operation, dict):
        errors.append(f"{label} must be an object")
        return
    operation_type = operation.get("type")
    if operation_type not in ALLOWED_OPERATION_TYPES:
        errors.append(f"{label}.type is unsupported: {operation_type!r}")
        return

    tracks = source["tracks"]
    total_beats = float(source["total_beats"])
    if any(field in operation for field in ("start_beat", "end_beat", "track_indices")):
        merged_scope = {
            "start_beat": operation.get("start_beat", plan_scope["start_beat"]),
            "end_beat": operation.get("end_beat", plan_scope["end_beat"]),
            "track_indices": operation.get("track_indices", plan_scope["track_indices"]),
        }
        operation_scope = _validate_scope(merged_scope, total_beats, len(tracks), label, errors)
        if operation_scope["start_beat"] < plan_scope["start_beat"] or operation_scope["end_beat"] > plan_scope["end_beat"]:
            errors.append(f"{label} beat range must stay inside the plan scope")
        if not set(operation_scope["track_indices"]).issubset(set(plan_scope["track_indices"])):
            errors.append(f"{label}.track_indices must stay inside the plan scope")

    if operation_type in {"set_tempo", "scale_tempo"}:
        if preserve.get("duration") is True:
            errors.append(f"{label} changes tempo while preserve.duration is true")
        field = "bpm" if operation_type == "set_tempo" else "factor"
        value = _finite_number(operation.get(field), f"{label}.{field}", errors)
        if operation_type == "set_tempo" and value is not None and not 40 <= value <= 220:
            errors.append(f"{label}.bpm must be between 40 and 220")
        if operation_type == "scale_tempo" and value is not None and not 0.5 <= value <= 2:
            errors.append(f"{label}.factor must be between 0.5 and 2")
    elif operation_type == "transpose":
        semitones = operation.get("semitones")
        if isinstance(semitones, bool) or not isinstance(semitones, int) or not -24 <= semitones <= 24:
            errors.append(f"{label}.semitones must be an integer between -24 and 24")
    elif operation_type == "velocity_shift":
        amount = operation.get("amount")
        if isinstance(amount, bool) or not isinstance(amount, int) or not -64 <= amount <= 64:
            errors.append(f"{label}.amount must be an integer between -64 and 64")
    elif operation_type in {"velocity_scale", "duration_scale"}:
        factor = _finite_number(operation.get("factor"), f"{label}.factor", errors)
        if factor is not None and not 0.25 <= factor <= 4:
            errors.append(f"{label}.factor must be between 0.25 and 4")
    elif operation_type == "note_edits":
        edits = operation.get("edits")
        if not isinstance(edits, list) or not edits:
            errors.append(f"{label}.edits must be a non-empty array")
        elif len(edits) > 256:
            errors.append(f"{label}.edits must contain at most 256 items")
        else:
            for edit_index, edit in enumerate(edits):
                _validate_note_reference(edit, source, f"{label}.edits[{edit_index}]", errors, require_update=True)
    elif operation_type == "add_notes":
        track_index = operation.get("track_index")
        if not _valid_track_index(track_index, len(tracks)):
            errors.append(f"{label}.track_index is invalid")
        notes = operation.get("notes")
        if not isinstance(notes, list) or not notes:
            errors.append(f"{label}.notes must be a non-empty array")
        elif len(notes) > 256:
            errors.append(f"{label}.notes must contain at most 256 items")
        else:
            for note_index, note in enumerate(notes):
                _validate_note_values(note, total_beats, f"{label}.notes[{note_index}]", errors, require_all=True)
    elif operation_type == "remove_notes":
        targets = operation.get("targets")
        if not isinstance(targets, list) or not targets:
            errors.append(f"{label}.targets must be a non-empty array")
        else:
            for target_index, target in enumerate(targets):
                _validate_note_reference(target, source, f"{label}.targets[{target_index}]", errors)
    elif operation_type == "set_program":
        if preserve.get("instruments") is True:
            errors.append(f"{label} changes an instrument while preserve.instruments is true")
        track_index = operation.get("track_index")
        program = operation.get("program")
        if not _valid_track_index(track_index, len(tracks)):
            errors.append(f"{label}.track_index is invalid")
        elif track_index not in plan_scope["track_indices"]:
            errors.append(f"{label}.track_index must stay inside the plan scope")
        if isinstance(program, bool) or not isinstance(program, int) or not 0 <= program <= 127:
            errors.append(f"{label}.program must be an integer between 0 and 127")
    elif operation_type == "add_track":
        if preserve.get("instruments") is True:
            errors.append(f"{label} adds an instrument while preserve.instruments is true")
        track = operation.get("track")
        if not isinstance(track, dict):
            errors.append(f"{label}.track must be an object")
        else:
            candidate = copy.deepcopy(source)
            candidate["tracks"] = [track]
            try:
                validate_music_json(candidate)
            except Exception as exc:
                errors.append(f"{label}.track is invalid: {exc}")
    elif operation_type == "remove_track":
        if preserve.get("instruments") is True:
            errors.append(f"{label} removes an instrument while preserve.instruments is true")
        if not _valid_track_index(operation.get("track_index"), len(tracks)):
            errors.append(f"{label}.track_index is invalid")
        if len(tracks) <= 1:
            errors.append(f"{label} cannot remove the only track")


def _apply_operation(
    music_json: dict[str, Any],
    operation: dict[str, Any],
    plan_scope: dict[str, Any],
    preserve: dict[str, Any],
) -> dict[str, Any]:
    operation_type = operation["type"]
    scope = _operation_scope(operation, plan_scope)
    changed = 0

    if operation_type == "set_tempo":
        old = int(music_json["tempo_bpm"])
        new = int(operation["bpm"])
        music_json["tempo_bpm"] = new
        music_json["duration_seconds"] = round(float(music_json["total_beats"]) * 60 / new, 3)
        return {"from": old, "to": new}
    if operation_type == "scale_tempo":
        old = int(music_json["tempo_bpm"])
        new = max(40, min(220, round(old * float(operation["factor"]))))
        music_json["tempo_bpm"] = new
        music_json["duration_seconds"] = round(float(music_json["total_beats"]) * 60 / new, 3)
        return {"from": old, "to": new}
    if operation_type in {"transpose", "velocity_shift", "velocity_scale", "duration_scale"}:
        for track_index, _, note in _iter_notes_in_scope(music_json, scope):
            track = music_json["tracks"][track_index]
            if operation_type == "transpose":
                if track.get("is_drum"):
                    continue
                note["pitch"] = max(0, min(127, int(note["pitch"]) + int(operation["semitones"])))
            elif operation_type == "velocity_shift":
                note["velocity"] = max(1, min(127, int(note["velocity"]) + int(operation["amount"])))
            elif operation_type == "velocity_scale":
                note["velocity"] = max(1, min(127, round(int(note["velocity"]) * float(operation["factor"]))))
            else:
                max_duration = min(
                    float(music_json["total_beats"]),
                    float(scope["end_beat"]),
                ) - float(note["start"])
                note["duration"] = round(
                    max(0.03125, min(max_duration, float(note["duration"]) * float(operation["factor"]))),
                    6,
                )
            changed += 1
        return {"changed_notes": changed, "scope": scope}
    if operation_type == "note_edits":
        for edit in operation["edits"]:
            note = music_json["tracks"][edit["track_index"]]["notes"][edit["note_index"]]
            _assert_note_in_scope(note, edit["track_index"], scope)
            for field in ("pitch", "start", "duration", "velocity"):
                if field in edit:
                    note[field] = edit[field]
            _validate_note_values(note, float(music_json["total_beats"]), "edited note", [], require_all=True, raise_immediately=True)
            _assert_note_in_scope(note, edit["track_index"], scope)
            changed += 1
        return {"changed_notes": changed}
    if operation_type == "add_notes":
        track_index = operation["track_index"]
        if track_index not in scope["track_indices"]:
            raise RevisionPlanValidationError("add_notes track is outside scope")
        for note in operation["notes"]:
            _assert_note_in_scope(note, track_index, scope)
            music_json["tracks"][track_index]["notes"].append(copy.deepcopy(note))
            changed += 1
        return {"added_notes": changed, "track_index": track_index}
    if operation_type == "remove_notes":
        grouped: dict[int, list[int]] = {}
        for target in operation["targets"]:
            track_index = target["track_index"]
            note_index = target["note_index"]
            note = music_json["tracks"][track_index]["notes"][note_index]
            _assert_note_in_scope(note, track_index, scope)
            grouped.setdefault(track_index, []).append(note_index)
        for track_index, note_indices in grouped.items():
            notes = music_json["tracks"][track_index]["notes"]
            if len(notes) - len(set(note_indices)) < 1:
                raise RevisionPlanValidationError("remove_notes cannot leave a track empty")
            for note_index in sorted(set(note_indices), reverse=True):
                del notes[note_index]
                changed += 1
        return {"removed_notes": changed}
    if operation_type == "set_program":
        track = music_json["tracks"][operation["track_index"]]
        old = {"program": track["program"], "instrument": track["instrument"]}
        track["program"] = operation["program"]
        if operation.get("instrument"):
            track["instrument"] = operation["instrument"]
        return {"track_index": operation["track_index"], "from": old, "to": {"program": track["program"], "instrument": track["instrument"]}}
    if operation_type == "add_track":
        music_json["tracks"].append(copy.deepcopy(operation["track"]))
        return {"added_track_index": len(music_json["tracks"]) - 1}
    if operation_type == "remove_track":
        removed = music_json["tracks"].pop(operation["track_index"])
        return {"removed_track_index": operation["track_index"], "removed_track_name": removed.get("name")}
    raise RevisionPlanValidationError(f"unsupported operation: {operation_type}")


def _resolved_scope(scope: dict[str, Any], source: dict[str, Any]) -> dict[str, Any]:
    return {
        "start_beat": float(scope["start_beat"]),
        "end_beat": float(scope["end_beat"]),
        "track_indices": list(dict.fromkeys(int(index) for index in scope["track_indices"])),
    }


def _operation_scope(operation: dict[str, Any], plan_scope: dict[str, Any]) -> dict[str, Any]:
    return {
        "start_beat": float(operation.get("start_beat", plan_scope["start_beat"])),
        "end_beat": float(operation.get("end_beat", plan_scope["end_beat"])),
        "track_indices": list(operation.get("track_indices", plan_scope["track_indices"])),
    }


def _iter_notes_in_scope(music_json: dict[str, Any], scope: dict[str, Any]):
    for track_index in scope["track_indices"]:
        for note_index, note in enumerate(music_json["tracks"][track_index]["notes"]):
            if scope["start_beat"] <= float(note["start"]) < scope["end_beat"]:
                yield track_index, note_index, note


def _assert_note_in_scope(note: dict[str, Any], track_index: int, scope: dict[str, Any]) -> None:
    if track_index not in scope["track_indices"]:
        raise RevisionPlanValidationError(f"track {track_index} is outside revision scope")
    start = float(note["start"])
    if not scope["start_beat"] <= start < scope["end_beat"]:
        raise RevisionPlanValidationError(f"note at beat {start:g} is outside revision scope")
    if start + float(note["duration"]) > scope["end_beat"] + 1e-6:
        raise RevisionPlanValidationError("edited/added note ends outside revision scope")


def _restore_instruments(revised: dict[str, Any], original: dict[str, Any]) -> None:
    if len(revised["tracks"]) != len(original["tracks"]):
        raise RevisionPlanValidationError("track count changed while preserve.instruments is true")
    for index, original_track in enumerate(original["tracks"]):
        revised_track = revised["tracks"][index]
        for field in ("name", "role", "instrument", "channel", "program", "is_drum"):
            revised_track[field] = copy.deepcopy(original_track[field])


def _sort_track_notes(music_json: dict[str, Any]) -> None:
    for track in music_json["tracks"]:
        track["notes"].sort(key=lambda note: (float(note["start"]), int(note["pitch"]), float(note["duration"])))


def _note_diff(original: dict[str, Any], revised: dict[str, Any]) -> dict[str, Any]:
    original_count = sum(len(track["notes"]) for track in original["tracks"])
    changed = 0
    added = 0
    removed = 0
    common_tracks = min(len(original["tracks"]), len(revised["tracks"]))
    for track_index in range(common_tracks):
        old_counter = Counter(_note_signature(note) for note in original["tracks"][track_index]["notes"])
        new_counter = Counter(_note_signature(note) for note in revised["tracks"][track_index]["notes"])
        old_unmatched = sum((old_counter - new_counter).values())
        new_unmatched = sum((new_counter - old_counter).values())
        replacements = min(old_unmatched, new_unmatched)
        changed += replacements
        removed += old_unmatched - replacements
        added += new_unmatched - replacements
    for track in revised["tracks"][common_tracks:]:
        added += len(track["notes"])
    for track in original["tracks"][common_tracks:]:
        removed += len(track["notes"])
    ratio = (changed + added + removed) / max(1, original_count)
    return {
        "source_note_count": original_count,
        "changed_existing_notes": changed,
        "added_notes": added,
        "removed_notes": removed,
        "changed_note_ratio": round(ratio, 6),
    }


def _assert_outside_scope_unchanged(
    original: dict[str, Any],
    revised: dict[str, Any],
    scope: dict[str, Any],
) -> None:
    if len(original["tracks"]) != len(revised["tracks"]):
        return
    scoped_tracks = set(scope["track_indices"])
    for track_index, original_track in enumerate(original["tracks"]):
        revised_track = revised["tracks"][track_index]
        if track_index not in scoped_tracks and original_track["notes"] != revised_track["notes"]:
            raise RevisionPlanValidationError(f"track {track_index} changed outside revision scope")
        if track_index in scoped_tracks:
            old_outside = [
                _note_signature(note) for note in original_track["notes"]
                if not scope["start_beat"] <= float(note["start"]) < scope["end_beat"]
            ]
            new_outside = [
                _note_signature(note) for note in revised_track["notes"]
                if not scope["start_beat"] <= float(note["start"]) < scope["end_beat"]
            ]
            if Counter(old_outside) != Counter(new_outside):
                raise RevisionPlanValidationError(f"track {track_index} notes outside beat scope changed")


def _note_signature(note: dict[str, Any]) -> tuple[int, float, float, int]:
    return (
        int(note["pitch"]),
        round(float(note["start"]), 6),
        round(float(note["duration"]), 6),
        int(note["velocity"]),
    )


def _validate_note_reference(
    value: Any,
    source: dict[str, Any],
    label: str,
    errors: list[str],
    require_update: bool = False,
) -> None:
    if not isinstance(value, dict):
        errors.append(f"{label} must be an object")
        return
    track_index = value.get("track_index")
    note_index = value.get("note_index")
    if not _valid_track_index(track_index, len(source["tracks"])):
        errors.append(f"{label}.track_index is invalid")
        return
    notes = source["tracks"][track_index]["notes"]
    if isinstance(note_index, bool) or not isinstance(note_index, int) or not 0 <= note_index < len(notes):
        errors.append(f"{label}.note_index is invalid")
    if require_update:
        update_fields = {"pitch", "start", "duration", "velocity"} & set(value)
        if not update_fields:
            errors.append(f"{label} must update at least one note field")
        _validate_note_values(value, float(source["total_beats"]), label, errors, require_all=False)


def _validate_note_values(
    value: Any,
    total_beats: float,
    label: str,
    errors: list[str],
    require_all: bool,
    raise_immediately: bool = False,
) -> None:
    local_errors: list[str] = []
    if not isinstance(value, dict):
        local_errors.append(f"{label} must be an object")
    else:
        required = {"pitch", "start", "duration", "velocity"}
        if require_all:
            missing = sorted(required - set(value))
            if missing:
                local_errors.append(f"{label} missing fields: {', '.join(missing)}")
        if "pitch" in value and (isinstance(value["pitch"], bool) or not isinstance(value["pitch"], int) or not 0 <= value["pitch"] <= 127):
            local_errors.append(f"{label}.pitch must be an integer between 0 and 127")
        if "velocity" in value and (isinstance(value["velocity"], bool) or not isinstance(value["velocity"], int) or not 1 <= value["velocity"] <= 127):
            local_errors.append(f"{label}.velocity must be an integer between 1 and 127")
        start = value.get("start")
        duration = value.get("duration")
        if "start" in value and (_not_finite_number(start) or not 0 <= float(start) < total_beats):
            local_errors.append(f"{label}.start must be within total_beats")
        if "duration" in value and (_not_finite_number(duration) or not 0 < float(duration) <= 32):
            local_errors.append(f"{label}.duration must be between 0 and 32")
        if start is not None and duration is not None and not _not_finite_number(start) and not _not_finite_number(duration):
            if float(start) + float(duration) > total_beats + 1e-6:
                local_errors.append(f"{label} ends after total_beats")
    if local_errors and raise_immediately:
        raise RevisionPlanValidationError("; ".join(local_errors))
    errors.extend(local_errors)


def _finite_number(value: Any, label: str, errors: list[str]) -> float | None:
    if _not_finite_number(value):
        errors.append(f"{label} must be a finite number")
        return None
    return float(value)


def _not_finite_number(value: Any) -> bool:
    return isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(float(value))


def _valid_track_index(value: Any, track_count: int) -> bool:
    return not isinstance(value, bool) and isinstance(value, int) and 0 <= value < track_count


def _non_empty_string_list(value: Any) -> bool:
    return isinstance(value, list) and bool(value) and all(isinstance(item, str) and item.strip() for item in value)
