package com.tezgpt.app.api;

import android.os.Handler;
import android.os.Looper;

import com.tezgpt.app.BuildConfig;
import com.tezgpt.app.storage.SessionStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApiClient {
    public interface Callback<T> {
        void onSuccess(T value);
        void onError(Exception error);
    }

    public interface StreamCallback {
        void onChunk(String text);
        void onComplete(String fullText);
        void onError(Exception error);
    }

    private final SessionStore sessionStore;
    private final String configuredBaseUrl;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ApiClient(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
        String saved = sessionStore.getApiBaseUrl();
        String buildValue = BuildConfig.TEZGPT_API_BASE_URL == null ? "" : BuildConfig.TEZGPT_API_BASE_URL.trim();
        this.configuredBaseUrl = saved.isEmpty() ? buildValue : saved;
    }

    public boolean isConfigured() {
        String url = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        return !url.isEmpty()
                && !url.contains("your-tezgpt-domain.example")
                && (url.startsWith("https://") || BuildConfig.DEBUG && url.startsWith("http://"));
    }

    public String baseUrl() {
        String url = configuredBaseUrl == null ? "" : configuredBaseUrl.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (!isConfigured()) {
            throw new IllegalStateException("Configure a real HTTPS TezGPT API URL before using the app.");
        }
        return url;
    }

    public String configuredBaseUrl() {
        return configuredBaseUrl == null ? "" : configuredBaseUrl;
    }

    public void login(String email, String password, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            callback.onError(e);
            return;
        }
        executeJson("POST", "/api/auth/login", body, callback);
    }

    public void register(String name, String username, String email, String password, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", name);
            body.put("username", username);
            body.put("email", email);
            body.put("password", password);
        } catch (Exception e) {
            callback.onError(e);
            return;
        }
        executeJson("POST", "/api/auth/register", body, callback);
    }

    public void logout(Callback<JSONObject> callback) {
        executeJson("POST", "/api/auth/logout", new JSONObject(), new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) {
                sessionStore.clear();
                callback.onSuccess(value);
            }
            @Override public void onError(Exception error) {
                sessionStore.clear();
                callback.onError(error);
            }
        });
    }

    public void refresh(Callback<JSONObject> callback) {
        executeJson("POST", "/api/auth/refresh", new JSONObject(), callback);
    }

    public void startupConfig(Callback<JSONObject> callback) {
        executeJson("GET", "/api/config", null, callback);
    }

    /** Real LibreChat endpoint/provider configuration used by the web ModelSelect flow. */
    public void aiEndpoints(Callback<JSONObject> callback) {
        executeJson("GET", "/api/endpoints", null, callback);
    }

    /** Real LibreChat model catalog. Response is normally a map keyed by endpoint name. */
    public void models(Callback<JSONObject> callback) {
        executeJson("GET", "/api/models", null, callback);
    }

    /** Save a user-provided provider credential through the real authenticated key route. */
    public void saveUserKey(String endpoint, String value, Long expiresAt, Callback<JSONObject> callback) {
        JSONObject body = new JSONObject();
        try {
            body.put("name", endpoint == null ? "" : endpoint);
            body.put("value", value == null ? "" : value);
            if (expiresAt == null) body.put("expiresAt", JSONObject.NULL);
            else body.put("expiresAt", expiresAt);
        } catch (Exception error) {
            callback.onError(error);
            return;
        }
        executeJson("PUT", "/api/keys", body, callback);
    }

    /** Return only expiry metadata; the server never returns the stored secret. */
    public void userKeyExpiry(String endpoint, Callback<JSONObject> callback) {
        String encoded = java.net.URLEncoder.encode(endpoint == null ? "" : endpoint, java.nio.charset.StandardCharsets.UTF_8);
        executeJson("GET", "/api/keys?name=" + encoded, null, callback);
    }

    public void revokeUserKey(String endpoint, Callback<JSONObject> callback) {
        String encoded = java.net.URLEncoder.encode(endpoint == null ? "" : endpoint, java.nio.charset.StandardCharsets.UTF_8);
        executeJson("DELETE", "/api/keys/" + encoded, null, callback);
    }

    public void conversations(Callback<JSONArray> callback) {
        executeJson("GET", "/api/convos?limit=50", null, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) {
                try {
                    JSONArray conversations = value.optJSONArray("conversations");
                    callback.onSuccess(conversations == null ? new JSONArray() : conversations);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void search(String query, Callback<JSONObject> callback) {
        String encoded = java.net.URLEncoder.encode(query == null ? "" : query, java.nio.charset.StandardCharsets.UTF_8);
        executeJson("GET", "/api/search?q=" + encoded, null, callback);
    }

    public void memories(Callback<JSONArray> callback) {
        executeJson("GET", "/api/memories", null, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) {
                JSONArray memories = value.optJSONArray("memories");
                callback.onSuccess(memories == null ? new JSONArray() : memories);
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    public void deleteMemory(String key, Callback<JSONObject> callback) {
        executeJson("DELETE", "/api/memories/" + java.net.URLEncoder.encode(key == null ? "" : key, java.nio.charset.StandardCharsets.UTF_8), null, callback);
    }

    public void sendMessage(String text, String conversationId, String parentMessageId,
                            String endpoint, String model, StreamCallback callback) {
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("text", text);
                body.put("conversationId", conversationId == null ? "new" : conversationId);
                body.put("parentMessageId", parentMessageId == null ? "" : parentMessageId);
                body.put("endpoint", endpoint == null ? "openAI" : endpoint);
                body.put("model", model == null ? "" : model);
                body.put("stream", true);

                HttpURLConnection connection = open("POST", "/api/ask");
                writeBody(connection, body.toString());
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("Chat request failed with HTTP " + status + ": " + readError(connection));
                }

                String contentType = connection.getContentType() == null ? "" : connection.getContentType();
                StringBuilder fullText = new StringBuilder();
                if (contentType.contains("text/event-stream")) {
                    readSse(connection.getInputStream(), fullText, callback);
                } else {
                    String response = readAll(connection.getInputStream());
                    String textValue = extractText(response);
                    if (!textValue.isEmpty()) {
                        fullText.append(textValue);
                        post(() -> callback.onChunk(textValue));
                    }
                }
                post(() -> callback.onComplete(fullText.toString()));
                connection.disconnect();
            } catch (Exception e) {
                post(() -> callback.onError(e));
            }
        });
    }

    public void uploadFile(byte[] bytes, String filename, String mimeType, Callback<JSONObject> callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            String boundary = "----TezGPTBoundary" + java.util.UUID.randomUUID();
            try {
                connection = open("POST", "/api/files");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                String safeName = filename == null || filename.trim().isEmpty() ? "attachment" : filename.replaceAll("[^A-Za-z0-9._-]", "_");
                String safeMime = mimeType == null || mimeType.trim().isEmpty() ? "application/octet-stream" : mimeType;
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(("--" + boundary + "\\r\\n").getBytes(StandardCharsets.UTF_8));
                    output.write(("Content-Disposition: form-data; name=\\\"file\\\"; filename=\\\"" + safeName + "\\\"\\r\\n").getBytes(StandardCharsets.UTF_8));
                    output.write(("Content-Type: " + safeMime + "\\r\\n\\r\\n").getBytes(StandardCharsets.UTF_8));
                    output.write(bytes == null ? new byte[0] : bytes);
                    output.write(("\\r\\n--" + boundary + "--\\r\\n").getBytes(StandardCharsets.UTF_8));
                }
                int status = connection.getResponseCode();
                String response = readAll(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
                if (status < 200 || status >= 300) throw new IOException("File upload failed with HTTP " + status + ": " + response);
                captureCookies(connection);
                JSONObject result = response.isEmpty() ? new JSONObject() : new JSONObject(response);
                post(() -> callback.onSuccess(result));
            } catch (Exception error) {
                post(() -> callback.onError(error));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    public void startAgent(String prompt, String conversationId, String agentId, StreamCallback callback) {
        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                JSONObject body = new JSONObject();
                body.put("text", prompt);
                body.put("conversationId", conversationId == null ? "new" : conversationId);
                body.put("agent_id", agentId == null ? "" : agentId);
                body.put("stream", true);
                connection = open("POST", "/api/agents/chat");
                writeBody(connection, body.toString());
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("Agent request failed with HTTP " + status + ": " + readError(connection));
                }
                StringBuilder fullText = new StringBuilder();
                readSse(connection.getInputStream(), fullText, callback);
                post(() -> callback.onComplete(fullText.toString()));
            } catch (Exception e) {
                post(() -> callback.onError(e));
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    public void agentStatus(String conversationId, Callback<JSONObject> callback) {
        executeJson("GET", "/api/agents/chat/status/" + (conversationId == null ? "" : conversationId), null, callback);
    }

    public void availableTools(Callback<JSONArray> callback) {
        executeJson("GET", "/api/agents/tools", null, new Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) {
                JSONArray tools = value.optJSONArray("tools");
                callback.onSuccess(tools == null ? new JSONArray() : tools);
            }
            @Override public void onError(Exception error) { callback.onError(error); }
        });
    }

    private void readSse(InputStream input, StringBuilder fullText, StreamCallback callback) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.startsWith("data:")) continue;
            String data = line.substring(5).trim();
            if (data.isEmpty() || "[DONE]".equals(data)) continue;
            String chunk = extractText(data);
            if (!chunk.isEmpty()) {
                fullText.append(chunk);
                post(() -> callback.onChunk(chunk));
            }
        }
    }

    private String extractText(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return "";
        try {
            JSONObject json = new JSONObject(trimmed);
            String[] keys = {"text", "content", "response", "message"};
            for (String key : keys) {
                Object value = json.opt(key);
                if (value instanceof String) return (String) value;
                if (value instanceof JSONObject) {
                    String nested = ((JSONObject) value).optString("content", "");
                    if (!nested.isEmpty()) return nested;
                }
            }
            JSONArray choices = json.optJSONArray("choices");
            if (choices != null && choices.length() > 0) {
                JSONObject choice = choices.optJSONObject(0);
                JSONObject delta = choice == null ? null : choice.optJSONObject("delta");
                if (delta != null) return delta.optString("content", "");
                JSONObject message = choice == null ? null : choice.optJSONObject("message");
                if (message != null) return message.optString("content", "");
            }
        } catch (Exception ignored) {
            return trimmed;
        }
        return "";
    }

    private void executeJson(String method, String path, JSONObject body, Callback<JSONObject> callback) {
        executor.execute(() -> {
            try {
                HttpURLConnection connection = open(method, path);
                if (body != null) writeBody(connection, body.toString());
                int status = connection.getResponseCode();
                String response = readAll(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
                if (status < 200 || status >= 300) {
                    throw new IOException("Request failed with HTTP " + status + ": " + response);
                }
                captureCookies(connection);
                JSONObject result = response.isEmpty() ? new JSONObject() : new JSONObject(response);
                post(() -> callback.onSuccess(result));
                connection.disconnect();
            } catch (Exception e) {
                post(() -> callback.onError(e));
            }
        });
    }

    private HttpURLConnection open(String method, String path) throws IOException {
        URL url = new URL(baseUrl() + (path.startsWith("/") ? path : "/" + path));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(20_000);
        connection.setReadTimeout(120_000);
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setRequestProperty("Accept", "application/json, text/event-stream");
        String cookie = sessionStore.getCookie();
        if (cookie != null && !cookie.isEmpty()) connection.setRequestProperty("Cookie", cookie);
        if ("POST".equals(method) || "PUT".equals(method)) {
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        }
        return connection;
    }

    private void writeBody(HttpURLConnection connection, String body) throws IOException {
        try (OutputStream output = connection.getOutputStream()) {
            output.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void captureCookies(HttpURLConnection connection) {
        Map<String, List<String>> headers = connection.getHeaderFields();
        if (headers == null) return;
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies == null) return;
        List<String> values = new ArrayList<>();
        for (String cookie : setCookies) {
            if (cookie == null) continue;
            int separator = cookie.indexOf(';');
            values.add(separator > 0 ? cookie.substring(0, separator) : cookie);
        }
        if (!values.isEmpty()) sessionStore.saveCookie(String.join("; ", values));
    }

    private String readError(HttpURLConnection connection) {
        try { return readAll(connection.getErrorStream()); }
        catch (Exception ignored) { return "Unknown server error"; }
    }

    private String readAll(InputStream input) throws IOException {
        if (input == null) return "";
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line).append('\n');
        }
        return value.toString().trim();
    }

    private void post(Runnable action) { mainHandler.post(action); }
}
