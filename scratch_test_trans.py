import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent / "VaniShiksha" / "android_ai" / "conversion"))
from benchmark_translation import StandaloneOnnxTranslator

sys.stdout.reconfigure(encoding="utf-8")

translator = StandaloneOnnxTranslator()
test_sentences = [
    "मेरा भारत महान जय हिंद",
    "नमस्ते",
    "आज हम पढ़ाई करेंगे",
    "भारत एक महान देश है",
    "किताब खोलो और पाठ पढ़ो",
    "शिक्षा मनुष्य के जीवन में सबसे महत्वपूर्ण शक्ति है और यह हमें आगे बढ़ाती है",
    "मेरा नाम आदित्य है",
    "मेरा भारत महान"
]

print("=== TRANSLATION REFERENCE TEST ===")
for text in test_sentences:
    res = translator.translate(text)
    print(f"\nHindi:   '{text}'")
    print(f"Santali: '{res['translated_text']}'")
    print(f"Tokens:  {res['tokens_generated']}, Time: {res['total_sec']}s")
