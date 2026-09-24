import sys
import json
import numpy as np
import soundfile as sf
import onnxruntime as ort

sys.stdout.reconfigure(encoding="utf-8")

tts_model_path = "app/src/main/assets/models/tts/sat_piper_model.onnx"
tts_cfg_path = "app/src/main/assets/models/tts/sat_piper_model.onnx.json"

with open(tts_cfg_path, "r", encoding="utf-8") as f:
    cfg = json.load(f)

phoneme_id_map = cfg["phoneme_id_map"]
sample_rate = cfg["audio"]["sample_rate"]
noise_scale = cfg["inference"]["noise_scale"]
length_scale = cfg["inference"]["length_scale"]
noise_w = cfg["inference"]["noise_w"]

bos_id = phoneme_id_map["^"][0]
eos_id = phoneme_id_map["$"][0]
pad_id = phoneme_id_map["_"][0]

sess_opts = ort.SessionOptions()
sess_opts.intra_op_num_threads = 4
tts_sess = ort.InferenceSession(tts_model_path, sess_opts, providers=["CPUExecutionProvider"])

santali_texts = [
    ("TC1", "ᱤᱧᱟᱹᱜ ᱵᱷᱟᱨᱚᱛ ᱫᱚ ᱦᱩᱭᱩᱜ ᱠᱟᱱᱟ ᱢᱮᱥᱴ ᱡᱚᱭ ᱦᱤᱱᱫ ᱾"),
    ("TC2", "ᱡᱚᱦᱟᱨ"),
    ("TC3", "ᱛᱮᱦᱮᱧ ᱟᱢ ᱯᱟᱲᱦᱟᱣ ᱢᱮ ᱾"),
    ("TC4", "ᱵᱷᱟᱨᱚᱛ ᱫᱚ ᱢᱤᱫᱴᱟᱝ ᱢᱟᱨᱟᱝ ᱫᱤᱥᱚᱢ ᱾"),
    ("TC5", "ᱯᱚᱛᱚᱵ ᱫᱚ ᱮᱦᱚᱵ ᱢᱮ ᱟᱨ ᱚᱞ ᱫᱚ ᱯᱟᱲᱦᱟᱣ ᱢᱮ ᱾"),
    ("TC6", "ᱥᱮᱪᱮᱫ ᱦᱩᱭᱩᱜ ᱠᱟᱱᱟ ᱦᱚᱲ ᱠᱚᱣᱟᱜ ᱡᱤᱭᱚᱱ ᱨᱮᱭᱟᱜ ᱡᱚᱛᱚ ᱠᱷᱚᱱ ᱞᱟᱹᱠᱛᱤᱭᱟᱱ ᱫᱟᱲᱮ ᱟᱨ ᱱᱚᱶᱟ ᱫᱚ ᱤᱧᱟᱹᱜ ᱮ ᱪᱟᱞᱟᱣ ᱮᱫᱟ ᱾"),
    ("Aditya", "ᱤᱧᱟᱹᱜ ᱧᱩᱛᱩᱢ آدᱤᱛᱛᱚ ᱾"),
    ("Bharat", "ᱤᱧᱟᱹᱜ ᱵᱷᱟᱨᱚᱛ ᱟᱹᱰᱤ ᱢᱟᱨᱟᱝ ᱾")
]

print("=== SANTALI TTS TEST ===")
for label, text in santali_texts:
    # Text to phoneme IDs
    phoneme_ids = [bos_id]
    for ch in text:
        if ch in phoneme_id_map:
            phoneme_ids.extend(phoneme_id_map[ch])
            phoneme_ids.append(pad_id)
    phoneme_ids.append(eos_id)
    
    seq_len = len(phoneme_ids)
    input_ids = np.array([phoneme_ids], dtype=np.int64)
    input_lengths = np.array([seq_len], dtype=np.int64)
    scales = np.array([noise_scale, length_scale, noise_w], dtype=np.float32)
    
    outputs = tts_sess.run(None, {
        "input": input_ids,
        "input_lengths": input_lengths,
        "scales": scales
    })
    
    audio = outputs[0][0, 0, :]
    dur = len(audio) / sample_rate
    rms = float(np.sqrt(np.mean(audio**2)))
    peak = float(np.max(np.abs(audio)))
    print(f"[{label}] text: '{text}' -> samples: {len(audio)}, dur: {dur:.2f}s, peak: {peak:.4f}, rms: {rms:.4f}")
    assert len(audio) > 0, "No audio generated!"
    assert peak > 0.01, "Audio is silent!"
    assert not np.isnan(audio).any(), "Audio has NaNs!"
