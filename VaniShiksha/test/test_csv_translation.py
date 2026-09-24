import os
import sys
import time
from pathlib import Path
import pandas as pd
import sacrebleu

# Ensure UTF-8 console output on Windows
if sys.platform == "win32":
    sys.stdout.reconfigure(encoding="utf-8")

# Ensure root directory is on sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.pipeline import TranslationPipeline
from src.config import get_local_model_path, HINDI_CODE, SANTALI_CODE, MODELS_DIR


def find_csv_path():
    candidate_paths = [
        os.path.expanduser("~/Downloads/santali-train-hindi.csv"),
        os.path.abspath("santali-train-hindi.csv"),
        os.path.abspath("../santali-train-hindi.csv"),
        os.path.abspath("test/santali-train-hindi.csv"),
        os.path.expanduser("~/Desktop/santali-train-hindi.csv"),
    ]
    for p in candidate_paths:
        if os.path.exists(p):
            return p
    return None


def calculate_metrics(hypothesis_list, reference_list):
    """
    Computes Corpus BLEU and chrF++ using sacrebleu.
    """
    # References for sacrebleu must be list of lists: [[ref1, ref2, ...]]
    refs = [reference_list]
    
    # BLEU calculation
    bleu = sacrebleu.corpus_bleu(hypothesis_list, refs)
    
    # chrF++ calculation (word_order=2)
    chrf = sacrebleu.corpus_chrf(hypothesis_list, refs, word_order=2)
    
    # Exact match calculation
    exact_matches = sum(1 for h, r in zip(hypothesis_list, reference_list) if h.strip() == r.strip())
    exact_match_pct = (exact_matches / len(hypothesis_list)) * 100.0 if hypothesis_list else 0.0

    return {
        "bleu": round(bleu.score, 2),
        "chrf": round(chrf.score, 2),
        "exact_match_count": exact_matches,
        "exact_match_pct": round(exact_match_pct, 2),
    }


