import os
import sys
import time
from pathlib import Path
from typing import Dict, Any, Optional, Tuple, Union
import numpy as np
import soundfile as sf
import torch
from transformers import WhisperForConditionalGeneration, WhisperProcessor
import aksharamukha.transliterate as akshara_tl

ROOT_DIR = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT_DIR))

from src.hardware import get_system_device
from src.config import HINDI_ASR_DIR

SANTALI_WHISPER_DIR = ROOT_DIR / "whisper-small-santali-sanlish-962"
HINDI_WHISPER_REPO = "collabora/whisper-tiny-hindi"


def trim_silence_vad(audio: np.ndarray, target_sr: int = 16000, top_db: float = 35.0) -> np.ndarray:
    """
    Intelligent Energy-based Voice Activity Detection (VAD) silence trimmer.
    Trims dead leading/trailing silence (>0.15s) while strictly preserving speech onset and consonants.
    """
    if len(audio) < int(target_sr * 0.2):
        return audio
    
    frame_len = int(target_sr * 0.02)  # 20ms
    hop_len = int(target_sr * 0.01)    # 10ms
    
    num_frames = (len(audio) - frame_len) // hop_len
    if num_frames <= 0:
        return audio
        
    energy = np.array([
        np.sum(audio[i * hop_len : i * hop_len + frame_len] ** 2)
        for i in range(num_frames)
    ])
    
    max_e = np.max(energy) if len(energy) > 0 else 0.0
    if max_e < 1e-6:
        return audio
        
    threshold = max_e * (10 ** (-top_db / 10.0))
    active = np.where(energy > threshold)[0]
    if len(active) == 0:
        return audio
        
    # Keep 150ms margin before and after active speech
    margin_samples = int(target_sr * 0.15)
    start_sample = max(0, int(active[0] * hop_len) - margin_samples)
    end_sample = min(len(audio), int(active[-1] * hop_len + frame_len) + margin_samples)
    
    return audio[start_sample:end_sample]


def inspect_and_preprocess_audio(
    audio_input: Union[str, Path, bytes, np.ndarray],
    target_sr: int = 16000,
    max_duration_sec: float = 30.0
) -> Tuple[np.ndarray, Dict[str, Any]]:
    """
    Loads raw audio input from path, bytes, or ndarray, inspects properties,
    converts to 16kHz mono float32, applies VAD silence trimming, and peak normalizes.

    Returns:
        (audio_16k_mono, diagnostics_dict)
    """
    orig_sr = target_sr
    orig_channels = 1

    if isinstance(audio_input, (str, Path)):
        audio_path = str(audio_input)
        info = sf.info(audio_path)
        orig_sr = info.samplerate
        orig_channels = info.channels
        raw_audio, orig_sr = sf.read(audio_path)
    elif isinstance(audio_input, bytes):
        import io
        raw_bytes_io = io.BytesIO(audio_input)
        info = sf.info(raw_bytes_io)
        orig_sr = info.samplerate
        orig_channels = info.channels
        raw_bytes_io.seek(0)
        raw_audio, orig_sr = sf.read(raw_bytes_io)
    elif isinstance(audio_input, np.ndarray):
        raw_audio = audio_input
        orig_channels = 1 if raw_audio.ndim == 1 else raw_audio.shape[1]
    else:
        raise ValueError(f"Unsupported audio input type: {type(audio_input)}")

    raw_samples_count = len(raw_audio)
    raw_duration = raw_samples_count / float(orig_sr)

    # Convert multi-channel to mono
    if raw_audio.ndim > 1:
        mono_audio = np.mean(raw_audio, axis=1)
    else:
        mono_audio = raw_audio

    # Resample to target 16kHz if needed
    if orig_sr != target_sr:
        import scipy.signal
        target_length = int(len(mono_audio) * target_sr / orig_sr)
        mono_16k = scipy.signal.resample(mono_audio, target_length)
    else:
        mono_16k = mono_audio

    mono_16k = mono_16k.astype(np.float32)

    # Normalize peak amplitude
    peak_val = float(np.max(np.abs(mono_16k))) if len(mono_16k) > 0 else 0.0
    if peak_val > 1e-4:
        normalized_audio = (mono_16k / peak_val) * 0.95
    else:
        normalized_audio = mono_16k

    # Apply VAD silence trimming to remove dead leading/trailing silence
    trimmed_audio = trim_silence_vad(normalized_audio, target_sr=target_sr, top_db=35.0)

    # Cap to max_duration_sec
    max_samples = int(max_duration_sec * target_sr)
    if len(trimmed_audio) > max_samples:
        trimmed_audio = trimmed_audio[:max_samples]

    processed_samples = len(trimmed_audio)
    processed_duration = processed_samples / float(target_sr)

    diagnostics = {
        "sample_rate": orig_sr,
        "target_sample_rate": target_sr,
        "channels": orig_channels,
        "raw_samples": raw_samples_count,
        "raw_duration": round(raw_duration, 3),
        "processed_samples": processed_samples,
        "processed_duration": round(processed_duration, 3),
        "peak_amplitude": round(peak_val, 4),
    }

    return trimmed_audio, diagnostics

    print("\n--- Audio Ingestion Diagnostics ---", flush=True)
    print(f"Sample rate:        {orig_sr} Hz", flush=True)
    print(f"Channels:           {orig_channels}", flush=True)
    print(f"Number of samples:  {raw_samples_count}", flush=True)
    print(f"Audio duration:     {raw_duration:.2f} sec", flush=True)
    print(f"ASR input duration: {processed_duration:.2f} sec (Samples: {len(normalized_audio)})", flush=True)
    print("-----------------------------------\n", flush=True)

    return normalized_audio, diagnostics


