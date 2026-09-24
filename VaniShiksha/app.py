import os
import sys
import time
import traceback
from pathlib import Path
import streamlit as st

# Ensure root directory is on sys.path
ROOT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT_DIR))

from src.hardware import get_hardware_info, get_system_device
from src.pipeline import TranslationPipeline
from src.voice_pipeline import VoiceTranslationPipeline
from src.config import get_local_model_path, HINDI_CODE, SANTALI_CODE
from src.audio_validator import validate_audio_file
from src.problems_data import KEY_HIGHLIGHT_PROBLEMS, ALL_64_PROBLEMS

# Page Configuration
st.set_page_config(
    page_title="VaaniShiksha AI – Offline Hindi ↔ Santali Voice Translation",
    page_icon="🌿",
    layout="wide",
    initial_sidebar_state="expanded"
)


def calculate_total_time(asr_time: float, translation_time: float, tts_time: float) -> float:
    """Calculate total voice pipeline processing time."""
    return asr_time + translation_time + tts_time


# Custom Tribal Heritage + Modern AI Theme CSS
st.markdown("""
<style>
    :root {
        --forest-green: #1b4332;
        --terracotta: #c85a32;
        --warm-cream: #fdfaf6;
        --earth-brown: #4a2c11;
        --accent-gold: #d4a373;
        --card-bg: rgba(255, 255, 255, 0.92);
        --border-color: #e5d9c5;
    }
    
    .stApp {
        background: linear-gradient(135deg, #fdfaf6 0%, #f4eee1 100%);
        color: #2d261e;
        font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
    }
    
    .hero-header {
        background: linear-gradient(120deg, #1b4332 0%, #2d5a27 60%, #c85a32 100%);
        color: #ffffff;
        padding: 24px 30px;
        border-radius: 16px;
        box-shadow: 0 10px 25px rgba(27, 67, 50, 0.15);
        margin-bottom: 24px;
        border: 1px solid rgba(255, 255, 255, 0.15);
    }
    
    .hero-title {
        font-size: 2.2rem;
        font-weight: 800;
        margin: 0;
        letter-spacing: -0.5px;
        display: flex;
        align-items: center;
        gap: 12px;
    }
    
    .hero-subtitle {
        font-size: 1.05rem;
        margin-top: 6px;
        color: #e8f5e9;
        font-weight: 400;
    }
    
    .status-card {
        background: var(--card-bg);
        border: 1px solid var(--border-color);
        border-radius: 12px;
        padding: 16px 20px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.03);
        margin-bottom: 20px;
    }
    
    .step-box {
        background: #ffffff;
        border-left: 4px solid #1b4332;
        border-radius: 8px;
        padding: 12px 16px;
        margin-bottom: 8px;
        font-size: 0.95rem;
        box-shadow: 0 2px 6px rgba(0,0,0,0.04);
    }
    
    .step-active {
        border-left-color: #c85a32;
        background: #fff8f5;
        font-weight: 600;
    }
    
    .step-done {
        border-left-color: #2e7d32;
        background: #f1f8f3;
    }
    
    .step-pending {
        border-left-color: #bdbdbd;
        color: #757575;
    }
    
    .metric-badge {
        display: inline-block;
        padding: 4px 10px;
        border-radius: 20px;
        font-size: 0.85rem;
        font-weight: 600;
    }
    
    .badge-success { background: #e8f5e9; color: #1b5e20; }
    .badge-warning { background: #fff3e0; color: #e65100; }
    .badge-info { background: #e3f2fd; color: #0d47a1; }
    .badge-danger { background: #ffebee; color: #b71c1c; }

    /* Problem Section Inline Cards */
    .problem-card {
        background: #ffffff;
        border: 1px solid var(--border-color);
        border-top: 4px solid var(--forest-green);
        border-radius: 12px;
        padding: 18px 20px;
        margin-bottom: 16px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.03);
        transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .problem-card:hover {
        transform: translateY(-2px);
        box-shadow: 0 6px 16px rgba(0,0,0,0.06);
    }
    .problem-card-highlight {
        border-top-color: var(--terracotta);
        background: #fffdfb;
    }
    .problem-header-row {
        display: flex;
        justify-content: space-between;
        align-items: flex-start;
        margin-bottom: 10px;
    }
    .problem-title {
        font-size: 1.05rem;
        font-weight: 700;
        color: var(--forest-green);
        margin: 0;
    }
    .problem-tag {
        font-size: 0.75rem;
        font-weight: 700;
        text-transform: uppercase;
        padding: 3px 8px;
        border-radius: 6px;
        background: #f0e6d6;
        color: #4a2c11;
    }
    .problem-desc {
        font-size: 0.9rem;
        color: #444;
        line-height: 1.45;
        margin-bottom: 8px;
    }
    .problem-impact {
        font-size: 0.85rem;
        color: #b71c1c;
        font-weight: 600;
        margin-bottom: 6px;
    }
    .problem-sol {
        font-size: 0.85rem;
        color: #1b5e20;
        background: #f1f8f3;
        padding: 8px 12px;
        border-radius: 6px;
        margin-top: 6px;
    }
</style>
""", unsafe_allow_html=True)

