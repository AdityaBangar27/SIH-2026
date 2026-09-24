import os
import time
import json
from pathlib import Path
from typing import Dict, Any, Optional

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

from src.config import get_local_model_path, HINDI_CODE, SANTALI_CODE, SUPPORTED_LANGUAGES


def _past_feed(past_outputs: list[np.ndarray], num_layers: int) -> dict[str, np.ndarray]:
    """Build the past_key_values feed dict for decoder_with_past."""
    feed = {}
    for i in range(num_layers):
        base = i * 4
        feed[f"past_key_values.{i}.decoder.key"] = past_outputs[base]
        feed[f"past_key_values.{i}.decoder.value"] = past_outputs[base + 1]
        feed[f"past_key_values.{i}.encoder.key"] = past_outputs[base + 2]
        feed[f"past_key_values.{i}.encoder.value"] = past_outputs[base + 3]
    return feed


class OfflineIndicTranslator:
    """
    Offline Local Translation engine for Hindi <-> Santali using IndicTrans2 320M INT8 ONNX.
    Keeps ONNX sessions persistently loaded in memory (singleton) with optimized CPU threading.
    """

    _instance: Optional["OfflineIndicTranslator"] = None

    def __init__(self, model_path: Optional[str] = None):
        self.model_path = model_path or get_local_model_path()

        if not self.model_path or not os.path.exists(self.model_path):
            raise FileNotFoundError(
                "Local IndicTrans2 ONNX model directory not found.\n"
                "Please run `python download_model.py` to download the model files."
            )

        print(f"[Translator] Loading IndicTrans2 ONNX model from: {self.model_path}", flush=True)
        start_load = time.time()

        snap = Path(self.model_path)

        # Load tokenizers
        self.src_tok = Tokenizer.from_file(str(snap / "tokenizer_src.json"))
        self.tgt_tok = Tokenizer.from_file(str(snap / "tokenizer_tgt.json"))
        self.meta: dict = json.loads((snap / "tokenizer_meta.json").read_text(encoding="utf-8"))

        # Load generation config
        gen_cfg_path = snap / "generation_config.json"
        gen_cfg: dict = {}
        if gen_cfg_path.exists():
            gen_cfg = json.loads(gen_cfg_path.read_text(encoding="utf-8"))
        self.decoder_start_id: int = int(gen_cfg.get("decoder_start_token_id", 2))
        self.eos_id: int = int(gen_cfg.get("eos_token_id", 2))

        # Optimized ONNX Runtime Session Options
        sess_options = ort.SessionOptions()
        cpu_threads = max(1, min(8, os.cpu_count() or 4))
        sess_options.intra_op_num_threads = cpu_threads
        sess_options.inter_op_num_threads = 1
        sess_options.graph_optimization_level = ort.GraphOptimizationLevel.ORT_ENABLE_ALL
        sess_options.execution_mode = ort.ExecutionMode.ORT_SEQUENTIAL

        providers = ["CPUExecutionProvider"]
        self.enc_sess = ort.InferenceSession(str(snap / "encoder_model.onnx"), sess_options, providers=providers)
        self.dec_sess = ort.InferenceSession(str(snap / "decoder_model.onnx"), sess_options, providers=providers)
        self.dec_past_sess = ort.InferenceSession(str(snap / "decoder_with_past_model.onnx"), sess_options, providers=providers)

        # Number of layers in decoder
        self.num_layers: int = (len(self.dec_sess.get_outputs()) - 1) // 4

        self.load_time = round(time.time() - start_load, 3)
        print(f"[Translator] Model loaded in {self.load_time}s on {cpu_threads} CPU threads (Decoder layers: {self.num_layers}).", flush=True)

    @classmethod
    def get_instance(cls, model_path: Optional[str] = None) -> "OfflineIndicTranslator":
        """Get or initialize the persistent loaded translator instance in memory."""
        if cls._instance is None:
            cls._instance = cls(model_path=model_path)
        return cls._instance

    def translate(
        self,
        text: str,
        source_lang: str = HINDI_CODE,
        target_lang: str = SANTALI_CODE,
        max_new_tokens: int = 128
    ) -> Dict[str, Any]:
        """
        Translate input text from source_lang to target_lang.

        Args:
            text (str): Input sentence to translate.
            source_lang (str): Source language code (e.g., 'hin_Deva' or 'Hindi').
            target_lang (str): Target language code (e.g., 'sat_Olck' or 'Santali').
            max_new_tokens (int): Max generation length.

        Returns:
            dict: Translation details and measured execution time.
        """
        src_code = SUPPORTED_LANGUAGES.get(source_lang, source_lang)
        tgt_code = SUPPORTED_LANGUAGES.get(target_lang, target_lang)

        if not text or not text.strip():
            return {
                "source_lang": src_code,
                "source_text": text,
                "target_lang": tgt_code,
                "translated_text": "",
                "translation_time": 0.0,
            }

        start_time = time.time()

        # Prefix source with language tags: <src_lang> <tgt_lang> <text>
        prefixed = f"{src_code} {tgt_code} {text.strip()}"
        encoded = self.src_tok.encode(prefixed)

        # Prepare numpy input arrays for encoder
        src_dict_size = self.meta["src_dict_size"]
        unk_id = self.meta["unk_id"]
        input_ids = np.array(
            [[i if i < src_dict_size else unk_id for i in encoded.ids]],
            dtype=np.int64
        )
        attn_mask = np.array([encoded.attention_mask], dtype=np.int64)

        # Run Encoder
        enc_out = self.enc_sess.run(
            ["last_hidden_state"],
            {"input_ids": input_ids, "attention_mask": attn_mask}
        )[0]

        # Greedy Decoder Loop with cached Key-Values
        decoder_input_ids = np.array([[self.decoder_start_id]], dtype=np.int64)
        output_ids: list[int] = [self.decoder_start_id]
        past_outputs: list[np.ndarray] | None = None

        for step in range(max_new_tokens):
            if step == 0:
                dec_out = self.dec_sess.run(
                    None,
                    {
                        "input_ids": decoder_input_ids,
                        "encoder_hidden_states": enc_out,
                        "encoder_attention_mask": attn_mask,
                    },
                )
            else:
                dec_out = self.dec_past_sess.run(
                    None,
                    {
                        "input_ids": decoder_input_ids,
                        "encoder_attention_mask": attn_mask,
                        **_past_feed(past_outputs, self.num_layers),
                    },
                )

            logits = dec_out[0]
            past_outputs = list(dec_out[1:])
            next_id = int(np.argmax(logits[0, -1, :]))
            output_ids.append(next_id)

            if next_id == self.eos_id:
                break

            decoder_input_ids = np.array([[next_id]], dtype=np.int64)

        # Decode tokens to target text
        tgt_dict_size = self.meta["tgt_dict_size"]
        safe_ids = [i if i < tgt_dict_size else unk_id for i in output_ids]
        translated_text = self.tgt_tok.decode(safe_ids, skip_special_tokens=True).strip()

        latency = round(time.time() - start_time, 3)

        return {
            "source_lang": src_code,
            "source_text": text,
            "target_lang": tgt_code,
            "translated_text": translated_text,
            "translation_time": latency,
        }

    def hindi_to_santali(self, text: str) -> Dict[str, Any]:
        return self.translate(text, source_lang=HINDI_CODE, target_lang=SANTALI_CODE)

    def santali_to_hindi(self, text: str) -> Dict[str, Any]:
        return self.translate(text, source_lang=SANTALI_CODE, target_lang=HINDI_CODE)