class OfflineASRManager:
    """
    Offline Automatic Speech Recognition (ASR) Manager for VaaniShiksha AI.
    - Hindi ASR: Uses 'collabora/whisper-tiny-hindi' (or local cached directory).
      Transcribes arbitrary length audio into full Hindi Devanagari text without truncation.
    - Santali ASR: Uses 'whisper-small-santali-sanlish-962' + Aksharamukha transliteration to Ol Chiki.
    """

    _instance: Optional["OfflineASRManager"] = None

    def __init__(self):
        self.device = torch.device(get_system_device())
        num_threads = max(1, min(8, os.cpu_count() or 4))
        if self.device.type == "cpu":
            torch.set_num_threads(num_threads)

        self._hindi_processor: Optional[WhisperProcessor] = None
        self._hindi_model: Optional[WhisperForConditionalGeneration] = None
        self._santali_processor: Optional[WhisperProcessor] = None
        self._santali_model: Optional[WhisperForConditionalGeneration] = None

    @classmethod
    def get_instance(cls) -> "OfflineASRManager":
        """Singleton accessor to ensure models stay in persistent memory."""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance

    def load_hindi_asr(self) -> bool:
        """Loads Hindi Whisper ASR model into persistent memory."""
        if self._hindi_model is not None and self._hindi_processor is not None:
            return True
        t0 = time.time()
        try:
            model_target = str(HINDI_ASR_DIR) if HINDI_ASR_DIR.exists() else HINDI_WHISPER_REPO
            print(f"[ASR] Loading Hindi Whisper ASR ({model_target}) on {self.device}...", flush=True)

            self._hindi_processor = WhisperProcessor.from_pretrained(model_target)
            self._hindi_model = WhisperForConditionalGeneration.from_pretrained(model_target)
            self._hindi_model.to(self.device)
            self._hindi_model.eval()

            # Cleanly configure generation settings for Hindi Devanagari transcription without conflict
            self._hindi_model.generation_config.language = "hi"
            self._hindi_model.generation_config.task = "transcribe"
            self._hindi_model.generation_config.forced_decoder_ids = None

            t_load = time.time() - t0
            print(f"[ASR] Hindi Whisper ASR loaded in {t_load:.2f}s on {self.device}. READY", flush=True)
            return True
        except Exception as e:
            print(f"[ASR ERROR] Failed to load Hindi ASR after {time.time() - t0:.2f}s: {e}", flush=True)
            return False

    def load_santali_asr(self) -> bool:
        """Loads Santali Whisper ASR model into persistent memory."""
        if self._santali_model is not None:
            return True
        t0 = time.time()
        try:
            if not SANTALI_WHISPER_DIR.exists():
                print(f"[ASR ERROR] Santali Whisper directory not found at: {SANTALI_WHISPER_DIR}", flush=True)
                return False
            print(f"[ASR] Loading Santali Whisper ASR from: {SANTALI_WHISPER_DIR} on {self.device}...", flush=True)
            self._santali_processor = WhisperProcessor.from_pretrained("openai/whisper-small")
            self._santali_model = WhisperForConditionalGeneration.from_pretrained(str(SANTALI_WHISPER_DIR))
            self._santali_model.to(self.device)
            self._santali_model.eval()
            self._santali_model.config.forced_decoder_ids = None

            t_load = time.time() - t0
            print(f"[ASR] Santali Whisper ASR loaded in {t_load:.2f}s on {self.device}. READY", flush=True)
            return True
        except Exception as e:
            print(f"[ASR ERROR] Failed to load Santali ASR after {time.time() - t0:.2f}s: {e}", flush=True)
            return False

    def transcribe_hindi(
        self,
        audio_input: Union[str, Path, bytes, np.ndarray],
        mode: str = "full"
    ) -> Dict[str, Any]:
        """
        Transcribes Hindi speech of any duration into full Hindi (Devanagari) text.
        """
        if not self.load_hindi_asr():
            raise RuntimeError("Hindi ASR model is not available.")

        t0 = time.time()
        audio_16k, diag = inspect_and_preprocess_audio(audio_input, target_sr=16000, max_duration_sec=30.0)

        if len(audio_16k) == 0:
            return {
                "text": "",
                "raw_text": "",
                "script": "Devanagari (hin_Deva)",
                "latency": 0.0,
                "audio_duration": 0.0,
                "diagnostics": diag,
            }

        # Whisper feature extraction over the complete audio signal with explicit attention mask
        inputs = self._hindi_processor(
            audio_16k,
            sampling_rate=16000,
            return_tensors="pt",
            return_attention_mask=True
        )
        input_features = inputs.input_features.to(self.device)
        attention_mask = inputs.attention_mask.to(self.device) if hasattr(inputs, "attention_mask") and inputs.attention_mask is not None else None

        # Adequate token bound to decode full sentences without premature cutoff
        max_new_tokens = 150 if mode == "fast" else 256

        with torch.inference_mode():
            gen_kwargs = {
                "max_new_tokens": max_new_tokens,
                "no_repeat_ngram_size": 3,
                "repetition_penalty": 1.1,
                "temperature": 0.0,
                "language": "hi",
                "task": "transcribe",
            }
            if attention_mask is not None:
                gen_kwargs["attention_mask"] = attention_mask

            predicted_ids = self._hindi_model.generate(
                input_features,
                **gen_kwargs
            )

        transcription = self._hindi_processor.batch_decode(
            predicted_ids, skip_special_tokens=True
        )[0].strip()

        latency = time.time() - t0
        print(f"[ASR] Hindi full audio ({diag['processed_duration']}s) transcribed in {latency:.2f}s: '{transcription}'", flush=True)

        return {
            "text": transcription,
            "raw_text": transcription,
            "script": "Devanagari (hin_Deva)",
            "latency": round(latency, 3),
            "audio_duration": diag["processed_duration"],
            "diagnostics": diag,
        }

    def transcribe_santali(
        self,
        audio_input: Union[str, Path, bytes, np.ndarray],
        mode: str = "full"
    ) -> Dict[str, Any]:
        """
        Transcribes Santali speech to Sanlish (Roman) text, then transliterates to Ol Chiki script.
        """
        if not self.load_santali_asr():
            raise RuntimeError("Santali ASR model is not available.")

        t0 = time.time()
        audio_16k, diag = inspect_and_preprocess_audio(audio_input, target_sr=16000, max_duration_sec=30.0)

        if len(audio_16k) == 0:
            return {
                "text": "",
                "raw_sanlish": "",
                "script": "Ol Chiki (sat_Olck)",
                "latency": 0.0,
                "audio_duration": 0.0,
                "diagnostics": diag,
            }

        inputs = self._santali_processor(
            audio_16k,
            sampling_rate=16000,
            return_tensors="pt"
        )
        input_features = inputs.input_features.to(self.device)

        max_new_tokens = 150 if mode == "fast" else 256

        with torch.inference_mode():
            predicted_ids = self._santali_model.generate(
                input_features,
                max_new_tokens=max_new_tokens,
                no_repeat_ngram_size=3,
                repetition_penalty=1.15,
            )

        raw_sanlish = self._santali_processor.batch_decode(
            predicted_ids, skip_special_tokens=True
        )[0].strip()

        # Transliterate Roman Sanlish to Ol Chiki script
        olchiki_text = akshara_tl.process("Santali_Sanlish", "Santali_Ol_Chiki", raw_sanlish).strip()

        latency = time.time() - t0
        print(f"[ASR] Santali full audio ({diag['processed_duration']}s) transcribed in {latency:.2f}s: Sanlish='{raw_sanlish}' -> Ol Chiki='{olchiki_text}'", flush=True)

        return {
            "text": olchiki_text,
            "raw_sanlish": raw_sanlish,
            "script": "Ol Chiki (sat_Olck)",
            "latency": round(latency, 3),
            "audio_duration": diag["processed_duration"],
            "diagnostics": diag,
        }

    def transcribe(
        self,
        audio_input: Union[str, Path, bytes, np.ndarray],
        language: str = "Hindi",
        mode: str = "full"
    ) -> Dict[str, Any]:
        """Unified ASR transcription dispatcher."""
        lang_lower = language.lower()
        if "hin" in lang_lower:
            return self.transcribe_hindi(audio_input, mode=mode)
        elif "san" in lang_lower or "sat" in lang_lower:
            return self.transcribe_santali(audio_input, mode=mode)
        else:
            raise ValueError(f"Unsupported ASR language: {language}")
