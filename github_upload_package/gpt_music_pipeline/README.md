# GPT Music Pipeline

This folder contains the new GPT-based music generation pipeline.

It replaces the old Text2midi route.

## Current Scope

Step 1: GPT prompt template.

Step 2: `music_json` standard format.

Step 3: GPT API call and JSON generation.

Step 4: JSON validation.

Step 5: Convert `music_json` to MIDI.

Step 6: Render MIDI to WAV with FluidSynth.

## Files

- `prompts/music_json_prompt_template.md`: prompt template for asking GPT to generate structured music data.
- `schemas/music_json_schema.json`: JSON Schema used to validate GPT output before converting it to MIDI.
- `examples/api_smoke_5s_expected_shape.json`: local expected-shape sample for smoke testing.
- `outputs/`: the only runtime output folder for generated JSON, MIDI, and WAV files.
- `gpt_music_client.py`: Chat Completions compatible API client.
- `generate_music_json.py`: one-command GPT generation and validation script.
- `validate_music_json.py`: local validator for generated music JSON.
- `music_json_to_midi.py`: converts validated `music_json` into a standard MIDI file.
- `render_wav.py`: renders MIDI into WAV with FluidSynth and can trim the result to a target duration.
- `run_music_pipeline.py`: one-command pipeline entry for backend integration.

## Pipeline

```text
frontend request
-> prompt builder
-> GPT API
-> music_json
-> JSON validation
-> MIDI renderer
-> FluidSynth WAV renderer
-> frontend audio playback
```

## Why JSON

GPT is the generator, but the program still needs a stable contract.

The JSON format tells GPT exactly what to return and tells the backend exactly what to validate and convert.
Without a fixed JSON format, GPT may return prose, incomplete music descriptions, invalid notes, or data that cannot be converted into MIDI.

## Generate Music JSON

Set the API key as an environment variable. Do not write the key into source code.

```powershell
$env:MUSIC_API_KEY = "your_api_key"
$env:MUSIC_API_BASE_URL = "https://jeniya.cn/v1"
```

Then run:

```powershell
F:\music\.venv\Scripts\python.exe .\generate_music_json.py `
  --request "Generate a 5-second pure instrumental piano test clip." `
  --duration 5 `
  --style "simple piano" `
  --mood "calm" `
  --model "gpt-5.5" `
  --timeout 240 `
  --output .\outputs\generated_api_smoke_5s.json
```

For other OpenAI-compatible third-party providers, set:

```powershell
$env:MUSIC_API_BASE_URL = "https://your-provider.example/v1"
$env:MUSIC_MODEL = "your-model-name"
```

## Validate Music JSON

```powershell
F:\music\.venv\Scripts\python.exe .\validate_music_json.py .\outputs\generated_api_smoke_5s.json
```

Expected output:

```text
valid
```

## Convert JSON To MIDI

```powershell
F:\music\.venv\Scripts\python.exe .\music_json_to_midi.py `
  .\outputs\generated_api_smoke_5s.json `
  .\outputs\generated_api_smoke_5s.mid
```

## Render MIDI To WAV

```powershell
F:\music\.venv\Scripts\python.exe .\render_wav.py `
  .\outputs\generated_api_smoke_5s.mid `
  .\outputs\generated_api_smoke_5s.wav `
  --trim-seconds 5
```

## Run Full Pipeline

Use this command when the backend needs one entry point from user request to WAV.

```powershell
F:\music\.venv\Scripts\python.exe .\run_music_pipeline.py `
  --request "Generate a 30-second pure instrumental platform demo track." `
  --duration 30 `
  --style "cinematic game background music" `
  --mood "uplifting, polished, gentle energy" `
  --model "gpt-5.5" `
  --timeout 240 `
  --output-name "version_001"
```

The script prints a manifest containing local file paths and frontend URLs such as:

```json
{
  "urls": {
    "wav": "/outputs/version_001.wav"
  }
}
```
