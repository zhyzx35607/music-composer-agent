from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import wave
from datetime import datetime
from pathlib import Path
from typing import Any

from gpt_music_client import SYSTEM_PROMPT, generate_music_json
from music_json_to_midi import write_midi_from_music_json
from prompt_builder import build_prompt
from render_wav import DEFAULT_FLUIDSYNTH, DEFAULT_SOUNDFONT, render_midi_to_wav
from revision_plan import (
    RevisionPlanValidationError,
    apply_revision_plan,
    build_revision_plan_prompt,
    deterministic_tempo_plan,
    save_revision_plan,
    validate_revision_plan,
)
from score_to_gpt_context import convert_score_to_midi, score_to_context, score_to_music_json
from validate_music_json import load_music_json, validate_music_json


ROOT = Path(__file__).resolve().parent
OUTPUTS = ROOT / "outputs"

REVISION_PLAN_SYSTEM_PROMPT = (
    "You create strict JSON edit plans for an existing symbolic music score. "
    "Return a revision_plan object only. Never return a complete music_json."
)


def run_pipeline(
    request: str,
    duration: float,
    style: str = "",
    mood: str = "",
    tempo_bpm: int | None = None,
    instruments: str = "",
    model: str | None = None,
    timeout: int = 240,
    output_name: str | None = None,
    input_json: str | Path | None = None,
    input_score: str | Path | None = None,
    mode: str = "generate",
    preserve_source_tempo: bool = False,
    lock_programs_from_json: str | Path | None = None,
    lock_programs_from_score: str | Path | None = None,
    revision_source_json: str | Path | None = None,
    fluidsynth_path: str | Path | None = None,
    soundfont_path: str | Path | None = None,
) -> dict[str, Any]:
    OUTPUTS.mkdir(parents=True, exist_ok=True)
    name = _safe_output_name(output_name or f"music_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

    json_path = OUTPUTS / f"{name}.json"
    midi_path = OUTPUTS / f"{name}.mid"
    wav_path = OUTPUTS / f"{name}.wav"
    prompt_txt_path = OUTPUTS / f"{name}.prompt.txt"
    prompt_json_path = OUTPUTS / f"{name}.prompt.json"
    ai_record_path = OUTPUTS / f"{name}.ai_record.json"
    manifest_path = OUTPUTS / f"{name}.manifest.json"
    revision_plan_path = OUTPUTS / f"{name}.revision_plan.json"
    prompt_available = False
    ui_parameters: dict[str, Any] = {}
    prompt = ""
    system_prompt = SYSTEM_PROMPT
    chosen_model = model or os.environ.get("MUSIC_MODEL") or "gpt-5.5"
    raw_ai_response: dict[str, Any] | None = None
    revision_plan: dict[str, Any] | None = None
    source_music_json: dict[str, Any] | None = None
    change_reason = None
    parameter_diff = None

    if input_score:
        return _run_direct_score_conversion(
            input_score=input_score,
            output_name=name,
            json_path=json_path,
            midi_path=midi_path,
            wav_path=wav_path,
            manifest_path=manifest_path,
            tempo_bpm=None if preserve_source_tempo else tempo_bpm,
            fluidsynth_path=fluidsynth_path,
            soundfont_path=soundfont_path,
        )

    if input_json:
        source_music_json = load_music_json(input_json)
        target_tempo = int(tempo_bpm or source_music_json["tempo_bpm"])
        revision_plan = deterministic_tempo_plan(source_music_json, target_tempo)
        music_json, parameter_diff = apply_revision_plan(source_music_json, revision_plan)
        change_reason = f"确定性处理：{revision_plan['summary']}"
        save_revision_plan(revision_plan, revision_plan_path)
    else:
        ui_parameters = {
            "duration_seconds": duration,
            "pure_instrumental": True,
        }
        if style:
            ui_parameters["style"] = style
        if mood:
            ui_parameters["mood"] = mood
        if tempo_bpm:
            ui_parameters["tempo_bpm"] = tempo_bpm
        if instruments:
            ui_parameters["instruments"] = [
                item.strip() for item in instruments.split(",") if item.strip()
            ]

        if mode == "revise":
            if not revision_source_json:
                raise ValueError("revision mode requires --revision-source-json")
            source_music_json = load_music_json(revision_source_json)
            prompt = build_revision_plan_prompt(request, source_music_json, ui_parameters)
            system_prompt = REVISION_PLAN_SYSTEM_PROMPT
        else:
            prompt = build_prompt(request, ui_parameters, duration)
        prompt_txt_path.write_text(prompt, encoding="utf-8")
        prompt_json_path.write_text(
            json.dumps(
                {
                    "version_id": name,
                    "original_request": request,
                    "ui_parameters": ui_parameters,
                    "model": chosen_model,
                    "system_prompt": system_prompt,
                    "user_prompt": prompt,
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        prompt_available = True

        if mode == "revise":
            if source_music_json is None:
                raise ValueError("revision source music_json was not loaded")
            revision_plan = _generate_revision_plan_with_retry(
                prompt,
                source_music_json,
                model,
                timeout,
                max_plan_attempts=3,
            )
            raw_ai_response = revision_plan
            music_json, parameter_diff = apply_revision_plan(source_music_json, revision_plan)
            change_reason = revision_plan["summary"]
            save_revision_plan(revision_plan, revision_plan_path)
        else:
            gpt_output = _call_with_retry(prompt, model, timeout, max_retries=3)
            raw_ai_response = gpt_output
            music_json = gpt_output

    locked_programs = _load_program_locks(lock_programs_from_json, lock_programs_from_score)
    if locked_programs:
        lock_diff = _apply_program_locks(music_json, locked_programs)
        if lock_diff:
            if isinstance(parameter_diff, dict):
                parameter_diff["program_locks"] = lock_diff
            elif parameter_diff is None:
                parameter_diff = {"program_locks": lock_diff}
            if change_reason:
                change_reason = f"{change_reason} 未要求更换乐器，已自动保持原文件/上一版的 MIDI program。"
            else:
                change_reason = "未要求更换乐器，已自动保持原文件/上一版的 MIDI program。"

    warnings = validate_music_json(music_json)
    json_path.write_text(json.dumps(music_json, ensure_ascii=False, indent=2), encoding="utf-8")

    write_midi_from_music_json(music_json, midi_path)
    render_midi_to_wav(
        midi_path,
        wav_path,
        fluidsynth_path=fluidsynth_path or DEFAULT_FLUIDSYNTH,
        soundfont_path=soundfont_path or DEFAULT_SOUNDFONT,
        trim_seconds=float(music_json["duration_seconds"]),
    )

    if prompt_available:
        ai_record = {
            "version_id": name,
            "record_type": "ai_music_generation_evidence",
            "mode": mode,
            "created_at": datetime.now().isoformat(timespec="seconds"),
            "model": chosen_model,
            "conversation": {
                "system_message": system_prompt,
                "user_original_request": request,
                "ui_parameters": ui_parameters,
                "final_prompt_sent_to_ai": prompt,
                "ai_response_raw": raw_ai_response,
                "ai_response_revision_plan": revision_plan,
                "ai_response_music_json": music_json,
                "change_reason": change_reason,
                "parameter_diff": parameter_diff,
            },
            "generated_files": {
                "music_json": json_path.name,
                "midi": midi_path.name,
                "wav": wav_path.name,
                "prompt_text": prompt_txt_path.name,
                "prompt_record": prompt_json_path.name,
                "manifest": manifest_path.name,
            },
            "file_hashes_sha256": {
                "music_json": _sha256_file(json_path),
                "midi": _sha256_file(midi_path),
                "wav": _sha256_file(wav_path),
                "prompt_text": _sha256_file(prompt_txt_path),
                "prompt_record": _sha256_file(prompt_json_path),
            },
        }
        if revision_plan is not None:
            ai_record["generated_files"]["revision_plan"] = revision_plan_path.name
            ai_record["file_hashes_sha256"]["revision_plan"] = _sha256_file(revision_plan_path)
        ai_record_path.write_text(
            json.dumps(ai_record, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    manifest = {
        "success": True,
        "version_id": name,
        "title": music_json["title"],
        "duration_seconds": music_json["duration_seconds"],
        "tempo_bpm": music_json["tempo_bpm"],
        "style": music_json["style"],
        "warnings": warnings,
        "files": {
            "music_json": str(json_path.resolve()),
            "midi": str(midi_path.resolve()),
            "wav": str(wav_path.resolve()),
            "manifest": str(manifest_path.resolve()),
        },
        "urls": {
            "music_json": f"/outputs/{json_path.name}",
            "midi": f"/outputs/{midi_path.name}",
            "wav": f"/outputs/{wav_path.name}",
            "manifest": f"/outputs/{manifest_path.name}",
        },
        "change_reason": change_reason,
        "parameter_diff": parameter_diff,
    }
    if prompt_available:
        manifest["files"]["prompt_text"] = str(prompt_txt_path.resolve())
        manifest["files"]["prompt_record"] = str(prompt_json_path.resolve())
        manifest["files"]["ai_record"] = str(ai_record_path.resolve())
        manifest["urls"]["prompt_text"] = f"/outputs/{prompt_txt_path.name}"
        manifest["urls"]["prompt_record"] = f"/outputs/{prompt_json_path.name}"
        manifest["urls"]["ai_record"] = f"/outputs/{ai_record_path.name}"
    if revision_plan is not None:
        manifest["files"]["revision_plan"] = str(revision_plan_path.resolve())
        manifest["urls"]["revision_plan"] = f"/outputs/{revision_plan_path.name}"

    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


def _call_with_retry(
    prompt: str,
    model: str | None,
    timeout: int,
    max_retries: int = 3,
    system_prompt: str | None = None,
) -> dict[str, Any]:
    import time
    last_error = None
    for attempt in range(1, max_retries + 1):
        try:
            result = generate_music_json(
                prompt,
                model=model,
                timeout_seconds=timeout,
                system_prompt=system_prompt,
            )
            if attempt > 1:
                print(f"[retry] GPT API succeeded on attempt {attempt}", file=__import__("sys").stderr)
            return result
        except Exception as e:
            last_error = e
            if attempt < max_retries:
                wait = 5 * attempt
                print(f"[retry] GPT API attempt {attempt}/{max_retries} failed: {e} — retrying in {wait}s",
                      file=__import__("sys").stderr)
                time.sleep(wait)
    raise last_error  # type: ignore[misc]


def _generate_revision_plan_with_retry(
    prompt: str,
    source_music_json: dict[str, Any],
    model: str | None,
    timeout: int,
    max_plan_attempts: int,
) -> dict[str, Any]:
    current_prompt = prompt
    last_error: Exception | None = None
    for attempt in range(1, max_plan_attempts + 1):
        result = _call_with_retry(
            current_prompt,
            model,
            timeout,
            max_retries=2,
            system_prompt=REVISION_PLAN_SYSTEM_PROMPT,
        )
        if isinstance(result, dict) and isinstance(result.get("revision_plan"), dict):
            result = result["revision_plan"]
        try:
            validate_revision_plan(result, source_music_json)
            return result
        except (RevisionPlanValidationError, TypeError) as exc:
            last_error = exc
            print(
                f"[revision-plan] invalid plan on attempt {attempt}/{max_plan_attempts}: {exc}",
                file=__import__("sys").stderr,
            )
            current_prompt = (
                prompt
                + "\n\nYOUR PREVIOUS PLAN WAS INVALID:\n"
                + str(exc)
                + "\nReturn a corrected revision_plan JSON object only."
            )
    raise RevisionPlanValidationError(f"GPT failed to produce a valid revision plan: {last_error}")


def _run_direct_score_conversion(
    input_score: str | Path,
    output_name: str,
    json_path: Path,
    midi_path: Path,
    wav_path: Path,
    manifest_path: Path,
    tempo_bpm: int | None = None,
    fluidsynth_path: str | Path | None = None,
    soundfont_path: str | Path | None = None,
) -> dict[str, Any]:
    source = Path(input_score)
    if not source.exists():
        raise FileNotFoundError(f"Input score file not found: {source}")

    score_context_path = OUTPUTS / f"{output_name}.score_context.json"
    context = score_to_context(source, max_events=4000)
    score_context_path.write_text(
        json.dumps(context, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    music_json = score_to_music_json(source, tempo_bpm=tempo_bpm)
    json_path.write_text(json.dumps(music_json, ensure_ascii=False, indent=2), encoding="utf-8")

    if source.suffix.lower() in {".mid", ".midi"} and tempo_bpm is None:
        midi_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, midi_path)
    else:
        convert_score_to_midi(source, midi_path, tempo_bpm=tempo_bpm)

    render_midi_to_wav(
        midi_path,
        wav_path,
        fluidsynth_path=fluidsynth_path or DEFAULT_FLUIDSYNTH,
        soundfont_path=soundfont_path or DEFAULT_SOUNDFONT,
        trim_seconds=None,
    )

    duration_seconds = _wav_duration_seconds(wav_path)
    title = context.get("title") or source.stem
    source_tempo_bpm = context.get("tempo_bpm")
    applied_tempo_bpm = tempo_bpm

    manifest = {
        "success": True,
        "version_id": output_name,
        "mode": "direct_score_conversion",
        "title": title,
        "description": "Direct score-to-audio conversion. GPT composition was not used.",
        "duration_seconds": duration_seconds,
        "tempo_bpm": applied_tempo_bpm or source_tempo_bpm,
        "style": "direct score conversion",
        "warnings": [],
        "files": {
            "source_score": str(source.resolve()),
            "score_context": str(score_context_path.resolve()),
            "music_json": str(json_path.resolve()),
            "midi": str(midi_path.resolve()),
            "wav": str(wav_path.resolve()),
            "manifest": str(manifest_path.resolve()),
        },
        "urls": {
            "score_context": f"/outputs/{score_context_path.name}",
            "music_json": f"/outputs/{json_path.name}",
            "midi": f"/outputs/{midi_path.name}",
            "wav": f"/outputs/{wav_path.name}",
            "manifest": f"/outputs/{manifest_path.name}",
        },
        "change_reason": (
            f"确定性处理：直接转换上传乐谱文件，并将速度设置为 {applied_tempo_bpm} BPM。"
            if applied_tempo_bpm else
            "确定性处理：直接转换上传乐谱文件，未调用 GPT。"
        ),
        "parameter_diff": (
            {"tempo": {"from": source_tempo_bpm, "to": applied_tempo_bpm}, "mode": "direct_score_conversion_with_tempo"}
            if applied_tempo_bpm else
            {"mode": "direct_score_conversion"}
        ),
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


def _apply_tempo_to_music_json(music_json: dict[str, Any], tempo_bpm: int) -> None:
    music_json["tempo_bpm"] = int(tempo_bpm)
    total_beats = float(music_json.get("total_beats") or 0)
    if total_beats > 0:
        music_json["duration_seconds"] = round(total_beats * 60 / int(tempo_bpm), 3)


def _load_program_locks(
    lock_programs_from_json: str | Path | None,
    lock_programs_from_score: str | Path | None,
) -> list[dict[str, Any]]:
    if lock_programs_from_json:
        try:
            source_json = load_music_json(lock_programs_from_json)
            return [
                {
                    "index": index,
                    "name": track.get("name"),
                    "instrument": track.get("instrument"),
                    "program": track.get("program"),
                    "channel": track.get("channel"),
                    "is_drum": track.get("is_drum", False),
                }
                for index, track in enumerate(source_json.get("tracks", []))
                if track.get("program") is not None
            ]
        except Exception as exc:
            print(f"[program-lock] failed to read JSON locks: {exc}", file=__import__("sys").stderr)
    if lock_programs_from_score:
        try:
            context = score_to_context(lock_programs_from_score, max_events=4000)
            locks = []
            for part in context.get("parts", []):
                program = part.get("midi_program")
                if program is None:
                    continue
                locks.append(
                    {
                        "index": part.get("part_index", len(locks)),
                        "name": part.get("name"),
                        "instrument": part.get("instrument"),
                        "program": program,
                        "channel": part.get("midi_channel"),
                        "is_drum": False,
                    }
                )
            return locks
        except Exception as exc:
            print(f"[program-lock] failed to read score locks: {exc}", file=__import__("sys").stderr)
    return []


def _apply_program_locks(music_json: dict[str, Any], locks: list[dict[str, Any]]) -> list[dict[str, Any]]:
    tracks = music_json.get("tracks", [])
    if not isinstance(tracks, list):
        return []

    diffs: list[dict[str, Any]] = []
    for index, lock in enumerate(locks):
        if index >= len(tracks):
            break
        track = tracks[index]
        if not isinstance(track, dict) or bool(track.get("is_drum")):
            continue
        old_program = track.get("program")
        old_instrument = track.get("instrument")
        locked_program = lock.get("program")
        if locked_program is None:
            continue
        if old_program != locked_program:
            track["program"] = int(locked_program)
            if lock.get("instrument"):
                track["instrument"] = lock["instrument"]
            diffs.append(
                {
                    "track_index": index,
                    "track_name": track.get("name") or lock.get("name"),
                    "program": {"from": old_program, "to": int(locked_program)},
                    "instrument": {"from": old_instrument, "to": track.get("instrument")},
                }
            )
    return diffs


def _wav_duration_seconds(path: Path) -> float:
    with wave.open(str(path), "rb") as wav:
        return round(wav.getnframes() / wav.getframerate(), 3)


def _safe_output_name(value: str) -> str:
    name = re.sub(r"[^A-Za-z0-9_-]+", "_", value.strip())
    return name.strip("_") or f"music_{datetime.now().strftime('%Y%m%d_%H%M%S')}"


def _sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the full GPT music pipeline.")
    parser.add_argument("--request", default="", help="User music request text.")
    parser.add_argument("--request-file", default=None, help="Read user music request text from a UTF-8 file.")
    parser.add_argument("--duration", type=float, default=30, help="Target duration in seconds.")
    parser.add_argument("--style", default="", help="Optional music style.")
    parser.add_argument("--mood", default="", help="Optional music mood.")
    parser.add_argument("--tempo-bpm", type=int, default=None, help="Optional target tempo BPM.")
    parser.add_argument("--instruments", default="", help="Optional comma-separated instrument names.")
    parser.add_argument("--model", default=None, help="API model name.")
    parser.add_argument("--timeout", type=int, default=240, help="API timeout in seconds.")
    parser.add_argument("--output-name", default=None, help="Base output filename without extension.")
    parser.add_argument("--input-json", default=None, help="Skip GPT and convert an existing music_json file.")
    parser.add_argument("--input-score", default=None,
                        help="Skip GPT and directly convert a MusicXML/MXL/MIDI score file to MIDI/WAV.")
    parser.add_argument("--preserve-source-tempo", action="store_true",
                        help="When --input-score is used, do not force --tempo-bpm onto the source score.")
    parser.add_argument("--lock-programs-from-json", default=None,
                        help="After GPT output, force track program values to match this source music_json.")
    parser.add_argument("--lock-programs-from-score", default=None,
                        help="After GPT output, force track program values to match this source score file.")
    parser.add_argument("--revision-source-json", default=None,
                        help="Existing music_json to modify when --mode revise is used.")
    parser.add_argument("--fluidsynth", default=None, help="Optional FluidSynth executable path.")
    parser.add_argument("--soundfont", default=None, help="Optional SoundFont .sf2 path.")
    parser.add_argument("--mode", default="generate", choices=["generate", "revise"],
                        help="Pipeline mode: generate (first creation) or revise (feedback revision).")
    args = parser.parse_args()

    request = args.request
    if args.request_file:
        request = Path(args.request_file).read_text(encoding="utf-8")

    manifest = run_pipeline(
        request=request,
        duration=args.duration,
        style=args.style,
        mood=args.mood,
        tempo_bpm=args.tempo_bpm,
        instruments=args.instruments,
        model=args.model,
        timeout=args.timeout,
        output_name=args.output_name,
        input_json=args.input_json,
        input_score=args.input_score,
        mode=args.mode,
        preserve_source_tempo=args.preserve_source_tempo,
        lock_programs_from_json=args.lock_programs_from_json,
        lock_programs_from_score=args.lock_programs_from_score,
        revision_source_json=args.revision_source_json,
        fluidsynth_path=args.fluidsynth,
        soundfont_path=args.soundfont,
    )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
