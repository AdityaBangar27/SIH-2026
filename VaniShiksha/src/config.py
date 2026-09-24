import os
from pathlib import Path

# Base directories
BASE_DIR = Path(__file__).resolve().parent.parent
MODELS_DIR = BASE_DIR / "models"
AUDIO_DIR = BASE_DIR / "audio"
TTS_OUTPUT_DIR = AUDIO_DIR / "tts_output"
TTS_CACHE_DIR = AUDIO_DIR / "tts_cache"

# Model Directories
ONNX_MODEL_DIR = MODELS_DIR / "indictrans2-indic-indic-dist-320M-ONNX-int8"
PARLER_TTS_DIR = MODELS_DIR / "indic-parler-tts"
HINDI_ASR_DIR = MODELS_DIR / "hindi-asr"

# Search paths for local IndicTrans2 model
INDICTRANS_SEARCH_PATHS = [
    str(ONNX_MODEL_DIR),
    str(BASE_DIR / "indictrans2-indic-indic-dist-320M-ONNX-int8"),
]

# BCP-47 language tags
HINDI_CODE = "hin_Deva"
SANTALI_CODE = "sat_Olck"

SUPPORTED_LANGUAGES = {
    "Hindi": HINDI_CODE,
    "Santali": SANTALI_CODE,
}

CODE_TO_NAME = {
    HINDI_CODE: "Hindi",
    SANTALI_CODE: "Santali",
}

# Standard Indic Parler-TTS Speaker Descriptions
SANTALI_SPEAKER_PROMPTS = {
    "female": "Sumitra speaks with a clear, warm voice at a moderate pace in a quiet room.",
    "male": "Raju speaks with a calm and natural tone at a moderate pace in a quiet room.",
}


def get_local_model_path() -> str | None:
    """
    Finds the first existing IndicTrans2 local ONNX model directory containing required ONNX files.
    """
    required_files = ["encoder_model.onnx", "decoder_model.onnx", "tokenizer_src.json", "tokenizer_tgt.json"]
    for path in INDICTRANS_SEARCH_PATHS:
        if os.path.exists(path):
            has_files = all(os.path.exists(os.path.join(path, f)) for f in required_files)
            if has_files:
                return str(path)
    return None

