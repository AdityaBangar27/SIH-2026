import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / "VaniShiksha" / "android_ai" / "conversion"))
from benchmark_translation import StandaloneOnnxTranslator

sys.stdout.reconfigure(encoding="utf-8")

translator = StandaloneOnnxTranslator()
text = "नमस्ते"
prefixed = f"hin_Deva sat_Olck {text.strip()}"
encoded = translator.src_tok.encode(prefixed)
print(f"Encoded tokens for '{prefixed}': {encoded.tokens}")
print(f"Encoded ids: {encoded.ids}")

# Let's inspect IndicTrans2 preprocessing!
# Does IndicTrans2 require specific punctuation or normalization?
# In IndicTrans2 official repo:
# IndicTrans2 uses IndicProcessor / indic_nlp normalization!
