package com.tezgpt.app.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class SessionStore {
    private static final String PREFS = "tezgpt_secure_session";
    private static final String COOKIE_KEY = "session_cookie";
    private static final String API_BASE_URL_KEY = "api_base_url";
    private static final String KEY_ALIAS = "tezgpt_session_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private final SharedPreferences preferences;

    public SessionStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ensureKey();
    }

    public boolean isAuthenticated() {
        return getCookie() != null;
    }

    public void saveApiBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            preferences.edit().remove(API_BASE_URL_KEY).apply();
            return;
        }
        preferences.edit().putString(API_BASE_URL_KEY, baseUrl.trim()).apply();
    }

    public String getApiBaseUrl() {
        return preferences.getString(API_BASE_URL_KEY, "");
    }

    public void saveCookie(String cookie) {
        if (cookie == null || cookie.trim().isEmpty()) {
            clear();
            return;
        }
        preferences.edit().putString(COOKIE_KEY, encrypt(cookie)).apply();
    }

    public String getCookie() {
        String encrypted = preferences.getString(COOKIE_KEY, null);
        if (encrypted == null) return null;
        try {
            return decrypt(encrypted);
        } catch (Exception ignored) {
            clear();
            return null;
        }
    }

    public void clear() {
        preferences.edit().remove(COOKIE_KEY).apply();
    }

    private void ensureKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
                generator.init(new android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setUserAuthenticationRequired(false)
                        .build());
                generator.generateKey();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize secure TezGPT session storage", e);
        }
    }

    private SecretKey getKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);
        return ((KeyStore.SecretKeyEntry) keyStore.getEntry(KEY_ALIAS, null)).getSecretKey();
    }

    private String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] iv = cipher.getIV();
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.encodeToString(combined, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to encrypt TezGPT session", e);
        }
    }

    private String decrypt(String encoded) throws Exception {
        byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);
        byte[] iv = new byte[12];
        byte[] ciphertext = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }
}