def main():
    print("=" * 65)
    print("VaaniShiksha AI: CSV DATASET TRANSLATION VALIDATION")
    print("=" * 65)

    model_path = get_local_model_path()
    print(f"MODEL STATUS    : {'LOADED' if model_path else 'NOT FOUND'}")
    print(f"MODEL PATH      : {model_path}")
    print(f"MODEL TYPE      : IndicTrans2 320M Indic-Indic INT8 ONNX (CPU)")
    print(f"LANGUAGE CODES  : Hindi = {HINDI_CODE}, Santali = {SANTALI_CODE}")
    print("-" * 65)

    if not model_path:
        print("[ERROR] Local translation model not found. Run download_model.py first.")
        sys.exit(1)

    csv_path = find_csv_path()
    if not csv_path:
        print("[ERROR] CSV file 'santali-train-hindi.csv' not found in Downloads or project root.")
        sys.exit(1)

    print(f"CSV DATASET PATH: {csv_path}")
    df_raw = pd.read_csv(csv_path)
    print(f"TOTAL ROWS IN CSV: {len(df_raw)}")
    print(f"COLUMNS         : {df_raw.columns.tolist()}")

    # Filter non-empty pairs
    df_valid = df_raw.dropna(subset=["hindi", "santali"]).copy()
    df_valid = df_valid[(df_valid["hindi"].str.strip() != "") & (df_valid["santali"].str.strip() != "")]
    print(f"VALID ROWS      : {len(df_valid)}")

    # Initialize translation pipeline
    pipeline = TranslationPipeline()

    # -------------------------------------------------------------
    # PHASE 1: 5-Sample Smoke Test
    # -------------------------------------------------------------
    print("\n" + "=" * 65)
    print("PHASE 1: 5-SAMPLE SMOKE TEST (Hindi -> Santali)")
    print("=" * 65)

    smoke_df = df_valid.head(5)
    smoke_passed = True

    for i, (_, row) in enumerate(smoke_df.iterrows(), 1):
        hi_text = str(row["hindi"]).strip()
        ref_sat = str(row["santali"]).strip()

        res = pipeline.run(hi_text, source_language="Hindi", target_language="Santali")
        pred_sat = res["translated_text"]

        print(f"\nTEST {i}:")
        print(f"  Hindi Input     : {hi_text}")
        print(f"  Model Santali   : {pred_sat}")
        print(f"  Reference Santali: {ref_sat}")
        print(f"  Latency         : {res['translation_time']:.3f} sec")

        if not pred_sat:
            smoke_passed = False

    if not smoke_passed:
        print("\n[ERROR] Smoke test produced empty output. Aborting full evaluation.")
        sys.exit(1)

    print("\n[SUCCESS] Smoke test passed! Proceeding to 100-sample evaluation...")

    # -------------------------------------------------------------
    # PHASE 2: 100-Sample Reproducible Validation
    # -------------------------------------------------------------
    sample_size = min(100, len(df_valid))
    df_sample = df_valid.sample(n=sample_size, random_state=42).reset_index(drop=True)

    print("\n" + "=" * 65)
    print(f"PHASE 2: FULL EVALUATION ON {sample_size} REPRODUCIBLE SAMPLES (random_state=42)")
    print("=" * 65)

    results_records = []
    hi_preds, hi_refs = [], []
    sat_preds, sat_refs = [], []
    hi_times, sat_times = [], []

    print(f"Evaluating {sample_size} translation pairs in both directions...")

    for idx, row in df_sample.iterrows():
        sample_id = row.get("id", idx + 1)
        hi_ref = str(row["hindi"]).strip()
        sat_ref = str(row["santali"]).strip()

        # 1. Hindi -> Santali
        res_hi2sat = pipeline.run(hi_ref, source_language="Hindi", target_language="Santali")
        pred_sat = res_hi2sat["translated_text"]
        t_hi2sat = res_hi2sat["translation_time"]

        # Sentence-level chrF++
        chrf_hi2sat = round(sacrebleu.sentence_chrf(pred_sat, [sat_ref], word_order=2).score, 2)
        em_hi2sat = (pred_sat == sat_ref)

        # 2. Santali -> Hindi
        res_sat2hi = pipeline.run(sat_ref, source_language="Santali", target_language="Hindi")
        pred_hi = res_sat2hi["translated_text"]
        t_sat2hi = res_sat2hi["translation_time"]

        # Sentence-level chrF++
        chrf_sat2hi = round(sacrebleu.sentence_chrf(pred_hi, [hi_ref], word_order=2).score, 2)
        em_sat2hi = (pred_hi == hi_ref)

        hi_preds.append(pred_sat)
        hi_refs.append(sat_ref)
        hi_times.append(t_hi2sat)

        sat_preds.append(pred_hi)
        sat_refs.append(hi_ref)
        sat_times.append(t_sat2hi)

        results_records.append({
            "Sample_ID": sample_id,
            "Hindi_Reference": hi_ref,
            "Santali_Reference": sat_ref,
            "Model_Hindi_to_Santali": pred_sat,
            "H2S_chrF": chrf_hi2sat,
            "H2S_ExactMatch": em_hi2sat,
            "H2S_Latency_Sec": t_hi2sat,
            "Model_Santali_to_Hindi": pred_hi,
            "S2H_chrF": chrf_sat2hi,
            "S2H_ExactMatch": em_sat2hi,
            "S2H_Latency_Sec": t_sat2hi,
        })

        if (idx + 1) % 20 == 0 or (idx + 1) == sample_size:
            print(f"  Processed {idx + 1}/{sample_size} pairs...")

    # Save detailed evaluation report CSV
    report_csv_path = os.path.abspath("test/csv_validation_report.csv")
    df_results = pd.DataFrame(results_records)
    df_results.to_csv(report_csv_path, index=False, encoding="utf-8-sig")
    print(f"\n[REPORT SAVED]: {report_csv_path}")

    # Compute overall metrics
    metrics_h2s = calculate_metrics(hi_preds, hi_refs)
    metrics_s2h = calculate_metrics(sat_preds, sat_refs)

    avg_t_h2s = sum(hi_times) / len(hi_times) if hi_times else 0.0
    avg_t_s2h = sum(sat_times) / len(sat_times) if sat_times else 0.0

    print("\n" + "=" * 65)
    print("FINAL EVALUATION METRICS SUMMARY")
    print("=" * 65)
    print(f"Evaluation Samples Tested  : {sample_size} pairs")
    print(f"Random Seed (Reproducible) : 42\n")

    print("--- 1. HINDI -> SANTALI (hin_Deva -> sat_Olck) ---")
    print(f"  Corpus BLEU Score        : {metrics_h2s['bleu']}")
    print(f"  Corpus chrF++ Score      : {metrics_h2s['chrf']}")
    print(f"  Exact Match Percentage   : {metrics_h2s['exact_match_pct']}% ({metrics_h2s['exact_match_count']}/{sample_size})")
    print(f"  Average Latency per Item : {avg_t_h2s:.3f} sec")

    print("\n--- 2. SANTALI -> HINDI (sat_Olck -> hin_Deva) ---")
    print(f"  Corpus BLEU Score        : {metrics_s2h['bleu']}")
    print(f"  Corpus chrF++ Score      : {metrics_s2h['chrf']}")
    print(f"  Exact Match Percentage   : {metrics_s2h['exact_match_pct']}% ({metrics_s2h['exact_match_count']}/{sample_size})")
    print(f"  Average Latency per Item : {avg_t_s2h:.3f} sec")
    print("=" * 65)

    print("\n--- 5 SAMPLE TRANSLATIONS FROM VALIDATION DATASET ---")
    for i in range(min(5, len(results_records))):
        rec = results_records[i]
        print(f"\n[SAMPLE {i+1}] (ID: {rec['Sample_ID']})")
        print(f"  Hindi Ref         : {rec['Hindi_Reference']}")
        print(f"  Model (Hi -> San) : {rec['Model_Hindi_to_Santali']}")
        print(f"  Santali Ref       : {rec['Santali_Reference']}")
        print(f"  Model (San -> Hi) : {rec['Model_Santali_to_Hindi']}")
        print(f"  Scores            : H2S chrF++: {rec['H2S_chrF']} | S2H chrF++: {rec['S2H_chrF']}")


if __name__ == "__main__":
    main()
