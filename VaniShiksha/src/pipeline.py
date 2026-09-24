import os
import sys

# Ensure project root is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.translator import OfflineIndicTranslator
from src.config import HINDI_CODE, SANTALI_CODE, SUPPORTED_LANGUAGES, CODE_TO_NAME


class TranslationPipeline:
    """
    End-to-end Translation Pipeline for VaaniShiksha AI.
    Translates text between Hindi (hin_Deva) and Santali (sat_Olck) locally and offline.
    """

    def __init__(self, model_path=None):
        self.translator = OfflineIndicTranslator.get_instance(model_path=model_path)

    def run(self, input_text: str, source_language: str = "Hindi", target_language: str = "Santali"):
        """
        Execute translation for the given text and languages.

        Args:
            input_text (str): The raw text sentence to translate.
            source_language (str): 'Hindi' or 'Santali' (or language codes).
            target_language (str): 'Santali' or 'Hindi' (or language codes).

        Returns:
            dict: Translation results with actual measured latency.
        """
        src_code = SUPPORTED_LANGUAGES.get(source_language, source_language)
        tgt_code = SUPPORTED_LANGUAGES.get(target_language, target_language)

        result = self.translator.translate(input_text, source_lang=src_code, target_lang=tgt_code)

        return {
            "source_language": CODE_TO_NAME.get(src_code, src_code),
            "source_code": src_code,
            "source_text": input_text,
            "target_language": CODE_TO_NAME.get(tgt_code, tgt_code),
            "target_code": tgt_code,
            "translated_text": result["translated_text"],
            "model_load_time": self.translator.load_time,
            "translation_time": result["translation_time"],
        }