# Hardware Detection
hw_info = get_hardware_info()

# Header
st.markdown("""
<div class="hero-header">
    <div class="hero-title">
        <span>🌿 VaaniShiksha AI</span>
    </div>
    <div class="hero-subtitle">
        Offline AI Teaching Assistant for Mother-Tongue Primary Education (Hindi ↔ Santali Ol Chiki)
    </div>
</div>
""", unsafe_allow_html=True)

# Sidebar: System Status & Configuration
with st.sidebar:
    st.markdown("### ⚙️ System & AI Diagnostics")
    
    # Mode Selector
    pipeline_mode = st.radio(
        "⚡ Execution Mode",
        ["⚡ Fast Demo (Recommended)", "🧠 Full Quality"],
        index=0,
        help="Fast Demo uses optimized token bounds for responsive voice turn-around. Full Quality runs extended autoregressive generation."
    )
    is_fast_mode = "Fast" in pipeline_mode
    mode_str = "fast" if is_fast_mode else "full"
    
    speaker_choice = st.selectbox(
        "🗣️ Santali Voice Speaker",
        ["female (Sumitra)", "male (Raju)"],
        index=0
    )
    speaker_str = "female" if "female" in speaker_choice else "male"
    
    st.markdown("---")
    st.markdown("#### 💻 Hardware Detection")
    st.markdown(f"**Device:** `{hw_info['device']}` ({hw_info['device_name']})")
    st.markdown(f"**Python:** `{hw_info['python_version']}` | **PyTorch:** `{hw_info['torch_version']}`")
    st.markdown(f"**CPU Cores:** `{hw_info['cpu_count']}`")
    
    if hw_info["flash_attn_available"]:
        st.success("✅ Flash Attention 2: Active")
    else:
        st.info("ℹ️ Flash Attention 2: Standard Attention Active (CPU)")
        
    st.markdown("---")
    st.markdown("#### 🔒 Offline Security")
    st.success("✅ 100% Offline Mode (Zero Cloud Dependency)")


# Cached Pipeline Loaders (Loaded ONCE into persistent memory)
@st.cache_resource(show_spinner="Loading IndicTrans2 ONNX INT8 Engine...")
def get_text_pipeline():
    return TranslationPipeline()


@st.cache_resource(show_spinner="Loading VaaniShiksha Voice Pipeline (Whisper ASR, MMS-TTS, Indic Parler-TTS)...")
def get_voice_pipeline():
    pipe = VoiceTranslationPipeline.get_instance()
    # Preload models once during startup
    pipe.preload_all()
    return pipe


text_pipeline = get_text_pipeline()
voice_pipeline = get_voice_pipeline()

# Check model readiness status
_asr_hi_ok = voice_pipeline.asr._hindi_model is not None
_trans_ok = text_pipeline.translator is not None
_tts_sat_ok = voice_pipeline.tts._santali_model is not None and voice_pipeline.tts._santali_desc_tokenizer is not None
_tts_hi_ok = voice_pipeline.tts._hindi_model is not None

# Top Status Banner: MODEL STATUS
col_s1, col_s2, col_s3, col_s4 = st.columns(4)
with col_s1:
    badge_cls = "badge-success" if _asr_hi_ok else "badge-warning"
    status_txt = "✓ Hindi ASR loaded" if _asr_hi_ok else "⏳ Loading..."
    st.markdown(f"""<div class="status-card">
        <div style="font-size:0.85rem; color:#666;">ASR Engine</div>
        <div style="font-weight:700; color:#1b4332;">Whisper-Tiny Hindi</div>
        <span class="metric-badge {badge_cls}">{status_txt}</span>
    </div>""", unsafe_allow_html=True)

