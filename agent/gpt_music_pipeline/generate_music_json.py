from __future__ import annotations

import argparse
import json
from pathlib import Path

from gpt_music_client import generate_music_json
from prompt_builder import build_prompt
from validate_music_json import validate_music_json


ROOT = Path(__file__).resolve().parent
OUTPUTS = ROOT / "outputs"


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate and validate GPT music JSON.")
    parser.add_argument("--request", required=True, help="User music request text.")
    parser.add_argument("--duration", type=float, default=30, help="Target duration in seconds.")
    parser.add_argument("--style", default="", help="Optional music style.")
    parser.add_argument("--mood", default="", help="Optional music mood.")
    parser.add_argument("--model", default=None, help="API model name.")
    parser.add_argument("--timeout", type=int, default=90, help="API timeout in seconds.")
    parser.add_argument(
        "--output",
        default=str(OUTPUTS / "generated_music.json"),
        help="Output JSON path.",
    )
    args = parser.parse_args()

    ui_parameters = {
        "duration_seconds": args.duration,
        "pure_instrumental": True,
    }
    if args.style:
        ui_parameters["style"] = args.style
    if args.mood:
        ui_parameters["mood"] = args.mood

    prompt = build_prompt(args.request, ui_parameters, args.duration)
    music_json = generate_music_json(prompt, model=args.model, timeout_seconds=args.timeout)
    warnings = validate_music_json(music_json)

    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(music_json, ensure_ascii=False, indent=2), encoding="utf-8")

    print(f"saved: {output_path}")
    print("validation: ok")
    for warning in warnings:
        print(f"warning: {warning}")


if __name__ == "__main__":
    main()
