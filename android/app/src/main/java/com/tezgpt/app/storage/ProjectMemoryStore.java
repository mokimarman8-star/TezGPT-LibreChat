package com.tezgpt.app.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Device-local, encrypted project context. This cache improves continuity when
 * revisiting a coding conversation but never stores server cookies or provider keys.
 */
public final class ProjectMemoryStore {
    private static final String PREFS = "tezgpt_project_memory";
    private static final String TRANSCRIPTS_KEY = "project_transcripts";
    private static final String CODING_MODE_KEY = "coding_mode";
    private static final String KEY_ALIAS = "tezgpt_project_memory_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final int MAX_PROJECTS = 20;
    private static final int MAX_MESSAGES_PER_PROJECT = 80;

    private final SharedPreferences preferences;

    public ProjectMemoryStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureKey();
    }

    public boolean isCodingModeEnabled() {
        return preferences.getBoolean(CODING_MODE_KEY, false);
    }

    public void setCodingModeEnabled(boolean enabled) {
        preferences.edit().putBoolean(CODING_MODE_KEY, enabled).apply();
    }

    public synchronized void recordMessage(String conversationId, String role, String content,
                                           String endpoint, String model, boolean codingMode) {
        if (content == null || content.trim().isEmpty()) return;
        try {
            JSONArray projects = loadProjects();
            String id = conversationId == null || conversationId.trim().isEmpty() ? "new" : conversationId;
            JSONObject project = null;
            for (int i = 0; i < projects.length(); i++) {
                JSONObject candidate = projects.optJSONObject(i);
                if (candidate != null && id.equals(candidate.optString("conversationId"))) {
                    project = candidate;
                    break;
                }
            }
            if (project == null) {
                project = new JSONObject();
                project.put("conversationId", id);
                project.put("messages", new JSONArray());
                projects.put(project);
            }
            JSONArray messages = project.optJSONArray("messages");
            if (messages == null) {
                messages = new JSONArray();
                project.put("messages", messages);
            }
            JSONObject message = new JSONObject();
            message.put("role", role == null ? "assistant" : role);
            message.put("content", content);
            message.put("endpoint", endpoint == null ? "" : endpoint);
            message.put("model", model == null ? "" : model);
            message.put("codingMode", codingMode);
            message.put("savedAt", System.currentTimeMillis());
            messages.put(message);
            while (messages.length() > MAX_MESSAGES_PER_PROJECT) removeAt(messages, 0);
            project.put("updatedAt", System.currentTimeMillis());
            project.put("codingMode", codingMode);
            while (projects.length() > MAX_PROJECTS) removeAt(projects, 0);
            preferences.edit().putString(TRANSCRIPTS_KEY, encrypt(projects.toString())).apply();
        } catch (Exception ignored) {
            // Local continuity is optional; remote chat behavior must continue if disk storage fails.
        }
    }

    public synchronized JSONArray getMessages(String conversationId) {
        try {
            String id = conversationId == null || conversationId.trim().isEmpty() ? "new" : conversationId;
            JSONArray projects = loadProjects();
            for (int i = 0; i < projects.length(); i++) {
                JSONObject project = projects.optJSONObject(i);
                if (project != null && id.equals(project.optString("conversationId"))) {
                    return project.optJSONArray("messages") == null ? new JSONArray() : project.optJSONArray("messages");
                }
            }
        } catch (Exception ignored) { }
        return new JSONArray();
    }

    public synchronized void clearAll() {
        preferences.edit().remove(TRANSCRIPTS_KEY).apply();
    }

    private JSONArray loadProjects() {
        String encrypted = preferences.getString(TRANSCRIPTS_KEY, null);
        if (encrypted == null || encrypted.isEmpty()) return new JSONArray();
        try {
            return new JSONArray(decrypt(encrypted));
        } catch (Exception ignored) {
            preferences.edit().remove(TRANSCRIPTS_KEY).apply();
            return new JSONArray();
        }
    }

    private static void removeAt(JSONArray values, int index) throws Exception {
        JSONArray compact = new JSONArray();
        for (int i = 0; i < values.length(); i++) if (i != index) compact.put(values.get(i));
        while (values.length() > 0) values.remove(values.length() - 1);
        for (int i = 0; i < compact.length(); i++) values.put(compact.get(i));
    }

    private void ensureKey() {
        try {
            KeyStore store = KeyStore.getInstance(ANDROID_KEYSTORE);
            store.load(null);
            if (!store.containsAlias(KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
                generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false)
                        .build());
                generator.generateKey();
            }
        } catch (Exception error) {
            throw new IllegalStateException("Unable to initialize TezGPT project memory", error);
        }
    }

    private SecretKey getKey() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEYSTORE);
        store.load(null);
        return ((KeyStore.SecretKeyEntry) store.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    private String encrypt(String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, getKey());
        byte[] iv = cipher.getIV();
        byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        return Base64.encodeToString(combined, Base64.NO_WRAP);
    }

    private String decrypt(String value) throws Exception {
        byte[] combined = Base64.decode(value, Base64.NO_WRAP);
        byte[] iv = new byte[12];
        byte[] ciphertext = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
}