with col_s2:
    badge_cls = "badge-success" if _trans_ok else "badge-warning"
    status_txt = "✓ IndicTrans2 loaded" if _trans_ok else "⏳ Loading..."
    st.markdown(f"""<div class="status-card">
        <div style="font-size:0.85rem; color:#666;">Translation Engine</div>
        <div style="font-weight:700; color:#1b4332;">IndicTrans2 320M INT8</div>
        <span class="metric-badge {badge_cls}">{status_txt}</span>
    </div>""", unsafe_allow_html=True)

with col_s3:
    badge_cls = "badge-success" if _tts_sat_ok else "badge-warning"
    status_txt = "✓ Santali TTS loaded" if _tts_sat_ok else "⏳ Loading..."
    st.markdown(f"""<div class="status-card">
        <div style="font-size:0.85rem; color:#666;">Santali TTS Engine</div>
        <div style="font-weight:700; color:#1b4332;">Indic Parler-TTS</div>
        <span class="metric-badge {badge_cls}">{status_txt}</span>
    </div>""", unsafe_allow_html=True)

with col_s4:
    badge_cls = "badge-success" if _tts_hi_ok else "badge-warning"
    status_txt = "✓ Hindi TTS loaded" if _tts_hi_ok else "⏳ Loading..."
    st.markdown(f"""<div class="status-card">
        <div style="font-size:0.85rem; color:#666;">Hindi TTS Engine</div>
        <div style="font-weight:700; color:#1b4332;">Meta MMS-TTS</div>
        <span class="metric-badge {badge_cls}">{status_txt}</span>
    </div>""", unsafe_allow_html=True)


# Main Tabs
tab_voice, tab_text, tab_problems, tab_debug = st.tabs([
    "🎙️ Voice Translation (Voice-to-Voice)",
    "📝 Text Translation",
    "🎯 Problem Statement (Challenges & Solutions)",
    "🧪 Diagnostic Test Suite"
])

LANG_OPTIONS = ["Hindi (hin_Deva)", "Santali (sat_Olck)"]

