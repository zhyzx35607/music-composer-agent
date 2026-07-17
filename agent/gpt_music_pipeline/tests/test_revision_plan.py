from __future__ import annotations

import copy
import json
import sys
import tempfile
import unittest
import wave
from pathlib import Path
from unittest.mock import patch


PIPELINE_DIR = Path(__file__).resolve().parents[1]
if str(PIPELINE_DIR) not in sys.path:
    sys.path.insert(0, str(PIPELINE_DIR))

from revision_plan import (  # noqa: E402
    RevisionPlanValidationError,
    apply_revision_plan,
    build_revision_plan_prompt,
    deterministic_tempo_plan,
    validate_revision_plan,
)
import run_music_pipeline  # noqa: E402
from score_to_gpt_context import score_to_music_json  # noqa: E402
from validate_music_json import validate_music_json  # noqa: E402


class RevisionPlanTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.source_path = PIPELINE_DIR / "examples" / "sample_valid_boss_battle_30s.json"
        cls.source = json.loads(cls.source_path.read_text(encoding="utf-8"))

    def test_local_mood_plan_changes_only_selected_notes(self) -> None:
        source = copy.deepcopy(self.source)
        plan = {
            "version": "1.0",
            "intent": "mood_adjustment",
            "summary": "让开头主旋律稍微更欢快，保留其余内容。",
            "strength": 0.25,
            "scope": {"start_beat": 0, "end_beat": 8, "track_indices": [0]},
            "preserve": {
                "duration": True,
                "time_signature": True,
                "instruments": True,
                "outside_scope": True,
            },
            "max_changed_note_ratio": 0.2,
            "metadata_updates": {"mood": ["cheerful", "energetic"]},
            "operations": [
                {"type": "velocity_shift", "amount": 6},
                {"type": "duration_scale", "factor": 0.85},
            ],
        }

        revised, report = apply_revision_plan(source, plan)

        self.assertEqual(source, self.source, "source music_json must remain immutable")
        self.assertEqual(revised["mood"], ["cheerful", "energetic"])
        self.assertEqual(revised["tracks"][0]["notes"][8:], self.source["tracks"][0]["notes"][8:])
        self.assertEqual(revised["tracks"][1:], self.source["tracks"][1:])
        self.assertEqual(revised["duration_seconds"], self.source["duration_seconds"])
        self.assertEqual(revised["tracks"][0]["program"], self.source["tracks"][0]["program"])
        self.assertEqual(report["changed_existing_notes"], 8)
        self.assertLessEqual(report["changed_note_ratio"], plan["max_changed_note_ratio"])

    def test_operation_cannot_escape_plan_scope(self) -> None:
        plan = {
            "version": "1.0",
            "intent": "local_edit",
            "summary": "只修改开头。",
            "strength": 0.2,
            "scope": {"start_beat": 0, "end_beat": 8, "track_indices": [0]},
            "preserve": {
                "duration": True,
                "time_signature": True,
                "instruments": True,
                "outside_scope": True,
            },
            "max_changed_note_ratio": 0.2,
            "metadata_updates": {},
            "operations": [
                {
                    "type": "velocity_shift",
                    "amount": 5,
                    "start_beat": 0,
                    "end_beat": 16,
                    "track_indices": [0],
                }
            ],
        }

        with self.assertRaises(RevisionPlanValidationError):
            validate_revision_plan(plan, self.source)

    def test_prompt_adds_stable_indices_without_mutating_source(self) -> None:
        source = copy.deepcopy(self.source)
        prompt = build_revision_plan_prompt("欢快一点", source, {})

        self.assertIn('"track_index":0', prompt)
        self.assertIn('"note_index":0', prompt)
        self.assertNotIn("track_index", source["tracks"][0])
        self.assertNotIn("note_index", source["tracks"][0]["notes"][0])

    def test_note_edit_uses_source_track_and_note_indices(self) -> None:
        plan = {
            "version": "1.0",
            "intent": "melody_edit",
            "summary": "只提高一个旋律音。",
            "strength": 0.1,
            "scope": {"start_beat": 0, "end_beat": 4, "track_indices": [0]},
            "preserve": {
                "duration": True,
                "time_signature": True,
                "instruments": True,
                "outside_scope": True,
            },
            "max_changed_note_ratio": 0.05,
            "metadata_updates": {},
            "operations": [
                {
                    "type": "note_edits",
                    "edits": [{"track_index": 0, "note_index": 1, "pitch": 74}],
                }
            ],
        }

        revised, report = apply_revision_plan(self.source, plan)

        self.assertEqual(revised["tracks"][0]["notes"][1]["pitch"], 74)
        self.assertEqual(report["changed_existing_notes"], 1)

    def test_deterministic_tempo_plan_preserves_notes_and_instruments(self) -> None:
        plan = deterministic_tempo_plan(self.source, 144)
        revised, report = apply_revision_plan(self.source, plan)

        self.assertEqual(revised["tempo_bpm"], 144)
        self.assertEqual(revised["duration_seconds"], 25.0)
        self.assertEqual(revised["tracks"], self.source["tracks"])
        self.assertEqual(report["changed_note_ratio"], 0)

    def test_musicxml_can_become_editable_music_json(self) -> None:
        music_json = score_to_music_json(
            PIPELINE_DIR / "examples" / "Canon_in_D_Pachelbel_piano_test.musicxml"
        )

        warnings = validate_music_json(music_json)
        self.assertIsInstance(warnings, list)
        self.assertGreater(len(music_json["tracks"]), 0)
        self.assertGreater(sum(len(track["notes"]) for track in music_json["tracks"]), 0)

    def test_revision_pipeline_emits_plan_json_midi_and_wav(self) -> None:
        plan = {
            "version": "1.0",
            "intent": "mood_adjustment",
            "summary": "局部增强开头旋律的明快力度。",
            "strength": 0.2,
            "scope": {"start_beat": 0, "end_beat": 4, "track_indices": [0]},
            "preserve": {
                "duration": True,
                "time_signature": True,
                "instruments": True,
                "outside_scope": True,
            },
            "max_changed_note_ratio": 0.1,
            "metadata_updates": {"mood": ["cheerful"]},
            "operations": [{"type": "velocity_shift", "amount": 5}],
        }

        def fake_render(_midi_path, wav_path, **_kwargs):
            output = Path(wav_path)
            with wave.open(str(output), "wb") as target:
                target.setnchannels(1)
                target.setsampwidth(2)
                target.setframerate(8000)
                target.writeframes(b"\x00\x00" * 80)
            return output

        with tempfile.TemporaryDirectory() as temp_dir:
            with (
                patch.object(run_music_pipeline, "OUTPUTS", Path(temp_dir)),
                patch.object(run_music_pipeline, "_generate_revision_plan_with_retry", return_value=plan),
                patch.object(run_music_pipeline, "render_midi_to_wav", side_effect=fake_render),
            ):
                manifest = run_music_pipeline.run_pipeline(
                    request="帮我把开头改得欢快一点",
                    duration=30,
                    output_name="revision_test",
                    mode="revise",
                    revision_source_json=self.source_path,
                )

            for key in ("revision_plan", "music_json", "midi", "wav", "ai_record", "manifest"):
                self.assertTrue(Path(manifest["files"][key]).exists(), key)
            saved_plan = json.loads(Path(manifest["files"]["revision_plan"]).read_text(encoding="utf-8"))
            saved_music = json.loads(Path(manifest["files"]["music_json"]).read_text(encoding="utf-8"))
            self.assertEqual(saved_plan, plan)
            self.assertEqual(saved_music["mood"], ["cheerful"])
            self.assertNotIn("music_json", saved_plan)


if __name__ == "__main__":
    unittest.main()
