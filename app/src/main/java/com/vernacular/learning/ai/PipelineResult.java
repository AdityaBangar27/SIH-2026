package com.vernacular.learning.ai;

import java.io.File;

/**
 * Structured pipeline execution result containing outputs and latency benchmarks.
 */
public class PipelineResult {
    public final String recognizedHindiText;
    public final String translatedSantaliText;
    public final File outputAudioFile;
    public final long asrLatencyMs;
    public final long translationLatencyMs;
    public final long ttsLatencyMs;
    public final long totalLatencyMs;
    public final boolean isSuccess;
    public final String errorMessage;

    public PipelineResult(
            String recognizedHindiText,
            String translatedSantaliText,
            File outputAudioFile,
            long asrLatencyMs,
            long translationLatencyMs,
            long ttsLatencyMs,
            long totalLatencyMs,
            boolean isSuccess,
            String errorMessage) {
        this.recognizedHindiText = recognizedHindiText != null ? recognizedHindiText : "";
        this.translatedSantaliText = translatedSantaliText != null ? translatedSantaliText : "";
        this.outputAudioFile = outputAudioFile;
        this.asrLatencyMs = asrLatencyMs;
        this.translationLatencyMs = translationLatencyMs;
        this.ttsLatencyMs = ttsLatencyMs;
        this.totalLatencyMs = totalLatencyMs;
        this.isSuccess = isSuccess;
        this.errorMessage = errorMessage;
    }

    public static PipelineResult success(
            String recognizedHindiText,
            String translatedSantaliText,
            File outputAudioFile,
            long asrLatencyMs,
            long translationLatencyMs,
            long ttsLatencyMs,
            long totalLatencyMs) {
        return new PipelineResult(
                recognizedHindiText,
                translatedSantaliText,
                outputAudioFile,
                asrLatencyMs,
                translationLatencyMs,
                ttsLatencyMs,
                totalLatencyMs,
                true,
                null);
    }

    public static PipelineResult failure(String errorMessage, long totalLatencyMs) {
        return new PipelineResult(
                "",
                "",
                null,
                0,
                0,
                0,
                totalLatencyMs,
                false,
                errorMessage);
    }

    @Override
    public String toString() {
        return "PipelineResult{" +
                "isSuccess=" + isSuccess +
                ", asrText='" + recognizedHindiText + '\'' +
                ", translationText='" + translatedSantaliText + '\'' +
                ", asrMs=" + asrLatencyMs +
                ", transMs=" + translationLatencyMs +
                ", ttsMs=" + ttsLatencyMs +
                ", totalMs=" + totalLatencyMs +
                (errorMessage != null ? ", error='" + errorMessage + '\'' : "") +
                '}';
    }
}