# =====================================================================
# TAB 1: VOICE TRANSLATION (Voice -> ASR -> Translation -> TTS -> Audio)
# =====================================================================
with tab_voice:
    st.markdown("### 🎙️ Offline Voice-to-Voice Translation")
    st.caption("Live Voice Input → Hindi Whisper ASR → IndicTrans2 ONNX → Santali Indic Parler-TTS → Audio Response")

    col_v1, col_v2 = st.columns(2)
    with col_v1:
        v_src_label = st.selectbox("Speech Input Language", LANG_OPTIONS, index=0, key="v_src_lang")
    with col_v2:
        default_v_tgt_idx = 1 if "Hindi" in v_src_label else 0
        v_tgt_label = st.selectbox("Voice Output Language", LANG_OPTIONS, index=default_v_tgt_idx, key="v_tgt_lang")

    v_src_clean = "Hindi" if "Hindi" in v_src_label else "Santali"
    v_tgt_clean = "Santali" if "Santali" in v_tgt_label else "Hindi"

    # Audio Recording & Upload Section
    col_input_mic, col_input_file = st.columns(2)
    with col_input_mic:
        st.markdown("**1. Record Speech (Max 15 seconds):**")
        audio_recorded = st.audio_input("Click the microphone to record:", key="v_audio_mic")
    with col_input_file:
        st.markdown("**OR 2. Upload Audio File (WAV / MP3):**")
        audio_uploaded = st.file_uploader("Upload audio file:", type=["wav", "mp3", "ogg"], key="v_audio_upload")

    active_audio = audio_recorded or audio_uploaded

    if active_audio is not None:
        st.audio(active_audio)

    # Voice Translation Action Button
    if st.button("🚀 Process Voice Pipeline", type="primary", use_container_width=True, key="btn_run_voice"):
        if active_audio is None:
            st.warning("Please record or upload audio first.")
        elif v_src_clean == v_tgt_clean:
            st.error("Input and Output languages must be different.")
        else:
            progress_container = st.container()
            with progress_container:
                st.markdown("#### ⏳ Pipeline Progress")
                step_box_1 = st.empty()
                step_box_2 = st.empty()
                step_box_3 = st.empty()
                step_box_4 = st.empty()
                step_box_5 = st.empty()

                step_box_1.markdown('<div class="step-box step-done">✓ 1. Audio recorded & preprocessed (16kHz Mono, silence trimmed)</div>', unsafe_allow_html=True)
                step_box_2.markdown(f'<div class="step-box step-active">● 2. Speech Recognition ({v_src_clean} Whisper ASR running)...</div>', unsafe_allow_html=True)
                step_box_3.markdown('<div class="step-box step-pending">○ 3. Translation (IndicTrans2 ONNX)</div>', unsafe_allow_html=True)
                step_box_4.markdown(f'<div class="step-box step-pending">○ 4. Speech Synthesis ({v_tgt_clean} TTS)</div>', unsafe_allow_html=True)
                step_box_5.markdown('<div class="step-box step-pending">○ 5. Voice Response Ready</div>', unsafe_allow_html=True)

            try:
                audio_bytes = active_audio.getvalue()
                temp_audio_dir = ROOT_DIR / "audio"
                temp_audio_dir.mkdir(parents=True, exist_ok=True)
                temp_audio_path = temp_audio_dir / "temp_voice_input.wav"
                with open(temp_audio_path, "wb") as f:
                    f.write(audio_bytes)

                # Step 1: ASR
                t_asr_0 = time.time()
                asr_res = voice_pipeline.asr.transcribe(str(temp_audio_path), language=v_src_clean, mode=mode_str)
                recognized_text = asr_res["text"]
                asr_time = asr_res["latency"]
                asr_diag = asr_res.get("diagnostics", {})

                step_box_2.markdown(f'<div class="step-box step-done">✓ 2. Speech Recognized ({asr_time:.2f}s, Input: {asr_diag.get("processed_duration", asr_res.get("audio_duration", 0)):.2f}s): <b>"{recognized_text}"</b></div>', unsafe_allow_html=True)
                step_box_3.markdown('<div class="step-box step-active">● 3. Translating with IndicTrans2 ONNX INT8...</div>', unsafe_allow_html=True)

                if not recognized_text.strip():
                    st.error("No speech recognized. Please speak clearly into the microphone.")
                    st.stop()

                # Step 2: Translation
                t_trans_0 = time.time()
                trans_res = text_pipeline.run(recognized_text, source_language=v_src_clean, target_language=v_tgt_clean)
                translated_text = trans_res["translated_text"]
                trans_time = trans_res["translation_time"]

                step_box_3.markdown(f'<div class="step-box step-done">✓ 3. Translated ({trans_time:.2f}s): <b>"{translated_text}"</b></div>', unsafe_allow_html=True)
                step_box_4.markdown(f'<div class="step-box step-active">● 4. Synthesizing {v_tgt_clean} Voice Response...</div>', unsafe_allow_html=True)

                # Step 3: TTS
                t_tts_0 = time.time()
                tts_res = voice_pipeline.tts.synthesize(
                    translated_text,
                    language=v_tgt_clean,
                    mode=mode_str,
                    speaker=speaker_str
                )
                tts_time = tts_res.get("latency", 0.0)
                audio_path = tts_res.get("audio_path")
                tts_success = tts_res.get("success", False)
                validation = tts_res.get("validation", {})
                is_audio_valid = validation.get("valid_audio", False)

                if tts_success and is_audio_valid and audio_path and os.path.exists(audio_path):
                    step_box_4.markdown(f'<div class="step-box step-done">✓ 4. {v_tgt_clean} Voice Synthesized ({tts_time:.2f}s, {validation["duration"]:.2f}s audio)</div>', unsafe_allow_html=True)
                    step_box_5.markdown('<div class="step-box step-done">✓ 5. Voice Response Ready for Playback!</div>', unsafe_allow_html=True)
                else:
                    reason = validation.get("reason", tts_res.get("error", "Unknown validation error"))
                    step_box_4.markdown(f'<div class="step-box step-active">❌ 4. {v_tgt_clean} TTS FAILED: generated audio is empty or invalid ({reason})</div>', unsafe_allow_html=True)
                    step_box_5.markdown('<div class="step-box step-done">✓ 5. Translation Complete (Text Display Active)</div>', unsafe_allow_html=True)

                total_time = asr_time + trans_time + tts_time

                # Audio Diagnostics Box (Task 10)
                st.write("---")
                st.markdown("#### 🎙️ Audio Diagnostics")
                d1, d2, d3, d4, d5 = st.columns(5)
                d1.metric("Recorded duration", f"{asr_diag.get('raw_duration', 0.0):.2f} sec")
                d2.metric("Sample rate", f"{asr_diag.get('sample_rate', 16000)} Hz")
                d3.metric("Channels", f"{asr_diag.get('channels', 1)}")
                d4.metric("Samples", f"{asr_diag.get('raw_samples', 0)}")
                d5.metric("ASR input duration", f"{asr_diag.get('processed_duration', 0.0):.2f} sec")

                # Pipeline flow presentation: Hindi ASR -> Santali Translated -> Santali TTS -> Audio Player
                st.write("---")
                st.subheader("🎯 Translation Results")
                
                col_res1, col_res2 = st.columns(2)
                with col_res1:
                    st.markdown(f"**Hindi Recognized Speech (ASR):**")
                    st.info(f"🗣️ **{recognized_text}**")
                with col_res2:
                    st.markdown(f"**Santali Translated Text (IndicTrans2):**")
                    st.success(f"📖 **{translated_text}**")

                st.write("---")
                st.markdown(f"### 🔊 {v_tgt_clean} Voice Output (TTS)")

                file_size_kb = (validation.get("file_size", 0) / 1024) if validation else 0.0

                if tts_success and is_audio_valid and audio_path and os.path.exists(audio_path):
                    st.success(f"✓ Audio generated\n✓ Audio validated\n\n**Duration:** {validation['duration']:.2f} seconds | **File size:** {file_size_kb:.1f} KB")
                    
                    # Read bytes to ensure Streamlit receives genuine audio buffer
                    with open(audio_path, "rb") as f_aud:
                        wav_bytes = f_aud.read()
                    st.audio(wav_bytes, format="audio/wav")
                else:
                    st.error(f"✗ Audio generation failed: {validation.get('reason', 'audio check failed')}")

                # Live Benchmark
                st.write("---")
                st.markdown("#### ⏱️ TTS & PIPELINE BENCHMARK")
                m1, m2, m3, m4 = st.columns(4)
                m1.metric("TTS Status", "READY" if (tts_success and is_audio_valid) else "FAILED")
                m2.metric("TTS Gen Time", f"{tts_time:.2f} s")
                m3.metric("Audio Duration", f"{validation.get('duration', 0.0):.2f} s")
                m4.metric("Audio File Size", f"{file_size_kb:.1f} KB")

                # Detailed Diagnostics
                if validation:
                    translation_time = trans_time
                    total_time = calculate_total_time(
                        asr_time,
                        translation_time,
                        tts_time
                    )

                    v1, v2, v3, v4 = st.columns(4)
                    v1.metric("ASR", f"{asr_time:.2f} s")
                    v2.metric("Translation", f"{translation_time:.2f} s")
                    v3.metric("Total Processing", f"{total_time:.2f} s")
                    v4.metric("RMS Energy", f"{validation.get('rms', 0.0):.4f}")

            except Exception as e:
                st.error(f"Voice Pipeline Error: {e}")
                st.code(traceback.format_exc(), language="python")


