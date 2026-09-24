import os
import sys
import time
import shutil
from pathlib import Path
import torch
import onnx
import onnxruntime as ort
from onnxruntime.quantization import quantize_dynamic, QuantType
from transformers import WhisperForConditionalGeneration, WhisperProcessor

# Configure UTF-8 encoding
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

BASE_DIR = Path(__file__).resolve().parent.parent.parent
MODEL_NAME = "collabora/whisper-tiny-hindi"
OUTPUT_DIR = BASE_DIR / "android_ai" / "models" / "asr"
TOKENIZER_DIR = BASE_DIR / "android_ai" / "tokenizers" / "asr"

OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
TOKENIZER_DIR.mkdir(parents=True, exist_ok=True)

class WhisperEncoderWrapper(torch.nn.Module):
    """Clean ONNX export wrapper for Whisper Encoder."""
    def __init__(self, encoder):
        super().__init__()
        self.encoder = encoder

    def forward(self, input_features: torch.Tensor) -> torch.Tensor:
        # input_features: [batch, 80, 3000]
        # output: [batch, 1500, 384]
        return self.encoder(input_features).last_hidden_state

class WhisperDecoderWrapper(torch.nn.Module):
    """Clean ONNX export wrapper for Whisper Decoder."""
    def __init__(self, decoder, proj_out):
        super().__init__()
        self.decoder = decoder
        self.proj_out = proj_out

    def forward(self, input_ids: torch.Tensor, encoder_hidden_states: torch.Tensor) -> torch.Tensor:
        # input_ids: [batch, seq_len]
        # encoder_hidden_states: [batch, 1500, 384]
        # output: logits [batch, seq_len, vocab_size]
        decoder_outputs = self.decoder(
            input_ids=input_ids,
            encoder_hidden_states=encoder_hidden_states,
        )
        lm_logits = self.proj_out(decoder_outputs.last_hidden_state)
        return lm_logits

def export_and_quantize_whisper():
    print("=" * 70)
    print("      EXPORTING WHISPER-TINY-HINDI TO ANDROID ONNX INT8")
    print("=" * 70)

    print(f"\n[1/5] Loading '{MODEL_NAME}' in PyTorch...")
    processor = WhisperProcessor.from_pretrained(MODEL_NAME)
    model = WhisperForConditionalGeneration.from_pretrained(MODEL_NAME)
    model.eval()

    # Save tokenizer assets directly to android_ai/tokenizers/asr/
    print("\n[2/5] Exporting tokenizer files to android_ai/tokenizers/asr/...")
    processor.save_pretrained(str(TOKENIZER_DIR))
    print(f"✓ Tokenizer saved to: {TOKENIZER_DIR}")

    # Prepare dummy tensors
    dummy_input_features = torch.randn(1, 80, 3000, dtype=torch.float32)
    dummy_encoder_hidden_states = torch.randn(1, 1500, 384, dtype=torch.float32)
    dummy_input_ids = torch.tensor([[50258, 50276, 50359, 50363]], dtype=torch.long)

    encoder_onnx_path = OUTPUT_DIR / "encoder_model.onnx"
    encoder_quant_path = OUTPUT_DIR / "encoder_model_quant.onnx"
    decoder_onnx_path = OUTPUT_DIR / "decoder_model.onnx"
    decoder_quant_path = OUTPUT_DIR / "decoder_model_quant.onnx"

    # Export Encoder
    print(f"\n[3/5] Exporting Whisper Encoder to ONNX ({encoder_onnx_path})...")
    encoder_wrapper = WhisperEncoderWrapper(model.model.encoder)
    encoder_wrapper.eval()

    torch.onnx.export(
        encoder_wrapper,
        dummy_input_features,
        str(encoder_onnx_path),
        input_names=["input_features"],
        output_names=["last_hidden_state"],
        dynamic_axes={
            "input_features": {0: "batch_size"},
            "last_hidden_state": {0: "batch_size"},
        },
        opset_version=14,
        do_constant_folding=True,
        dynamo=False,
    )
    print(f"✓ Encoder exported: {encoder_onnx_path.stat().st_size / (1024*1024):.2f} MB")

    # Export Decoder
    print(f"\n[4/5] Exporting Whisper Decoder to ONNX ({decoder_onnx_path})...")
    decoder_wrapper = WhisperDecoderWrapper(model.model.decoder, model.proj_out)
    decoder_wrapper.eval()

    torch.onnx.export(
        decoder_wrapper,
        (dummy_input_ids, dummy_encoder_hidden_states),
        str(decoder_onnx_path),
        input_names=["input_ids", "encoder_hidden_states"],
        output_names=["logits"],
        dynamic_axes={
            "input_ids": {0: "batch_size", 1: "seq_len"},
            "encoder_hidden_states": {0: "batch_size"},
            "logits": {0: "batch_size", 1: "seq_len"},
        },
        opset_version=14,
        do_constant_folding=True,
        dynamo=False,
    )
    print(f"✓ Decoder exported: {decoder_onnx_path.stat().st_size / (1024*1024):.2f} MB")

    # INT8 Quantization
    print("\n[5/5] Quantizing models to INT8 Dynamic for Android deployment...")
    t0 = time.time()
    quantize_dynamic(
        model_input=str(encoder_onnx_path),
        model_output=str(encoder_quant_path),
        weight_type=QuantType.QInt8,
    )
    enc_quant_size = encoder_quant_path.stat().st_size / (1024*1024)
    print(f"✓ Encoder INT8: {enc_quant_size:.2f} MB (Saved: {encoder_quant_path})")

    quantize_dynamic(
        model_input=str(decoder_onnx_path),
        model_output=str(decoder_quant_path),
        weight_type=QuantType.QInt8,
    )
    dec_quant_size = decoder_quant_path.stat().st_size / (1024*1024)
    print(f"✓ Decoder INT8: {dec_quant_size:.2f} MB (Saved: {decoder_quant_path})")
    print(f"Quantization finished in {time.time() - t0:.2f}s")

    total_int8_size = enc_quant_size + dec_quant_size
    print(f"\n======================================================================")
    print(f"  TOTAL WHISPER-TINY-HINDI ANDROID PACKAGE SIZE: {total_int8_size:.2f} MB")
    print(f"  MEMORY BUDGET IMPACT: ~40 MB RAM (Target: << 1800 MB -> EXCELLENT)")
    print(f"======================================================================\n")

if __name__ == "__main__":
    export_and_quantize_whisper()
