"""
Audio validation module for VaaniShiksha AI.
Provides strict validation of generated WAV audio to prevent reporting success on empty,
silent, truncated, or invalid audio files.
"""

import os
from pathlib import Path
from typing import Dict, Any, Union, Optional
import numpy as np
import soundfile as sf


def validate_audio_file(
    audio_input: Union[str, Path, bytes, np.ndarray],
    sample_rate: Optional[int] = None,
    min_duration_sec: float = 0.1,
    min_amplitude_threshold: float = 0.005,
    min_rms_threshold: float = 0.001,
) -> Dict[str, Any]:
    """
    Validates that an audio file or buffer contains genuine, non-silent, playable audio.

    Validation checks:
    1. File / buffer exists and is non-empty (> 0 bytes).
    2. Header is valid and readable by soundfile/wave.
    3. Sample rate is valid (> 0 Hz).
    4. Channels are valid (>= 1).
    5. Number of audio frames > 0.
    6. Duration > min_duration_sec (> 0 seconds).
    7. Audio samples are not all zero.
    8. Maximum absolute amplitude > min_amplitude_threshold (> 0).
    9. RMS energy > min_rms_threshold (> 0).

    Returns:
        Dict[str, Any] with keys:
            - exists (bool)
            - file_size (int)
            - sample_rate (int)
            - channels (int)
            - frames (int)
            - duration (float)
            - min_amplitude (float)
            - max_amplitude (float)
            - rms (float)
            - valid_audio (bool)
            - reason (str)
    """
    diagnostics: Dict[str, Any] = {
        "exists": False,
        "file_size": 0,
        "sample_rate": 0,
        "channels": 0,
        "frames": 0,
        "duration": 0.0,
        "min_amplitude": 0.0,
        "max_amplitude": 0.0,
        "rms": 0.0,
        "valid_audio": False,
        "reason": "Unknown",
    }

    try:
        # Case 1: File path
        if isinstance(audio_input, (str, Path)):
            path = Path(audio_input)
            if not path.exists():
                diagnostics["reason"] = f"File does not exist: {path}"
                return diagnostics
            
            file_size = path.stat().st_size
            diagnostics["exists"] = True
            diagnostics["file_size"] = file_size

            if file_size == 0:
                diagnostics["reason"] = "File size is 0 bytes (empty file)."
                return diagnostics

            try:
                data, sr = sf.read(str(path))
            except Exception as e:
                diagnostics["reason"] = f"Failed to read WAV header/data: {e}"
                return diagnostics

        # Case 2: In-memory bytes
        elif isinstance(audio_input, bytes):
            import io
            diagnostics["exists"] = True
            diagnostics["file_size"] = len(audio_input)

            if len(audio_input) == 0:
                diagnostics["reason"] = "Byte buffer is empty (0 bytes)."
                return diagnostics

            try:
                data, sr = sf.read(io.BytesIO(audio_input))
            except Exception as e:
                diagnostics["reason"] = f"Failed to read WAV from bytes: {e}"
                return diagnostics

        # Case 3: Numpy array
        elif isinstance(audio_input, np.ndarray):
            diagnostics["exists"] = True
            diagnostics["file_size"] = audio_input.nbytes
            data = audio_input
            sr = sample_rate or 16000

        else:
            diagnostics["reason"] = f"Unsupported audio input type: {type(audio_input)}"
            return diagnostics

        # Check sample rate
        diagnostics["sample_rate"] = int(sr)
        if sr <= 0:
            diagnostics["reason"] = f"Invalid sample rate: {sr} Hz"
            return diagnostics

        # Check shape / channels / frames
        if data.ndim == 1:
            channels = 1
            frames = len(data)
        elif data.ndim == 2:
            channels = data.shape[1]
            frames = data.shape[0]
        else:
            diagnostics["reason"] = f"Invalid audio dimensions: {data.ndim}"
            return diagnostics

        diagnostics["channels"] = channels
        diagnostics["frames"] = frames

        if frames == 0:
            diagnostics["reason"] = "Audio frame count is 0."
            return diagnostics

        # Check duration
        duration = float(frames) / float(sr)
        diagnostics["duration"] = round(duration, 3)

        if duration < min_duration_sec:
            diagnostics["reason"] = f"Audio duration ({duration:.3f}s) is below minimum threshold ({min_duration_sec}s)."
            return diagnostics

        # Numerical checks
        data_flat = data.flatten().astype(np.float64)
        min_val = float(np.min(data_flat))
        max_val = float(np.max(data_flat))
        max_abs = float(np.max(np.abs(data_flat)))
        rms_val = float(np.sqrt(np.mean(data_flat ** 2)))

        diagnostics["min_amplitude"] = round(min_val, 5)
        diagnostics["max_amplitude"] = round(max_val, 5)
        diagnostics["rms"] = round(rms_val, 6)

        # Check for absolute silence / all zeros
        if max_abs == 0.0:
            diagnostics["reason"] = "All audio samples are exactly zero (pure digital silence)."
            return diagnostics

        # Check for near-silent / corrupted low amplitude
        if max_abs < min_amplitude_threshold:
            diagnostics["reason"] = f"Max amplitude ({max_abs:.6f}) is below audible threshold ({min_amplitude_threshold}). Audio is near-inaudible."
            return diagnostics

        if rms_val < min_rms_threshold:
            diagnostics["reason"] = f"RMS energy ({rms_val:.6f}) is below minimum threshold ({min_rms_threshold}). Audio contains no substantial speech signal."
            return diagnostics

        # All checks passed
        diagnostics["valid_audio"] = True
        diagnostics["reason"] = "Valid non-zero playable audio."
        return diagnostics

    except Exception as e:
        diagnostics["reason"] = f"Unexpected audio validation error: {e}"
        return diagnostics