# =====================================================================
# TAB 2: TEXT TRANSLATION
# =====================================================================
with tab_text:
    st.markdown("### 📝 Hindi ↔ Santali Text Translation")
    st.caption("Direct text-to-text translation using offline IndicTrans2 ONNX INT8 engine")

    col_t_s1, col_t_s2 = st.columns(2)
    with col_t_s1:
        t_src = st.selectbox("Source Language", LANG_OPTIONS, index=0, key="t_src_sel")
    with col_t_s2:
        t_tgt = st.selectbox("Target Language", LANG_OPTIONS, index=1 if "Hindi" in t_src else 0, key="t_tgt_sel")

    t_src_clean = "Hindi" if "Hindi" in t_src else "Santali"
    t_tgt_clean = "Santali" if "Santali" in t_tgt else "Hindi"

    # Quick Presets
    preset_samples = {
        "Hindi": [
            "नमस्ते, आप कैसे हैं?",
            "शिक्षा बच्चों के भविष्य के लिए बहुत महत्वपूर्ण है।",
            "आज हम गणित और विज्ञान का पाठ पढ़ेंगे।",
        ],
        "Santali": [
            "ᱡᱚᱦᱟᱨ, ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?",
            "ᱥᱮᱪᱮᱫ ᱫᱚ ᱜᱤᱫᱽᱨᱟᱹ ᱠᱚᱣᱟᱜ ᱟᱜᱟᱢ ᱞᱟᱹᱜᱤᱫ ᱟᱹᱰᱤ ᱡᱟᱹᱨᱩᱲᱟᱱ ᱠᱟᱱᱟ᱾",
            "ᱛᱮᱦᱮᱧ ᱟᱵᱚ ᱮᱞᱠᱷᱟ ᱟᱨ ᱥᱟᱬᱮᱥ ᱵᱚᱱ ᱯᱟᱲᱦᱟᱣᱜᱼᱟ ᱾",
        ]
    }

    if "txt_input_val" not in st.session_state:
        st.session_state.txt_input_val = ""

    def set_text_preset(txt: str):
        st.session_state.txt_input_val = txt

    st.markdown("**Quick Presets:**")
    p_cols = st.columns(len(preset_samples[t_src_clean]))
    for idx, sample in enumerate(preset_samples[t_src_clean], 1):
        p_cols[idx - 1].button(f"Preset {idx}", key=f"p_btn_{t_src_clean}_{idx}", on_click=set_text_preset, args=(sample,), use_container_width=True)

    text_input = st.text_area(f"Enter {t_src_clean} text:", key="txt_input_val", height=120, placeholder=f"Type {t_src_clean} text...")

    if st.button("Translate Text", type="primary", use_container_width=True, key="btn_translate_direct"):
        if not text_input.strip():
            st.warning("Please enter text to translate.")
        elif t_src_clean == t_tgt_clean:
            st.error("Source and Target languages must be different.")
        else:
            with st.spinner("Translating with IndicTrans2 ONNX..."):
                t_res = text_pipeline.run(text_input.strip(), source_language=t_src_clean, target_language=t_tgt_clean)

            st.write("---")
            st.subheader("Translation Results")
            st.markdown(f"**Input ({t_src_clean}):**")
            st.info(f"{t_res['source_text']}")
            st.markdown(f"**Output ({t_tgt_clean}):**")
            st.success(f"{t_res['translated_text']}")
            st.caption(f"⏱️ Translation time: {t_res['translation_time']:.3f}s | Engine: IndicTrans2 ONNX INT8")


