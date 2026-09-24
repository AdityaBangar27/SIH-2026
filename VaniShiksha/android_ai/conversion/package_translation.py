import os
import sys
import shutil
from pathlib import Path

# Configure UTF-8
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = Path(__file__).resolve().parent.parent.parent
SRC_MODEL_DIR = BASE_DIR / "models" / "indictrans2-indic-indic-dist-320M-ONNX-int8"

TGT_MODEL_DIR = BASE_DIR / "android_ai" / "models" / "translation"
TGT_TOKENIZER_DIR = BASE_DIR / "android_ai" / "tokenizers" / "translation"

TGT_MODEL_DIR.mkdir(parents=True, exist_ok=True)
TGT_TOKENIZER_DIR.mkdir(parents=True, exist_ok=True)

MODEL_FILES = [
    "encoder_model.onnx",
    "encoder_model.onnx.data",
    "decoder_model.onnx",
    "decoder_shared.onnx.data",
    "decoder_with_past_model.onnx",
    "generation_config.json",
    "config.json",
]

TOKENIZER_FILES = [
    "tokenizer_src.json",
    "tokenizer_tgt.json",
    "tokenizer_meta.json",
    "dict.SRC.json",
    "dict.TGT.json",
    "special_tokens_map.json",
    "tokenizer_config.json",
]

def package_indictrans():
    print("=" * 70)
    print("   PACKAGING INDICTRANS2 INT8 ONNX FOR ANDROID DEPLOYMENT")
    print("=" * 70)
    
    if not SRC_MODEL_DIR.exists():
        print(f"❌ Source directory not found: {SRC_MODEL_DIR}")
        return False
        
    print("\n[1/2] Packaging ONNX Model weights...")
    total_model_bytes = 0
    for f in MODEL_FILES:
        src_path = SRC_MODEL_DIR / f
        if src_path.exists():
            tgt_path = TGT_MODEL_DIR / f
            shutil.copyfile(str(src_path), str(tgt_path))
            sz_mb = tgt_path.stat().st_size / (1024 * 1024)
            total_model_bytes += tgt_path.stat().st_size
            print(f"  ✓ {f:<32} {sz_mb:>8.2f} MB")
        else:
            print(f"  ⚠️ Warning: {f} not found in source!")

    print("\n[2/2] Packaging Tokenizer and Vocabulary assets...")
    total_tok_bytes = 0
    for f in TOKENIZER_FILES:
        src_path = SRC_MODEL_DIR / f
        if src_path.exists():
            tgt_path = TGT_TOKENIZER_DIR / f
            shutil.copyfile(str(src_path), str(tgt_path))
            sz_mb = tgt_path.stat().st_size / (1024 * 1024)
            total_tok_bytes += tgt_path.stat().st_size
            print(f"  ✓ {f:<32} {sz_mb:>8.2f} MB")
        else:
            print(f"  ⚠️ Warning: {f} not found in source!")
            
    print("\n" + "=" * 70)
    print(f"  TOTAL INDICTRANS2 MODEL SIZE:     {total_model_bytes/(1024*1024):.2f} MB")
    print(f"  TOTAL TOKENIZER/ASSETS SIZE:      {total_tok_bytes/(1024*1024):.2f} MB")
    print(f"  TOTAL TRANSLATION PACKAGE SIZE:   {(total_model_bytes+total_tok_bytes)/(1024*1024):.2f} MB")
    print("=" * 70 + "\n")
    return True

if __name__ == "__main__":
    package_indictrans()
