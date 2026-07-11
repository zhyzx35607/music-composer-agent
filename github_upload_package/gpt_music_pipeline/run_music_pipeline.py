from __future__ import annotations

import argparse
import json
import re
from datetime import datetime
from pathlib import Path
from typing import Any

from gpt_music_client import generate_music_json
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
    manifest_path = OUTPUTS / f"{name}.manifest.json"

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

        prompt = build_prompt(request, ui_parameters, duration)
        music_json = generate_music_json(prompt, model=model, timeout_seconds=timeout)

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
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    return manifest


def _safe_output_name(value: str) -> str:
    name = re.sub(r"[^A-Za-z0-9_-]+", "_", value.strip())
    return name.strip("_") or f"music_{datetime.now().strftime('%Y%m%d_%H%M%S')}"


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the full GPT music pipeline.")
    parser.add_argument("--request", default="", help="User music request text.")
    parser.add_argument("--duration", type=float, default=30, help="Target duration in seconds.")
    parser.add_argument("--style", default="", help="Optional music style.")
    parser.add_argument("--mood", default="", help="Optional music mood.")
    parser.add_argument("--model", default=None, help="API model name.")
    parser.add_argument("--timeout", type=int, default=240, help="API timeout in seconds.")
    parser.add_argument("--output-name", default=None, help="Base output filename without extension.")
    parser.add_argument("--input-json", default=None, help="Skip GPT and convert an existing music_json file.")
    args = parser.parse_args()

    if not args.input_json and not args.request:
        parser.error("--request is required unless --input-json is provided")

    manifest = run_pipeline(
        request=args.request,
        duration=args.duration,
        style=args.style,
        mood=args.mood,
        model=args.model,
        timeout=args.timeout,
        output_name=args.output_name,
        input_json=args.input_json,
    )
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