# =====================================================================
# TAB 3: PROBLEM STATEMENT & EDUCATIONAL CHALLENGES
# =====================================================================
with tab_problems:
    st.markdown("### 🎯 Mother-Tongue Primary Education: The Problem Statement")
    st.caption("Addressing the acute linguistic barrier, bilingual teacher shortage, and offline edge AI constraints for 7.6M+ Santali speakers.")

    # High-level Metrics Row
    m_col1, m_col2, m_col3, m_col4 = st.columns(4)
    with m_col1:
        st.metric("Identified Challenges", "64 Issues", "Across 5 Domains")
    with m_col2:
        st.metric("Affected Demographics", "7.6M+ Speakers", "JH / WB / OD / BH")
    with m_col3:
        st.metric("Primary Dropout Gap", "2.5x Higher", "Due to Language Shock")
    with m_col4:
        st.metric("Cloud Connectivity", "0% Required", "100% Offline AI Solution")

    st.write("---")
    st.markdown("#### 🌟 Key Core Challenges (Top 6 Highlights)")

    # 3-column responsive card grid for the Top 6 Highlights
    # Row 1: Problems 1, 2, 3
    col_p1, col_p2, col_p3 = st.columns(3)
    for idx, p in enumerate(KEY_HIGHLIGHT_PROBLEMS[:3]):
        target_col = [col_p1, col_p2, col_p3][idx]
        with target_col:
            st.markdown(f"""
            <div class="problem-card problem-card-highlight">
                <div class="problem-header-row">
                    <h4 class="problem-title">#{p['id']} {p['title']}</h4>
                    <span class="problem-tag">{p['tag']}</span>
                </div>
                <div class="problem-desc">{p['desc']}</div>
                <div class="problem-impact">⚠️ <b>Impact:</b> {p['impact']}</div>
                <div class="problem-sol">💡 <b>VaaniShiksha AI:</b> {p['solution']}</div>
            </div>
            """, unsafe_allow_html=True)

    # Row 2: Problems 4, 5, 6
    col_p4, col_p5, col_p6 = st.columns(3)
    for idx, p in enumerate(KEY_HIGHLIGHT_PROBLEMS[3:6]):
        target_col = [col_p4, col_p5, col_p6][idx]
        with target_col:
            st.markdown(f"""
            <div class="problem-card problem-card-highlight">
                <div class="problem-header-row">
                    <h4 class="problem-title">#{p['id']} {p['title']}</h4>
                    <span class="problem-tag">{p['tag']}</span>
                </div>
                <div class="problem-desc">{p['desc']}</div>
                <div class="problem-impact">⚠️ <b>Impact:</b> {p['impact']}</div>
                <div class="problem-sol">💡 <b>VaaniShiksha AI:</b> {p['solution']}</div>
            </div>
            """, unsafe_allow_html=True)

    st.write("---")

    # State management for View All Problems
    if "show_all_problems" not in st.session_state:
        st.session_state.show_all_problems = False

    def toggle_problems_view():
        st.session_state.show_all_problems = not st.session_state.show_all_problems

    btn_label = "📁 Show Key 6 Problems Only" if st.session_state.show_all_problems else "📂 View All 64 Identified Educational & AI Challenges"
    st.button(btn_label, on_click=toggle_problems_view, type="secondary" if st.session_state.show_all_problems else "primary", use_container_width=True, key="btn_toggle_problems_inline")

    # When expanded, display all 64 categorized problems directly within page
    if st.session_state.show_all_problems:
        st.markdown("### 📚 Comprehensive 64 Problem Breakdown by Domain")
        st.caption("All 64 identified challenges displayed inline. Click any domain to explore obstacles, educational impacts, and offline AI solutions.")

        categories = [
            ("🎓 1. Primary Classroom & Pedagogical Barriers", "Pedagogical & Classroom Barriers"),
            ("🔤 2. Ol Chiki vs Devanagari Linguistic & Script Complexities", "Linguistic & Script Complexities"),
            ("🤖 3. Low-Resource Speech & NLP Obstacles", "Low-Resource Speech & NLP Obstacles"),
            ("⚡ 4. Edge Device & Offline 2GB RAM Hardware Limits", "Edge Hardware & Offline Limits"),
            ("🏛️ 5. Policy, NEP 2020 & Socio-Economic Hurdles", "Policy & Socio-Economic Hurdles"),
        ]

        for cat_title, cat_filter in categories:
            cat_items = [p for p in ALL_64_PROBLEMS if p["category"] == cat_filter]
            with st.expander(f"{cat_title} ({len(cat_items)} Problems)", expanded=False):
                # 2-column clean card layout inside category
                cat_col1, cat_col2 = st.columns(2)
                for idx, p in enumerate(cat_items):
                    col = cat_col1 if idx % 2 == 0 else cat_col2
                    with col:
                        st.markdown(f"""
                        <div class="problem-card">
                            <div class="problem-header-row">
                                <h4 class="problem-title">#{p['id']} {p['title']}</h4>
                                <span class="problem-tag">{p['tag']}</span>
                            </div>
                            <div class="problem-desc">{p['desc']}</div>
                            <div class="problem-impact">⚠️ <b>Impact:</b> {p['impact']}</div>
                            <div class="problem-sol">💡 <b>VaaniShiksha AI:</b> {p['solution']}</div>
                        </div>
                        """, unsafe_allow_html=True)


