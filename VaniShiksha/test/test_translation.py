import os
import sys

# Configure UTF-8 encoding on Windows console
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

# Ensure root directory is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.pipeline import TranslationPipeline


def main():
    print("==================================================")
    print("VaaniShiksha AI: OFFLINE HINDI <-> SANTALI TRANSLATION TEST")
    print("==================================================")

    try:
        pipeline = TranslationPipeline()
    except Exception as e:
        print(f"\n[Initialization Error]: {e}\n")
        sys.exit(1)

    test_cases = [
        ("Hindi", "Santali", "नमस्ते, आप कैसे हैं?"),
        ("Hindi", "Santali", "शिक्षा बच्चों के भविष्य के लिए बहुत महत्वपूर्ण है।"),
        ("Santali", "Hindi", "ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?"),
        ("Santali", "Hindi", "ᱥᱮᱪᱮᱫ ᱫᱚ ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚᱣᱟᱜ ᱟᱜᱟᱢ ᱞᱟᱹᱜᱤᱫ ᱟᱹᱰᱤ ᱡᱟᱹᱨᱩᱲᱟᱱ ᱠᱟᱱᱟ᱾"),
    ]

    for idx, (src_lang, tgt_lang, text) in enumerate(test_cases, 1):
        print(f"\n--- TEST CASE {idx} ---")
        res = pipeline.run(text, source_language=src_lang, target_language=tgt_lang)

        print(f"SOURCE LANGUAGE : {res['source_language']} ({res['source_code']})")
        print(f"SOURCE TEXT     : {res['source_text']}")
        print(f"TARGET LANGUAGE : {res['target_language']} ({res['target_code']})")
        print(f"TRANSLATED TEXT : {res['translated_text']}")
        print(f"MODEL LOAD TIME : {res['model_load_time']:.2f} sec")
        print(f"TRANSLATION TIME: {res['translation_time']:.3f} sec")

    print("\n==================================================")
    print("TEST STATUS: ALL TESTS COMPLETED LOCALLY & OFFLINE")
    print("==================================================")


if __name__ == "__main__":
    main()
