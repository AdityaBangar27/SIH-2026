import os
import sys
from pathlib import Path
from huggingface_hub import snapshot_download

BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

INDICTRANS_REPO = "hari31416/indictrans2-indic-indic-dist-320M-ONNX-int8"
INDICTRANS_DIR = MODELS_DIR / "indictrans2-indic-indic-dist-320M-ONNX-int8"

PARLER_REPO = "RXD03/indic-parler-tts"
PARLER_DIR = MODELS_DIR / "indic-parler-tts"


def download():
    """
    Downloads and caches offline models into local models/ directory:
    - IndicTrans2 Indic-Indic 320M INT8 ONNX model
    - Indic Parler-TTS model for Santali speech synthesis
    """
    MODELS_DIR.mkdir(parents=True, exist_ok=True)

    print("==================================================")
    print("VaaniShiksha AI: Offline Models Local Downloader")
    print("==================================================")

    # 1. IndicTrans2 ONNX
    print(f"\n[1/2] Downloading IndicTrans2 ONNX INT8 ({INDICTRANS_REPO})...")
    try:
        snapshot_download(
            repo_id=INDICTRANS_REPO,
            local_dir=str(INDICTRANS_DIR),
            resume_download=True,
        )
        print(f"✓ IndicTrans2 ONNX model cached at: {INDICTRANS_DIR}")
    except Exception as e:
        print(f"❌ Error downloading IndicTrans2: {e}")

    # 2. Indic Parler-TTS
    print(f"\n[2/2] Downloading Indic Parler-TTS ({PARLER_REPO})...")
    try:
        snapshot_download(
            repo_id=PARLER_REPO,
            local_dir=str(PARLER_DIR),
            resume_download=True,
        )
        print(f"✓ Indic Parler-TTS model cached at: {PARLER_DIR}")
    except Exception as e:
        print(f"❌ Error downloading Indic Parler-TTS: {e}")

    print("\n==================================================")
    print("Offline model download check complete!")
    print("==================================================")


if __name__ == "__main__":
    download()