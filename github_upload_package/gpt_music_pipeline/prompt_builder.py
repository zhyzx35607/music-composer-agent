from __future__ import annotations

import json
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parent
TEMPLATE_PATH = ROOT / "prompts" / "music_json_prompt_template.md"


def build_prompt(
    user_request: str,
    ui_parameters: dict[str, Any] | None = None,
    duration_seconds: int | float | None = None,
) -> str:
    """Build the strict GPT prompt used to request convertible music JSON."""
    params = dict(ui_parameters or {})
    duration = duration_seconds or params.get("duration_seconds") or 30

    template = TEMPLATE_PATH.read_text(encoding="utf-8")
    return (
        template.replace("{{USER_REQUEST}}", user_request.strip())
        .replace(
            "{{UI_PARAMETERS_JSON}}",
            json.dumps(params, ensure_ascii=False, indent=2),
        )
        .replace("{{DURATION_SECONDS}}", str(duration))
    )