# =====================================================================
# TAB 4: DIAGNOSTIC TEST SUITE
# =====================================================================
with tab_debug:
    st.markdown("### 🧪 Component Diagnostic & Benchmark Suite")
    st.caption("Test individual pipeline stages in isolation with verified audio diagnostics.")

    AUDIO_TEST_DIR = ROOT_DIR / "audio" / "test"
    AUDIO_TEST_DIR.mkdir(parents=True, exist_ok=True)
    test_hindi_path = AUDIO_TEST_DIR / "hindi_sample.wav"

    col_b1, col_b2, col_b3, col_b4 = st.columns(4)

    # 1. Test ASR
    with col_b1:
        if st.button("1. Test Hindi ASR", use_container_width=True, key="btn_test_asr"):
            if not test_hindi_path.exists():
                st.warning("Generating test audio...")
                voice_pipeline.tts.synthesize_hindi("नमस्ते आप कैसे हैं", output_filename="hindi_sample.wav")
                import shutil
                shutil.copy(str(ROOT_DIR / "audio" / "tts_output" / "hindi_sample.wav"), str(test_hindi_path))
            
            with st.spinner("Running Hindi Whisper ASR..."):
                res = voice_pipeline.asr.transcribe(str(test_hindi_path), language="Hindi", mode=mode_str)
                st.success(f"✓ ASR OK ({res['latency']:.2f}s)\n\nText: **{res['text']}**")

    # 2. Test Translation
    with col_b2:
        if st.button("2. Test Translation", use_container_width=True, key="btn_test_trans"):
            with st.spinner("Running IndicTrans2..."):
                res = text_pipeline.run("नमस्ते आप कैसे हैं", source_language="Hindi", target_language="Santali")
                st.success(f"✓ Translation OK ({res['translation_time']:.2f}s)\n\nOutput: **{res['translated_text']}**")

    # 3. Test Hindi TTS
    with col_b3:
        if st.button("3. Test Hindi TTS", use_container_width=True, key="btn_test_hi_tts"):
            with st.spinner("Running Meta MMS-TTS..."):
                res = voice_pipeline.tts.synthesize_hindi("नमस्ते आप कैसे हैं")
                if res.get("success"):
                    st.success(f"✓ Hindi TTS OK ({res['latency']:.2f}s, Dur: {res['duration_sec']:.2f}s)")
                    with open(res["audio_path"], "rb") as fa:
                        st.audio(fa.read(), format="audio/wav")
                else:
                    st.error(f"❌ Error: {res.get('error')}")

    # 4. Test Santali TTS
    with col_b4:
        if st.button("4. Test Santali TTS", use_container_width=True, key="btn_test_sat_tts"):
            with st.spinner(f"Running Indic Parler-TTS ({mode_str} mode)..."):
                res = voice_pipeline.tts.synthesize_santali("ᱟᱢ ᱪᱮᱫ ᱞᱮᱠᱟ ᱢᱮᱱᱟᱜ ᱵᱤᱱᱟ?", mode=mode_str, speaker=speaker_str)
                val = res.get("validation", {})
                if res.get("success") and val.get("valid_audio", False):
                    st.success(f"✓ Santali TTS OK ({res['latency']:.2f}s, Dur: {res['duration_sec']:.2f}s)")
                    with open(res["audio_path"], "rb") as fa:
                        st.audio(fa.read(), format="audio/wav")
                    st.caption(f"RMS: {val['rms']} | Max Amplitude: {val['max_amplitude']} | Sample Rate: {val['sample_rate']} Hz")
                else:
                    st.error(f"Santali TTS FAILED: generated audio is empty or invalid ({val.get('reason', res.get('error'))})")

    st.write("---")
    st.markdown("#### ⚡ Full End-to-End Voice Pipeline Benchmark")
    if st.button("▶ Run Complete Voice Pipeline Benchmark", type="primary", use_container_width=True, key="btn_bench_full"):
        if not test_hindi_path.exists():
            voice_pipeline.tts.synthesize_hindi("मुझे कल बाजार जाना है।", output_filename="hindi_sample.wav")
            import shutil
            shutil.copy(str(ROOT_DIR / "audio" / "tts_output" / "hindi_sample.wav"), str(test_hindi_path))

        with st.spinner(f"Running Full Voice Pipeline ({mode_str} mode)..."):
            res = voice_pipeline.run(
                str(test_hindi_path),
                source_language="Hindi",
                target_language="Santali",
                mode=mode_str,
                speaker=speaker_str
            )

        val = res.get("audio_validation", {})
        is_valid = val.get("valid_audio", False)

        st.markdown("### LIVE BENCHMARK")
        c1, c2, c3, c4 = st.columns(4)
        c1.metric("ASR", f"{res['asr_time']:.2f} s", "✓ PASS" if res['recognized_text'] else "❌ FAIL")
        c2.metric("Translation", f"{res['translation_time']:.2f} s", "✓ PASS" if res['translated_text'] else "❌ FAIL")
        c3.metric("Santali TTS", f"{res['tts_time']:.2f} s", "✓ PASS" if is_valid else "❌ FAIL")
        c4.metric("Total", f"{res['total_time']:.2f} s")

        st.markdown(f"**Recognized (Hindi):** `{res['recognized_text']}`")
        st.markdown(f"**Translated (Santali):** `{res['translated_text']}`")

        if is_valid and res.get("tts_audio_path") and os.path.exists(res["tts_audio_path"]):
            st.markdown("#### Audio Validation")
            st.success(f"✓ WAV valid | Duration: {val['duration']:.2f} sec | Sample rate: {val['sample_rate']} Hz | Audio samples detected (RMS: {val['rms']})")
            with open(res["tts_audio_path"], "rb") as fa:
                st.audio(fa.read(), format="audio/wav")
        else:
            st.error(f"Santali TTS FAILED: generated audio is empty or invalid ({val.get('reason', 'audio check failed')})")
