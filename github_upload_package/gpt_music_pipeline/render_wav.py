from __future__ import annotations

import argparse
import subprocess
import wave
from pathlib import Path


ROOT = Path(__file__).resolve().parent
DEFAULT_FLUIDSYNTH = ROOT.parent / "tools" / "fluidsynth" / "fluidsynth-v2.5.6-win10-x64-cpp11" / "bin" / "fluidsynth.exe"
DEFAULT_SOUNDFONT = ROOT.parent / "soundfonts" / "FluidR3_GM.sf2"


def render_midi_to_wav(
    midi_path: str | Path,
    wav_path: str | Path,
    fluidsynth_path: str | Path = DEFAULT_FLUIDSYNTH,
    soundfont_path: str | Path = DEFAULT_SOUNDFONT,
    trim_seconds: float | None = None,
) -> Path:
    midi = Path(midi_path)
    wav = Path(wav_path)
    fluidsynth = Path(fluidsynth_path)
    soundfont = Path(soundfont_path)

    if not midi.exists():
        raise FileNotFoundError(f"MIDI file not found: {midi}")
    if not fluidsynth.exists():
        raise FileNotFoundError(f"FluidSynth executable not found: {fluidsynth}")
    if not soundfont.exists():
        raise FileNotFoundError(f"SoundFont file not found: {soundfont}")

    wav.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(
        [
            str(fluidsynth),
            "-ni",
            "-F",
            str(wav),
            "-r",
            "44100",
            str(soundfont),
            str(midi),
        ],
        check=True,
    )
    if trim_seconds is not None:
        _trim_wav(wav, trim_seconds)
    return wav


def _trim_wav(wav_path: Path, seconds: float) -> None:
    if seconds <= 0:
        raise ValueError("trim_seconds must be greater than 0")

    with wave.open(str(wav_path), "rb") as source:
        params = source.getparams()
        target_frames = min(source.getnframes(), round(seconds * source.getframerate()))
        frames = source.readframes(target_frames)

    with wave.open(str(wav_path), "wb") as target:
        target.setparams(params)
        target.writeframes(frames)


def main() -> None:
    parser = argparse.ArgumentParser(description="Render MIDI to WAV with FluidSynth.")
    parser.add_argument("input_midi")
    parser.add_argument("output_wav")
    parser.add_argument("--fluidsynth", default=str(DEFAULT_FLUIDSYNTH))
    parser.add_argument("--soundfont", default=str(DEFAULT_SOUNDFONT))
    parser.add_argument("--trim-seconds", type=float, default=None)
    args = parser.parse_args()

    output = render_midi_to_wav(
        args.input_midi,
        args.output_wav,
        fluidsynth_path=args.fluidsynth,
        soundfont_path=args.soundfont,
        trim_seconds=args.trim_seconds,
    )
    print(f"saved: {output}")


if __name__ == "__main__":
    main()
