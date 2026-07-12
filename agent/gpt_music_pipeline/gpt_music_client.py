from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
from typing import Any


DEFAULT_BASE_URL = "https://api.openai.com/v1"
DEFAULT_MODEL = "gpt-5.5"
SYSTEM_PROMPT = "You generate strict JSON music data for a MIDI renderer. Return JSON only."


class GPTMusicClientError(RuntimeError):
    pass


def generate_music_json(
    prompt: str,
    model: str | None = None,
    base_url: str | None = None,
    api_key: str | None = None,
    timeout_seconds: int = 90,
) -> dict[str, Any]:
    """Call a Chat Completions compatible API and return parsed JSON content."""
    key = api_key or os.environ.get("MUSIC_API_KEY") or os.environ.get("OPENAI_API_KEY")
    if not key:
        raise GPTMusicClientError("Missing API key. Set MUSIC_API_KEY or OPENAI_API_KEY.")

    url = (base_url or os.environ.get("MUSIC_API_BASE_URL") or DEFAULT_BASE_URL).rstrip("/")
    chosen_model = model or os.environ.get("MUSIC_MODEL") or DEFAULT_MODEL

    payload = {
        "model": chosen_model,
        "messages": [
            {
                "role": "system",
                "content": SYSTEM_PROMPT,
            },
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.4,
        "response_format": {"type": "json_object"},
    }

    request = urllib.request.Request(
        f"{url}/chat/completions",
        data=json.dumps(payload, ensure_ascii=False).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            response_data = json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise GPTMusicClientError(f"API request failed: HTTP {exc.code} {detail[:800]}") from exc
    except urllib.error.URLError as exc:
        raise GPTMusicClientError(f"API request failed: network error {exc.reason}") from exc

    try:
        content = response_data["choices"][0]["message"]["content"]
    except (KeyError, IndexError, TypeError) as exc:
        raise GPTMusicClientError("API response did not contain choices[0].message.content") from exc

    try:
        return json.loads(content)
    except json.JSONDecodeError as exc:
        raise GPTMusicClientError(f"Model returned invalid JSON: {exc}") from exc
