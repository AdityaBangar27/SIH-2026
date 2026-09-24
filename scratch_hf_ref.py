import sys
import torch
import soundfile as sf
from transformers import WhisperForConditionalGeneration, WhisperProcessor

sys.stdout.reconfigure(encoding="utf-8")

processor = WhisperProcessor.from_pretrained("app/src/main/assets/tokenizers/asr")
model = WhisperForConditionalGeneration.from_pretrained("collabora/whisper-tiny-hindi")
model.eval()

test_files = [
    (1, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_1.wav", "मेरा भारत महान जय हिंद"),
    (2, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_2.wav", "नमस्ते"),
    (3, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_3.wav", "आज हम पढ़ाई करेंगे"),
    (4, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_4.wav", "भारत एक महान देश है"),
    (5, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_5.wav", "किताब खोलो और पाठ पढ़ो"),
    (6, "VaniShiksha/android_ai/samples/sample_audio/test_sentence_6.wav", "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है"),
]

forced_ids = processor.get_decoder_prompt_ids(language="hi", task="transcribe")
print("forced_ids:", forced_ids)
print("generation_config:", model.generation_config)

for tc_id, wav_path, expected in test_files:
    audio, sr = sf.read(wav_path)
    inputs = processor(audio, sampling_rate=16000, return_tensors="pt")
    with torch.no_grad():
        predicted_ids = model.generate(inputs.input_features, forced_decoder_ids=forced_ids)
    transcription = processor.batch_decode(predicted_ids, skip_special_tokens=True)[0].strip()
    all_tokens = predicted_ids[0].tolist()
    print(f"\n--- TC {tc_id} ---")
    print(f"Expected: '{expected}'")
    print(f"HF Gen:   '{transcription}'")
    print(f"Tokens:   {all_tokens}")
