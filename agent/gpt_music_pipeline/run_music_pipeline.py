from __future__ import annotations

import argparse
import json
import os
import re
from datetime import datetime
from pathlib import Path
from typing import Any

from gpt_music_client import SYSTEM_PROMPT, generate_music_json
from music_json_to_midi import write_midi_from_music_json
from prompt_builder import build_prompt
from render_wav import render_midi_to_wav
from validate_music_json import load_music_json, validate_music_json


ROOT = Path(__file__).resolve().parent
OUTPUTS = ROOT / "outputs"


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
) -> dict[str, Any]:
    OUTPUTS.mkdir(parents=True, exist_ok=True)
    name = _safe_output_name(output_name or f"music_{datetime.now().strftime('%Y%m%d_%H%M%S')}")

    json_path = OUTPUTS / f"{name}.json"
    midi_path = OUTPUTS / f"{name}.mid"
    wav_path = OUTPUTS / f"{name}.wav"
    prompt_txt_path = OUTPUTS / f"{name}.prompt.txt"
    prompt_json_path = OUTPUTS / f"{name}.prompt.json"
    manifest_path = OUTPUTS / f"{name}.manifest.json"
    prompt_available = False

    if input_json:
        music_json = load_music_json(input_json)
    else:
        ui_parameters: dict[str, Any] = {
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

        prompt = build_prompt(request, ui_parameters, duration)
        chosen_model = model or os.environ.get("MUSIC_MODEL") or "gpt-5.5"
        prompt_txt_path.write_text(prompt, encoding="utf-8")
        prompt_json_path.write_text(
            json.dumps(
                {
                    "version_id": name,
                    "original_request": request,
                    "ui_parameters": ui_parameters,
                    "model": chosen_model,
                    "system_prompt": SYSTEM_PROMPT,
                    "user_prompt": prompt,
                },
                ensure_ascii=False,
                indent=2,
            ),
            encoding="utf-8",
        )
        prompt_available = True
        music_json = _call_with_retry(prompt, model, timeout, max_retries=3)

    warnings = validate_music_json(music_json)
    json_path.write_text(json.dumps(music_json, ensure_ascii=False, indent=2), encoding="utf-8")

    write_midi_from_music_json(music_json, midi_path)
    render_midi_to_wav(midi_path, wav_path, trim_seconds=float(music_json["duration_seconds"]))

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
    }
    if prompt_available:
        manifest["files"]["prompt_text"] = str(prompt_txt_path.resolve())
        manifest["files"]["prompt_record"] = str(prompt_json_path.resolve())
        manifest["urls"]["prompt_text"] = f"/outputs/{prompt_txt_path.name}"
        manifest["urls"]["prompt_record"] = f"/outputs/{prompt_json_path.name}"

    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


def _call_with_retry(prompt: str, model: str | None, timeout: int, max_retries: int = 3) -> dict[str, Any]:
    import time
    last_error = None
    for attempt in range(1, max_retries + 1):
        try:
            result = generate_music_json(prompt, model=model, timeout_seconds=timeout)
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


def _safe_output_name(value: str) -> str:
    name = re.sub(r"[^A-Za-z0-9_-]+", "_", value.strip())
    return name.strip("_") or f"music_{datetime.now().strftime('%Y%m%d_%H%M%S')}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the full GPT music pipeline.")
    parser.add_argument("--request", default="", help="User music request text.")
    parser.add_argument("--duration", type=float, default=30, help="Target duration in seconds.")
    parser.add_argument("--style", default="", help="Optional music style.")
    parser.add_argument("--mood", default="", help="Optional music mood.")
    parser.add_argument("--tempo-bpm", type=int, default=None, help="Optional target tempo BPM.")
    parser.add_argument("--instruments", default="", help="Optional comma-separated instrument names.")
    parser.add_argument("--model", default=None, help="API model name.")
    parser.add_argument("--timeout", type=int, default=240, help="API timeout in seconds.")
    parser.add_argument("--output-name", default=None, help="Base output filename without extension.")
    parser.add_argument("--input-json", default=None, help="Skip GPT and convert an existing music_json file.")
    args = parser.parse_args()

    manifest = run_pipeline(
        request=args.request,
        duration=args.duration,
        style=args.style,
        mood=args.mood,
        tempo_bpm=args.tempo_bpm,
        instruments=args.instruments,
        model=args.model,
        timeout=args.timeout,
        output_name=args.output_name,
        input_json=args.input_json,
    )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
