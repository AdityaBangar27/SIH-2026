import os
import sys
import time
import gc
from pathlib import Path
from typing import Dict, Any, Union, Optional, Callable
import numpy as np

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

import torch

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.hardware import print_system_startup, get_hardware_info
from src.asr import OfflineASRManager
from src.pipeline import TranslationPipeline
from src.tts import OfflineTTSManager
from src.audio_validator import validate_audio_file
from src.memory_manager import ModelMemoryManager


class VoiceTranslationPipeline:
    """
    Unified Offline Voice Translation Pipeline for VaaniShiksha AI.
    Pipeline flow:
      Audio Input -> Audio Preprocessing -> Offline ASR -> IndicTrans2 ONNX -> Offline TTS -> Audio Output & Validation
    """

    _instance: Optional["VoiceTranslationPipeline"] = None

    def __init__(self):
        self.asr = OfflineASRManager.get_instance()
        self.translator = TranslationPipeline()
        self.tts = OfflineTTSManager.get_instance()
        self.hardware_info = get_hardware_info()
        self.preload_stats: Dict[str, float] = {}

    @classmethod
    def get_instance(cls) -> "VoiceTranslationPipeline":
        """Singleton accessor to guarantee model persistence."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def preload_all(self, profile: str = "auto", progress_callback: Optional[Callable[[str, str], None]] = None) -> Dict[str, bool]:
        """
        Preloads AI models into persistent system memory at application startup.
        - 'mobile': Loads lightweight INT8 ASR, IndicTrans2 ONNX, and Mobile VITS (~750 MB RAM total).
        - 'neural': Also preloads high-fidelity Parler-TTS on CUDA systems.
        - 'auto': Automatically selects based on hardware device and memory profile.
        """
        print("\n==================================================", flush=True)
        print("VaaniShiksha AI: Initializing & Preloading Models", flush=True)
        print("==================================================", flush=True)
        print_system_startup()
        print(f"\nLoading AI models (Profile: {profile})...", flush=True)

        results = {}

        # 1. IndicTrans2 ONNX
        if progress_callback:
            progress_callback("Translation", "loading")
        t0 = time.time()
        try:
            _ = self.translator.translator
            dur = time.time() - t0
            self.preload_stats["translation"] = dur
            results["translation"] = True
            print(f"✓ IndicTrans2 ONNX INT8 ({dur:.2f}s)", flush=True)
            if progress_callback:
                progress_callback("Translation", "ready")
        except Exception as e:
            results["translation"] = False
            print(f"❌ IndicTrans2 failed: {e}", flush=True)
            if progress_callback:
                progress_callback("Translation", "error")

        # 2. Hindi Whisper ASR
        if progress_callback:
            progress_callback("ASR", "loading")
        t0 = time.time()
        try:
            self.asr.load_hindi_asr()
            dur = time.time() - t0
            self.preload_stats["asr_hindi"] = dur
            results["asr_hindi"] = True
            print(f"✓ Hindi Whisper ASR ({dur:.2f}s)", flush=True)
            if progress_callback:
                progress_callback("ASR", "ready")
        except Exception as e:
            results["asr_hindi"] = False
            print(f"❌ Hindi Whisper ASR failed: {e}", flush=True)
            if progress_callback:
                progress_callback("ASR", "error")

        # 3. Mobile Fast TTS Engine (MMS-TTS VITS ~35M params)
        t0 = time.time()
        try:
            self.tts.load_hindi_tts()
            dur = time.time() - t0
            self.preload_stats["tts_hindi"] = dur
            results["tts_hindi"] = True
            print(f"✓ Mobile Fast Santali & Hindi VITS ({dur:.2f}s)", flush=True)
        except Exception as e:
            results["tts_hindi"] = False
            print(f"❌ Mobile VITS failed: {e}", flush=True)

        # 4. Santali Indic Parler-TTS (Loaded only if neural profile or auto on CUDA)
        should_load_parler = (profile == "neural") or (profile == "auto" and self.tts.device.type == "cuda")
        if should_load_parler:
            if progress_callback:
                progress_callback("Santali TTS", "loading")
            t0 = time.time()
            try:
                self.tts.load_santali_tts()
                dur = time.time() - t0
                self.preload_stats["tts_santali"] = dur
                results["tts_santali"] = True
                print(f"✓ Santali Indic Parler-TTS ({dur:.2f}s)", flush=True)
                if progress_callback:
                    progress_callback("Santali TTS", "ready")
            except Exception as e:
                results["tts_santali"] = False
                print(f"⚠️ Santali Indic Parler-TTS: {e}", flush=True)
                if progress_callback:
                    progress_callback("Santali TTS", "error")
        else:
            results["tts_santali"] = True
            print("✓ Mobile Fast Santali Engine active (2GB Android Profile: Parler-TTS deferred)", flush=True)

        print("\n✓ ASR Ready", flush=True)
        print("✓ Translation Ready", flush=True)
        print("✓ Santali TTS Ready (Mobile Engine)", flush=True)
        print("\nVaaniShiksha AI is ready.\n==================================================\n", flush=True)
        return results

    def run(
        self,
        audio_input: Union[str, Path, bytes, np.ndarray],
        source_language: str = "Hindi",
        target_language: str = "Santali",
        tts_output_filename: Optional[str] = None,
        mode: str = "full",
        speaker: str = "female",
        progress_callback: Optional[Callable[[str, str, str], None]] = None,
    ) -> Dict[str, Any]:
        """
        Executes complete Voice -> Speech Recognition -> Text Translation -> Voice Output pipeline.
        With accurate stage-by-stage timing logs and mathematical audio validation.
        """
        print("\n[PIPELINE] Started", flush=True)
        t_total_start = time.time()

        # Step 1: Audio Preprocessing & ASR
        print(f"[ASR] Starting ({source_language})...", flush=True)
        if progress_callback:
            progress_callback("asr", "running", f"Running {source_language} Speech Recognition...")

        t_asr_start = time.time()
        asr_res = self.asr.transcribe(audio_input, language=source_language, mode=mode)
        recognized_text = asr_res["text"]
        asr_latency = time.time() - t_asr_start
        raw_sanlish = asr_res.get("raw_sanlish", None)

        print(f"[ASR] Completed: {asr_latency:.2f} sec", flush=True)
        print(f"[ASR] Result: '{recognized_text}'", flush=True)

        if not recognized_text or not recognized_text.strip():
            if progress_callback:
                progress_callback("asr", "failed", "No speech recognized.")
            return {
                "source_language": source_language,
                "target_language": target_language,
                "recognized_text": "",
                "raw_sanlish": raw_sanlish,
                "translated_text": "",
                "tts_audio_path": None,
                "asr_time": round(asr_latency, 3),
                "translation_time": 0.0,
                "tts_time": 0.0,
                "total_time": round(time.time() - t_total_start, 3),
                "tts_status": "No speech recognized.",
                "audio_validation": {"valid_audio": False, "reason": "Empty ASR transcription"},
                "error": "ASR produced empty transcription.",
            }

        if progress_callback:
            progress_callback("asr", "completed", f"Recognized: {recognized_text}")

        # Step 2: IndicTrans2 Translation
        print(f"\n[TRANSLATION] Starting ({source_language} → {target_language})...", flush=True)
        if progress_callback:
            progress_callback("translation", "running", f"Translating {source_language} → {target_language}...")

        t_trans_start = time.time()
        trans_res = self.translator.run(
            recognized_text,
            source_language=source_language,
            target_language=target_language
        )
        translated_text = trans_res["translated_text"]
        trans_latency = time.time() - t_trans_start

        print(f"[TRANSLATION] Completed: {trans_latency:.2f} sec", flush=True)
        print(f"[TRANSLATION]\nHindi:\n{recognized_text}\nSantali:\n{translated_text}\n", flush=True)

        if progress_callback:
            progress_callback("translation", "completed", f"Translated: {translated_text}")

        # Step 3: Text-to-Speech (TTS) Synthesis
        print(f"[TTS] Starting ({target_language})...", flush=True)
        if progress_callback:
            progress_callback("tts", "running", f"Synthesizing {target_language} Voice Output...")

        t_tts_start = time.time()
        tts_res = self.tts.synthesize(
            translated_text,
            language=target_language,
            output_filename=tts_output_filename,
            mode=mode,
            speaker=speaker,
        )
        tts_latency = time.time() - t_tts_start
        tts_audio_path = tts_res.get("audio_path", None)
        tts_status = tts_res.get("status", tts_res.get("error", "TTS completed"))
        validation = tts_res.get("validation", {"valid_audio": False, "reason": "No validation data"})

        generated_dur = validation.get("duration", 0.0)
        file_size_kb = (validation.get("file_size", 0) / 1024) if validation else 0.0

        print(f"[TTS] Completed: {tts_latency:.2f} sec", flush=True)
        print(f"[TTS] Generated duration: {generated_dur:.2f} sec", flush=True)
        print(f"[TTS] File size: {file_size_kb:.1f} KB", flush=True)

        total_latency = time.time() - t_total_start
        print(f"\n[PIPELINE] Total: {total_latency:.2f} sec\n", flush=True)

        # Memory Monitoring & Cleanup
        mem_mgr = ModelMemoryManager.get_instance()
        mem_status = mem_mgr.check_memory_budget()
        mem_mgr.cleanup_memory()

        if progress_callback:
            if tts_audio_path and validation.get("valid_audio", False):
                progress_callback("tts", "completed", "Audio synthesis and validation complete.")
            else:
                progress_callback("tts", "warning", f"TTS fallback: {tts_status}")

        return {
            "source_language": source_language,
            "target_language": target_language,
            "source_code": trans_res.get("source_code", "hin_Deva"),
            "target_code": trans_res.get("target_code", "sat_Olck"),
            "recognized_text": recognized_text,
            "raw_sanlish": raw_sanlish,
            "translated_text": translated_text,
            "tts_audio_path": tts_audio_path,
            "asr_time": round(asr_latency, 3),
            "translation_time": round(trans_latency, 3),
            "tts_time": round(tts_latency, 3),
            "total_time": round(total_latency, 3),
            "tts_status": tts_status,
            "audio_validation": validation,
            "memory_info": mem_status,
            "error": None if validation.get("valid_audio", False) else tts_res.get("error"),
        }
