package com.vernacular.learning.ai;

import android.content.Context;
import android.util.Log;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages ONNX Runtime environment, model asset extraction, and session creation.
 * Configured for low-resource devices (2 intra-op threads, CPU execution provider).
 *
 * Implements strict lifecycle states: NOT_INITIALIZED, LOADING, READY, ERROR.
 */
public class ModelManager {
    private static final String TAG = "ModelManager";
    private static ModelManager instance;
    private OrtEnvironment env;

    public enum State {
        NOT_INITIALIZED,
        LOADING,
        READY,
        ERROR
    }

    private State currentState = State.NOT_INITIALIZED;
    private String lastErrorMessage = null;
    private final Map<String, OrtSession> activeSessions = new HashMap<>();

    private ModelManager() {
        try {
            env = OrtEnvironment.getEnvironment();
            currentState = State.NOT_INITIALIZED;
        } catch (Exception e) {
            currentState = State.ERROR;
            lastErrorMessage = e.getMessage();
            Log.e(TAG, "Failed to initialize OrtEnvironment", e);
        }
    }

    public static synchronized ModelManager getInstance() {
        if (instance == null) {
            instance = new ModelManager();
        }
        return instance;
    }

    public State getState() {
        return currentState;
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public OrtEnvironment getEnvironment() {
        if (env == null) {
            try {
                env = OrtEnvironment.getEnvironment();
            } catch (Exception e) {
                currentState = State.ERROR;
                lastErrorMessage = e.getMessage();
                Log.e(TAG, "Failed to re-initialize OrtEnvironment", e);
            }
        }
        return env;
    }

    /**
     * Extracts model asset (and associated directory assets such as external .data files)
     * to internal storage and creates an OrtSession.
     * Prevents duplicate simultaneous session creation.
     */
    public synchronized OrtSession createSession(Context context, String assetPath) throws OrtException {
        if (activeSessions.containsKey(assetPath)) {
            OrtSession existing = activeSessions.get(assetPath);
            if (existing != null) {
                return existing;
            }
        }

        File modelFile = extractAssetIfNeeded(context, assetPath);
        if (modelFile == null || !modelFile.exists()) {
            currentState = State.ERROR;
            lastErrorMessage = "Failed to extract model asset: " + assetPath;
            throw new OrtException(lastErrorMessage);
        }

        // Also extract sibling files in the same asset directory (e.g., .data external weight files)
        int lastSlash = assetPath.lastIndexOf('/');
        if (lastSlash > 0) {
            String dirPath = assetPath.substring(0, lastSlash);
            extractAssetDirectory(context, dirPath);
        }

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        options.setIntraOpNumThreads(2);
        options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT);

        try {
            OrtSession session = getEnvironment().createSession(modelFile.getAbsolutePath(), options);
            activeSessions.put(assetPath, session);
            return session;
        } catch (OrtException e) {
            currentState = State.ERROR;
            lastErrorMessage = e.getMessage();
            throw e;
        }
    }

    /**
     * Extracts an asset file to app private storage if not already present or if incomplete.
     */
    public synchronized File extractAssetIfNeeded(Context context, String assetPath) {
        File targetFile = new File(context.getFilesDir(), "extracted_assets/" + assetPath);
        if (targetFile.exists() && targetFile.length() > 0) {
            return targetFile;
        }

        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        InputStream is = null;
        try {
            try {
                is = context.getAssets().open(assetPath);
            } catch (Exception e) {
                // Fallback for Robolectric/JVM unit testing
                File localAsset = new File("src/main/assets/" + assetPath);
                if (!localAsset.exists()) {
                    localAsset = new File("app/src/main/assets/" + assetPath);
                }
                if (localAsset.exists()) {
                    is = new java.io.FileInputStream(localAsset);
                } else {
                    throw e;
                }
            }

            try (OutputStream os = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[65536];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
                Log.i(TAG, "Extracted asset: " + assetPath + " (" + targetFile.length() + " bytes)");
                return targetFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting asset: " + assetPath, e);
            return null;
        } finally {
            if (is != null) {
                try { is.close(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Extracts all files in an asset directory (such as external .data weight files).
     */
    public synchronized void extractAssetDirectory(Context context, String dirPath) {
        try {
            String[] files = context.getAssets().list(dirPath);
            if (files != null) {
                for (String file : files) {
                    String subPath = dirPath + "/" + file;
                    extractAssetIfNeeded(context, subPath);
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to list directory: " + dirPath, e);
        }
    }

    public synchronized void closeSession(String assetPath) {
        OrtSession s = activeSessions.remove(assetPath);
        if (s != null) {
            try {
                s.close();
            } catch (Exception ignored) {}
        }
    }

    public synchronized void close() {
        for (OrtSession s : activeSessions.values()) {
            if (s != null) {
                try {
                    s.close();
                } catch (Exception ignored) {}
            }
        }
        activeSessions.clear();

        if (env != null) {
            try {
                env.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing OrtEnvironment", e);
            }
            env = null;
        }
        currentState = State.NOT_INITIALIZED;
    }
}
