package com.tezgpt.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.tezgpt.app.api.ApiClient;
import com.tezgpt.app.storage.ProjectMemoryStore;
import com.tezgpt.app.storage.SessionStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final int FILE_PICKER_REQUEST = 9101;
    private SessionStore sessionStore;
    private ProjectMemoryStore projectMemoryStore;
    private ApiClient apiClient;
    private LinearLayout messageContainer;
    private ScrollView messageScroll;
    private TextView emptyChatText;
    private TextView chatError;
    private EditText messageInput;
    private Button sendButton;
    private ProgressBar chatProgress;
    private Spinner endpointSpinner;
    private Spinner modelSpinner;
    private Button codingModeButton;
    private JSONObject serverModelCatalog;
    private boolean codingMode;
    private String conversationId = "new";
    private String parentMessageId = "";
    private Uri selectedFileUri;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_PICKER_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selected = data.getData();
            selectedFileUri = selected;
            try {
                getContentResolver().takePersistableUriPermission(selected, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) { }
            showFiles(findViewById(R.id.app_drawer), selected.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionStore = new SessionStore(this);
        projectMemoryStore = new ProjectMemoryStore(this);
        apiClient = new ApiClient(sessionStore);

        if (!apiClient.isConfigured()) {
            showConfigurationError();
        } else if (sessionStore.isAuthenticated()) {
            showChat();
        } else {
            showLogin();
        }
    }

    private void showConfigurationError() {
        LinearLayout root = verticalRoot();
        TextView title = text("TezGPT", 32, true);
        title.setTextColor(getColor(com.tezgpt.app.R.color.tezgpt_green));
        root.addView(title, marginParams(0, 0, 0, 18));

        TextView message = text("Enter the HTTPS address of your own TezGPT server.", 16, false);
        message.setGravity(Gravity.CENTER);
        root.addView(message, marginParams(0, 0, 0, 16));

        EditText serverUrl = new EditText(this);
        serverUrl.setSingleLine(true);
        serverUrl.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        serverUrl.setHint("https://your-tezgpt-server.example");
        serverUrl.setText(apiClient.configuredBaseUrl());
        serverUrl.setSelectAllOnFocus(true);
        root.addView(serverUrl, marginParams(0, 0, 0, 10));

        TextView error = text("", 13, false);
        error.setTextColor(getColor(R.color.tezgpt_error));
        error.setVisibility(View.GONE);
        root.addView(error, marginParams(0, 0, 0, 10));

        Button save = actionButton("Save server and continue", true);
        save.setOnClickListener(v -> {
            String normalized = normalizeServerUrl(serverUrl.getText().toString());
            if (normalized == null) {
                showError(error, "Enter a valid HTTPS URL, for example https://ai.example.com");
                return;
            }
            sessionStore.saveApiBaseUrl(normalized);
            apiClient = new ApiClient(sessionStore);
            if (sessionStore.isAuthenticated()) showChat(); else showLogin();
        });
        root.addView(save, matchParams());

        TextView details = text("The address is stored on this device. No browser or external sign-in page is opened.", 13, false);
        details.setGravity(Gravity.CENTER);
        root.addView(details, marginParams(0, 18, 0, 0));
        setContentView(root);
    }

    private void showLogin() {
        setContentView(R.layout.screen_login);
        EditText email = findViewById(R.id.email_input);
        EditText password = findViewById(R.id.password_input);
        TextView error = findViewById(R.id.login_error);
        Button signIn = findViewById(R.id.sign_in_button);
        Button createAccount = findViewById(R.id.create_account_button);
        ProgressBar progress = findViewById(R.id.login_progress);

        createAccount.setOnClickListener(v -> showRegister());
        applyLoginDefaults(createAccount, findViewById(R.id.social_login_container), findViewById(R.id.login_or_divider));
        apiClient.startupConfig(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { applyLoginConfig(value, createAccount); }
            @Override public void onError(Exception ignored) { /* Keep the real email form available on transient config failure. */ }
        });
        signIn.setOnClickListener(v -> {
            String emailValue = email.getText().toString().trim();
            String passwordValue = password.getText().toString();
            String validationError = validateCredentials(emailValue, passwordValue);
            if (validationError != null) {
                showError(error, validationError);
                return;
            }
            progress.setVisibility(View.VISIBLE);
            signIn.setEnabled(false);
            apiClient.login(emailValue, passwordValue, new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject value) {
                    progress.setVisibility(View.GONE);
                    signIn.setEnabled(true);
                    showChat();
                }
                @Override public void onError(Exception e) {
                    progress.setVisibility(View.GONE);
                    signIn.setEnabled(true);
                    showError(error, friendlyError(e));
                }
            });
        });
    }

    private void applyLoginDefaults(Button createAccount, LinearLayout socialContainer, TextView divider) {
        createAccount.setVisibility(View.VISIBLE);
        socialContainer.setVisibility(View.GONE);
        divider.setVisibility(View.GONE);
    }

    /** Native TezGPT uses local email/password account access only; no provider browser flow is shown. */
    private void applyLoginConfig(JSONObject config, Button createAccount) {
        boolean emailLoginEnabled = config.optBoolean("emailLoginEnabled", true);
        boolean registrationEnabled = config.optBoolean("registrationEnabled", true);
        createAccount.setVisibility(registrationEnabled ? View.VISIBLE : View.GONE);
        if (!emailLoginEnabled) {
            findViewById(R.id.email_input).setVisibility(View.GONE);
            findViewById(R.id.password_input).setVisibility(View.GONE);
            findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        }
    }

    private void showRegister() {
        setContentView(R.layout.screen_register);
        EditText name = findViewById(R.id.register_name);
        EditText username = findViewById(R.id.register_username);
        EditText email = findViewById(R.id.register_email);
        EditText password = findViewById(R.id.register_password);
        TextView error = findViewById(R.id.register_error);
        Button register = findViewById(R.id.register_button);
        Button back = findViewById(R.id.back_to_login_button);

        back.setOnClickListener(v -> showLogin());
        register.setOnClickListener(v -> {
            String emailValue = email.getText().toString().trim();
            String passwordValue = password.getText().toString();
            String nameValue = name.getText().toString().trim();
            String usernameValue = username.getText().toString().trim();
            String validationError = validateRegistration(nameValue, usernameValue, emailValue, passwordValue);
            if (validationError != null) {
                showError(error, validationError);
                return;
            }
            register.setEnabled(false);
            apiClient.register(nameValue, usernameValue, emailValue, passwordValue,
                    new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject value) {
                    register.setEnabled(true);
                    showLogin();
                }
                @Override public void onError(Exception e) {
                    register.setEnabled(true);
                    showError(error, friendlyError(e));
                }
            });
        });
    }

    private void showChat() {
        setContentView(R.layout.screen_shell);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        getLayoutInflater().inflate(R.layout.screen_chat, shellContent, true);
        TextView shellTitle = findViewById(R.id.shell_title);
        shellTitle.setText(getString(R.string.app_name));
        DrawerLayout drawer = findViewById(R.id.app_drawer);
        findViewById(R.id.open_drawer_button).setOnClickListener(v -> drawer.openDrawer(Gravity.LEFT));
        findViewById(R.id.shell_new_chat_button).setOnClickListener(v -> clearConversation());
        configureDrawer(drawer);

        messageContainer = findViewById(R.id.message_container);
        messageScroll = findViewById(R.id.message_scroll);
        emptyChatText = findViewById(R.id.empty_chat_text);
        chatError = findViewById(R.id.chat_error);
        messageInput = findViewById(R.id.message_input);
        sendButton = findViewById(R.id.send_button);
        chatProgress = findViewById(R.id.chat_progress);
        endpointSpinner = findViewById(R.id.endpoint_spinner);
        modelSpinner = findViewById(R.id.model_spinner);
        codingModeButton = findViewById(R.id.coding_mode_button);
        codingMode = projectMemoryStore.isCodingModeEnabled();
        updateCodingModeButton();
        codingModeButton.setOnClickListener(v -> {
            codingMode = !codingMode;
            projectMemoryStore.setCodingModeEnabled(codingMode);
            updateCodingModeButton();
            updateModelsForEndpoint(String.valueOf(endpointSpinner.getSelectedItem()));
        });
        endpointSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                updateModelsForEndpoint(String.valueOf(parent.getItemAtPosition(position)));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        configureDefaultSpinners();
        // LibreChat separates pre/login startup flags, endpoint configuration, and model catalog.
        // Load all three native, exactly as the web client does, instead of inventing model names.
        apiClient.startupConfig(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { applyServerConfig(value); }
            @Override public void onError(Exception error) { /* Keep safe fallback selectors. */ }
        });
        apiClient.aiEndpoints(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { applyEndpointConfig(value); }
            @Override public void onError(Exception error) { /* Keep fallback endpoint only if unavailable. */ }
        });
        apiClient.models(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { applyModelCatalog(value); }
            @Override public void onError(Exception error) { /* Keep fallback model only if unavailable. */ }
        });

        findViewById(R.id.new_chat_button).setOnClickListener(v -> clearConversation());
        findViewById(R.id.logout_button).setOnClickListener(v -> {
            apiClient.logout(new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject value) { showLogin(); }
                @Override public void onError(Exception error) { showLogin(); }
            });
        });
        sendButton.setOnClickListener(v -> sendCurrentMessage());
        emptyChatText.setVisibility(View.VISIBLE);
    }

    private void configureDefaultSpinners() {
        endpointSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(getString(R.string.endpoint_default))));
        modelSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(getString(R.string.model_default))));
    }

    /** Apply only startup flags; model/provider data comes from the dedicated real routes. */
    private void applyServerConfig(JSONObject config) {
        if (config == null) return;
        String title = config.optString("appTitle", "");
        if (!title.isEmpty()) {
            TextView shellTitle = findViewById(R.id.shell_title);
            if (shellTitle != null && getString(R.string.app_name).equals(shellTitle.getText().toString())) {
                shellTitle.setText(title);
            }
        }
    }

    /** `/api/endpoints` returns a map keyed by endpoint/provider name. */
    private void applyEndpointConfig(JSONObject endpointConfig) {
        if (endpointConfig == null || endpointSpinner == null) return;
        java.util.ArrayList<String> endpoints = new java.util.ArrayList<>();
        java.util.Iterator<String> keys = endpointConfig.keys();
        while (keys.hasNext()) {
            String endpoint = keys.next();
            if (!endpoint.isEmpty() && !endpoints.contains(endpoint)) endpoints.add(endpoint);
        }
        if (endpoints.isEmpty()) endpoints.add(getString(R.string.endpoint_default));
        endpointSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, endpoints));
    }

    /** `/api/models` returns `{ endpointName: [modelName, ...] }`; preserve all endpoint models. */
    private void applyModelCatalog(JSONObject modelCatalog) {
        if (modelCatalog == null || modelSpinner == null) return;
        serverModelCatalog = modelCatalog;
        updateModelsForEndpoint(endpointSpinner == null ? "" : String.valueOf(endpointSpinner.getSelectedItem()));
    }

    private void updateModelsForEndpoint(String endpoint) {
        if (modelSpinner == null) return;
        java.util.LinkedHashSet<String> models = new java.util.LinkedHashSet<>();
        JSONArray endpointModels = serverModelCatalog == null ? null : serverModelCatalog.optJSONArray(endpoint);
        if (endpointModels != null) {
            for (int i = 0; i < endpointModels.length(); i++) {
                Object item = endpointModels.opt(i);
                String model = item instanceof JSONObject
                        ? ((JSONObject) item).optString("model", ((JSONObject) item).optString("name", ""))
                        : String.valueOf(item == null ? "" : item);
                if (!model.trim().isEmpty()) models.add(model.trim());
            }
        }
        if (models.isEmpty() && serverModelCatalog != null) {
            java.util.Iterator<String> keys = serverModelCatalog.keys();
            while (keys.hasNext()) {
                JSONArray values = serverModelCatalog.optJSONArray(keys.next());
                if (values == null) continue;
                for (int i = 0; i < values.length(); i++) {
                    Object item = values.opt(i);
                    String model = item instanceof JSONObject
                            ? ((JSONObject) item).optString("model", ((JSONObject) item).optString("name", ""))
                            : String.valueOf(item == null ? "" : item);
                    if (!model.trim().isEmpty()) models.add(model.trim());
                }
            }
        }
        if (codingMode && !models.isEmpty()) {
            java.util.LinkedHashSet<String> codingModels = new java.util.LinkedHashSet<>();
            for (String model : models) if (isCodingCapableModel(model)) codingModels.add(model);
            if (!codingModels.isEmpty()) models = codingModels;
        }
        if (models.isEmpty()) models.add(getString(R.string.model_default));
        modelSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new java.util.ArrayList<>(models)));
    }

    private boolean isCodingCapableModel(String model) {
        String value = model == null ? "" : model.toLowerCase(java.util.Locale.ROOT);
        return value.contains("code") || value.contains("codex") || value.contains("coder")
                || value.contains("gpt") || value.contains("claude") || value.contains("gemini")
                || value.contains("qwen") || value.contains("deepseek") || value.contains("mistral")
                || value.contains("llama") || value.contains("command") || value.matches(".*\\bo[0-9].*");
    }

    private void updateCodingModeButton() {
        if (codingModeButton == null) return;
        codingModeButton.setText(codingMode ? "Coding mode: on" : "Coding mode: off");
        codingModeButton.setContentDescription(codingMode
                ? "Coding Mode is on. Showing coding-capable models from your server catalog."
                : "Coding Mode is off.");
    }

    /* Legacy fallback parser retained only as a defensive reference for unusual servers. */
    private void applyFlattenedModelCatalog(JSONObject modelCatalog) {
        if (modelCatalog == null || modelSpinner == null) return;
        java.util.LinkedHashSet<String> models = new java.util.LinkedHashSet<>();
        java.util.Iterator<String> endpointKeys = modelCatalog.keys();
        while (endpointKeys.hasNext()) {
            Object raw = modelCatalog.opt(endpointKeys.next());
            if (raw instanceof JSONArray) {
                JSONArray values = (JSONArray) raw;
                for (int i = 0; i < values.length(); i++) {
                    Object item = values.opt(i);
                    String model = item instanceof JSONObject
                            ? ((JSONObject) item).optString("model", ((JSONObject) item).optString("name", ""))
                            : String.valueOf(item == null ? "" : item);
                    if (!model.trim().isEmpty()) models.add(model.trim());
                }
            }
        }
        if (models.isEmpty()) models.add(getString(R.string.model_default));
        java.util.ArrayList<String> options = new java.util.ArrayList<>(models);
        modelSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, options));
    }

    private void configureDrawer(DrawerLayout drawer) {
        findViewById(R.id.nav_chat).setOnClickListener(v -> {
            drawer.closeDrawer(Gravity.LEFT);
            showChat();
        });
        findViewById(R.id.nav_conversations).setOnClickListener(v -> showConversations(drawer));
        findViewById(R.id.nav_search).setOnClickListener(v -> showSearch(drawer));
        findViewById(R.id.nav_agents).setOnClickListener(v -> showAgents(drawer));
        findViewById(R.id.nav_files).setOnClickListener(v -> showFiles(drawer, null));
        findViewById(R.id.nav_memory).setOnClickListener(v -> showMemory(drawer));
        findViewById(R.id.nav_settings).setOnClickListener(v -> showSettings(drawer));
        findViewById(R.id.nav_logout).setOnClickListener(v -> {
            drawer.closeDrawer(Gravity.LEFT);
            apiClient.logout(new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject value) { showLogin(); }
                @Override public void onError(Exception error) { showLogin(); }
            });
        });
    }

    private void showConversations(DrawerLayout drawer) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.conversations), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 12));
        ProgressBar progress = new ProgressBar(this);
        page.addView(progress, marginParams(0, 0, 0, 12));
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.conversations));
        apiClient.conversations(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray values) {
                page.removeView(progress);
                if (values.length() == 0) {
                    TextView empty = text("No conversations found.", 16, false);
                    empty.setTextColor(getColor(R.color.tezgpt_text_muted));
                    page.addView(empty, marginParams(0, 8, 0, 0));
                    return;
                }
                for (int i = 0; i < values.length(); i++) {
                    JSONObject item = values.optJSONObject(i);
                    if (item == null) continue;
                    String id = item.optString("conversationId", item.optString("conversation_id", ""));
                    String title = item.optString("title", item.optString("text", "Untitled conversation"));
                    Button row = actionButton(title, false);
                    row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    row.setOnClickListener(v -> {
                        conversationId = id.isEmpty() ? "new" : id;
                        parentMessageId = "";
                        showChat();
                    });
                    page.addView(row, marginParams(0, 0, 0, 8));
                }
            }
            @Override public void onError(Exception error) {
                page.removeView(progress);
                showError(chatError == null ? heading : chatError, friendlyError(error));
                TextView failure = text(friendlyError(error), 14, false);
                failure.setTextColor(getColor(R.color.tezgpt_error));
                page.addView(failure, marginParams(0, 8, 0, 0));
            }
        });
    }

    private void showSearch(DrawerLayout drawer) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.search), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 16));
        EditText query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Search conversations and messages");
        page.addView(query, marginParams(0, 0, 0, 8));
        Button submit = actionButton(getString(R.string.search), true);
        page.addView(submit, matchParams());
        LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        page.addView(results, marginParams(0, 16, 0, 0));
        submit.setOnClickListener(v -> {
            String value = query.getText().toString().trim();
            if (value.isEmpty()) return;
            submit.setEnabled(false);
            results.removeAllViews();
            results.addView(text("Searching…", 14, false), marginParams(0, 0, 0, 8));
            apiClient.search(value, new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject response) {
                    submit.setEnabled(true);
                    results.removeAllViews();
                    JSONArray items = response.optJSONArray("results");
                    if (items == null) items = response.optJSONArray("messages");
                    if (items == null) items = response.optJSONArray("conversations");
                    if (items == null || items.length() == 0) {
                        results.addView(text("No results found.", 16, false), marginParams(0, 0, 0, 0));
                        return;
                    }
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        String title = item.optString("title", item.optString("text", item.optString("content", "Search result")));
                        Button result = actionButton(title, false);
                        result.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                        results.addView(result, marginParams(0, 0, 0, 8));
                    }
                }
                @Override public void onError(Exception error) {
                    submit.setEnabled(true);
                    results.removeAllViews();
                    TextView failure = text(friendlyError(error), 14, false);
                    failure.setTextColor(getColor(R.color.tezgpt_error));
                    results.addView(failure, marginParams(0, 0, 0, 0));
                }
            });
        });
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.search));
    }

    private void showSettings(DrawerLayout drawer) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.settings), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 16));

        TextView serverLabel = text("TezGPT server URL", 14, true);
        page.addView(serverLabel, marginParams(0, 0, 0, 6));
        EditText server = new EditText(this);
        server.setSingleLine(true);
        server.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        server.setText(apiClient.configuredBaseUrl());
        page.addView(server, marginParams(0, 0, 0, 8));
        Button saveServer = actionButton("Save server URL", true);
        saveServer.setOnClickListener(v -> {
            String normalized = normalizeServerUrl(server.getText().toString());
            if (normalized == null) {
                showError(serverLabel, "Enter a valid HTTPS server URL.");
                return;
            }
            sessionStore.saveApiBaseUrl(normalized);
            apiClient = new ApiClient(sessionStore);
            saveServer.setText("Saved");
        });
        page.addView(saveServer, matchParams());

        TextView providerHeading = text("Provider API keys", 16, true);
        page.addView(providerHeading, marginParams(0, 22, 0, 6));
        TextView providerHelp = text("Enter a key for an endpoint configured for user-provided credentials. The key is sent only to your TezGPT server.", 13, false);
        providerHelp.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(providerHelp, marginParams(0, 0, 0, 8));
        Spinner keyEndpointSpinner = new Spinner(this);
        keyEndpointSpinner.setBackgroundResource(R.drawable.bg_secondary_button);
        page.addView(keyEndpointSpinner, marginParams(0, 0, 0, 8));
        EditText providerKey = new EditText(this);
        providerKey.setSingleLine(true);
        providerKey.setHint("Provider API key");
        providerKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        providerKey.setBackgroundResource(R.drawable.bg_input);
        page.addView(providerKey, marginParams(0, 0, 0, 8));
        TextView keyStatus = text("No key status loaded.", 13, false);
        keyStatus.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(keyStatus, marginParams(0, 0, 0, 8));
        LinearLayout keyActions = new LinearLayout(this);
        keyActions.setOrientation(LinearLayout.HORIZONTAL);
        Button saveKey = actionButton("Save key", true);
        Button revokeKey = actionButton("Revoke", false);
        keyActions.addView(saveKey, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams revokeParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        revokeParams.setMargins(8, 0, 0, 0);
        keyActions.addView(revokeKey, revokeParams);
        page.addView(keyActions, matchParams());

        apiClient.aiEndpoints(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject endpointConfig) {
                java.util.ArrayList<String> names = new java.util.ArrayList<>();
                java.util.Iterator<String> keys = endpointConfig.keys();
                while (keys.hasNext()) names.add(keys.next());
                if (names.isEmpty()) names.add(getString(R.string.endpoint_default));
                keyEndpointSpinner.setAdapter(new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, names));
                android.widget.AdapterView.OnItemSelectedListener listener = new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                        String endpoint = String.valueOf(parent.getItemAtPosition(position));
                        apiClient.userKeyExpiry(endpoint, new ApiClient.Callback<JSONObject>() {
                            @Override public void onSuccess(JSONObject value) {
                                String expiry = value.optString("expiresAt", "");
                                keyStatus.setText(expiry.isEmpty() ? "No saved key or key never expires." : "Key status: expires " + expiry);
                            }
                            @Override public void onError(Exception error) { keyStatus.setText("Key status unavailable: " + friendlyError(error)); }
                        });
                    }
                    @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                };
                keyEndpointSpinner.setOnItemSelectedListener(listener);
            }
            @Override public void onError(Exception error) { keyStatus.setText("Provider list unavailable: " + friendlyError(error)); }
        });
        saveKey.setOnClickListener(v -> {
            String endpoint = String.valueOf(keyEndpointSpinner.getSelectedItem());
            String value = providerKey.getText().toString().trim();
            if (endpoint.isEmpty() || value.isEmpty()) {
                keyStatus.setText("Choose an endpoint and enter a key.");
                return;
            }
            saveKey.setEnabled(false);
            apiClient.saveUserKey(endpoint, value, System.currentTimeMillis() + (12L * 60L * 60L * 1000L), new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject result) { saveKey.setEnabled(true); providerKey.setText(""); keyStatus.setText("Saved securely for 12 hours."); }
                @Override public void onError(Exception error) { saveKey.setEnabled(true); keyStatus.setText(friendlyError(error)); }
            });
        });
        revokeKey.setOnClickListener(v -> {
            String endpoint = String.valueOf(keyEndpointSpinner.getSelectedItem());
            if (endpoint.isEmpty()) return;
            revokeKey.setEnabled(false);
            apiClient.revokeUserKey(endpoint, new ApiClient.Callback<JSONObject>() {
                @Override public void onSuccess(JSONObject result) { revokeKey.setEnabled(true); keyStatus.setText("Provider key revoked."); }
                @Override public void onError(Exception error) { revokeKey.setEnabled(true); keyStatus.setText(friendlyError(error)); }
            });
        });

        android.widget.Switch theme = new android.widget.Switch(this);
        theme.setText(getString(R.string.theme));
        theme.setTextColor(getColor(R.color.tezgpt_text));
        theme.setChecked(getPreferences(MODE_PRIVATE).getBoolean("dark_theme", false));
        theme.setOnCheckedChangeListener((button, checked) -> getPreferences(MODE_PRIVATE).edit().putBoolean("dark_theme", checked).apply());
        page.addView(theme, marginParams(0, 18, 0, 12));

        TextView policy = text("Account, privacy, provider, endpoint, preset, and agent controls remain server-backed and will be added as native screens without a web runtime.", 14, false);
        policy.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(policy, marginParams(0, 0, 0, 18));
        Button logout = actionButton(getString(R.string.logout), false);
        logout.setOnClickListener(v -> apiClient.logout(new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject value) { showLogin(); }
            @Override public void onError(Exception error) { showLogin(); }
        }));
        page.addView(logout, matchParams());
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.settings));
    }

    private void showMemory(DrawerLayout drawer) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.memory), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 12));
        TextView description = text("Review and remove server-backed memories. Memory content is never silently hidden from the user.", 16, false);
        description.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(description, marginParams(0, 0, 0, 18));
        ProgressBar progress = new ProgressBar(this);
        page.addView(progress, marginParams(0, 0, 0, 12));
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.memory));
        apiClient.memories(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray memories) {
                page.removeView(progress);
                if (memories.length() == 0) {
                    page.addView(text("No saved memories found.", 16, false), marginParams(0, 8, 0, 0));
                    return;
                }
                for (int i = 0; i < memories.length(); i++) {
                    JSONObject memory = memories.optJSONObject(i);
                    if (memory == null) continue;
                    String key = memory.optString("key", "memory-" + i);
                    String value = memory.optString("value", "");
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(14, 10, 14, 10);
                    row.setBackgroundResource(R.drawable.bg_secondary_button);
                    TextView keyView = text(key, 16, true);
                    TextView valueView = text(value, 14, false);
                    valueView.setTextColor(getColor(R.color.tezgpt_text_muted));
                    row.addView(keyView, matchParams());
                    row.addView(valueView, marginParams(0, 4, 0, 6));
                    Button delete = actionButton("Delete", false);
                    delete.setOnClickListener(v -> apiClient.deleteMemory(key, new ApiClient.Callback<JSONObject>() {
                        @Override public void onSuccess(JSONObject result) { showMemory(drawer); }
                        @Override public void onError(Exception error) { showError(valueView, friendlyError(error)); }
                    }));
                    row.addView(delete, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    page.addView(row, marginParams(0, 0, 0, 10));
                }
            }
            @Override public void onError(Exception error) {
                page.removeView(progress);
                TextView failure = text(friendlyError(error), 14, false);
                failure.setTextColor(getColor(R.color.tezgpt_error));
                page.addView(failure, marginParams(0, 8, 0, 0));
            }
        });
    }

    private void showAgents(DrawerLayout drawer) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.agents), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 12));
        TextView description = text("Native agent runs use the server as the execution boundary. Available tools and approval requirements are shown below.", 16, false);
        description.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(description, marginParams(0, 0, 0, 18));
        EditText agentPrompt = new EditText(this);
        agentPrompt.setHint("Describe the task for an agent");
        agentPrompt.setMinLines(3);
        agentPrompt.setGravity(Gravity.TOP | Gravity.START);
        page.addView(agentPrompt, marginParams(0, 0, 0, 8));
        Button runAgent = actionButton("Run agent", true);
        TextView runOutput = text("", 14, false);
        runOutput.setTextColor(getColor(R.color.tezgpt_text_muted));
        runAgent.setOnClickListener(v -> {
            String prompt = agentPrompt.getText().toString().trim();
            if (prompt.isEmpty()) return;
            runAgent.setEnabled(false);
            runOutput.setText("Starting agent…\\n");
            apiClient.startAgent(prompt, conversationId, "", new ApiClient.StreamCallback() {
                @Override public void onChunk(String text) { runOutput.append(text); }
                @Override public void onComplete(String fullText) { runAgent.setEnabled(true); runOutput.append("\\n\\nCompleted."); }
                @Override public void onError(Exception error) { runAgent.setEnabled(true); runOutput.setText(friendlyError(error)); }
            });
        });
        page.addView(runAgent, matchParams());
        page.addView(runOutput, marginParams(0, 10, 0, 16));
        ProgressBar progress = new ProgressBar(this);
        page.addView(progress, marginParams(0, 0, 0, 12));
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.agents));
        apiClient.availableTools(new ApiClient.Callback<JSONArray>() {
            @Override public void onSuccess(JSONArray tools) {
                page.removeView(progress);
                if (tools.length() == 0) {
                    page.addView(text("No tools are available for this account.", 16, false), marginParams(0, 8, 0, 0));
                    return;
                }
                for (int i = 0; i < tools.length(); i++) {
                    JSONObject tool = tools.optJSONObject(i);
                    if (tool == null) continue;
                    String name = tool.optString("name", tool.optString("id", "Unnamed tool"));
                    String risk = tool.optString("risk", "unknown");
                    boolean approval = tool.optBoolean("approvalRequired", true);
                    Button row = actionButton(name + " · " + risk + (approval ? " · approval" : ""), false);
                    row.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
                    page.addView(row, marginParams(0, 0, 0, 8));
                }
            }
            @Override public void onError(Exception error) {
                page.removeView(progress);
                TextView failure = text(friendlyError(error), 14, false);
                failure.setTextColor(getColor(R.color.tezgpt_error));
                page.addView(failure, marginParams(0, 8, 0, 0));
            }
        });
    }

    private void showFiles(DrawerLayout drawer, String selectedUri) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(getString(R.string.files), 28, true);
        page.addView(heading, marginParams(0, 18, 0, 12));
        TextView description = text("Choose a document or image from this device. The selected URI stays inside the native Android flow.", 16, false);
        description.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(description, marginParams(0, 0, 0, 18));
        Button choose = actionButton(getString(R.string.choose_file), true);
        choose.setOnClickListener(v -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(picker, FILE_PICKER_REQUEST);
        });
        page.addView(choose, matchParams());
        if (selectedUri != null && !selectedUri.isEmpty()) {
            TextView selected = text("Selected: " + selectedUri, 13, false);
            selected.setTextIsSelectable(true);
            page.addView(selected, marginParams(0, 16, 0, 8));
            Button upload = actionButton("Upload to TezGPT", true);
            upload.setOnClickListener(v -> uploadSelectedFile(upload));
            page.addView(upload, matchParams());
        }
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(getString(R.string.files));
    }

    private void uploadSelectedFile(Button uploadButton) {
        if (selectedFileUri == null) return;
        uploadButton.setEnabled(false);
        uploadButton.setText("Uploading…");
        new Thread(() -> {
            try {
                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
                try (java.io.InputStream input = getContentResolver().openInputStream(selectedFileUri)) {
                    if (input == null) throw new java.io.IOException("Unable to open selected file");
                    byte[] chunk = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = input.read(chunk)) != -1) {
                        total += read;
                        if (total > 25 * 1024 * 1024) throw new java.io.IOException("File exceeds the 25 MB native upload limit");
                        buffer.write(chunk, 0, read);
                    }
                }
                String mime = getContentResolver().getType(selectedFileUri);
                apiClient.uploadFile(buffer.toByteArray(), "attachment", mime, new ApiClient.Callback<JSONObject>() {
                    @Override public void onSuccess(JSONObject value) { uploadButton.setEnabled(true); uploadButton.setText("Uploaded"); }
                    @Override public void onError(Exception error) { uploadButton.setEnabled(true); uploadButton.setText(friendlyError(error)); }
                });
            } catch (Exception error) {
                runOnUiThread(() -> { uploadButton.setEnabled(true); uploadButton.setText(friendlyError(error)); });
            }
        }).start();
    }

    private void showNativeSection(DrawerLayout drawer, String title, String description) {
        drawer.closeDrawer(Gravity.LEFT);
        android.widget.FrameLayout shellContent = findViewById(R.id.shell_content);
        shellContent.removeAllViews();
        LinearLayout page = verticalRoot();
        page.setGravity(Gravity.TOP);
        TextView heading = text(title, 28, true);
        heading.setTextColor(getColor(R.color.tezgpt_text));
        page.addView(heading, marginParams(0, 18, 0, 12));
        TextView body = text(description, 16, false);
        body.setTextColor(getColor(R.color.tezgpt_text_muted));
        page.addView(body, marginParams(0, 0, 0, 22));
        Button back = actionButton(getString(R.string.chat), true);
        back.setOnClickListener(v -> showChat());
        page.addView(back, matchParams());
        shellContent.addView(page, matchParams());
        ((TextView) findViewById(R.id.shell_title)).setText(title);
    }

    private void sendCurrentMessage() {
        String prompt = messageInput.getText().toString().trim();
        if (prompt.isEmpty()) return;
        if (prompt.length() > 20000) {
            showError(chatError, "Message is too long. Please keep it under 20,000 characters.");
            chatError.setVisibility(View.VISIBLE);
            return;
        }
        messageInput.setText("");
        emptyChatText.setVisibility(View.GONE);
        chatError.setVisibility(View.GONE);
        sendButton.setEnabled(false);
        chatProgress.setVisibility(View.VISIBLE);
        appendMessage(getString(R.string.you), prompt, true);
        TextView assistant = appendMessage(getString(R.string.assistant), "", false);
        String selectedEndpoint = String.valueOf(endpointSpinner.getSelectedItem());
        String endpoint = selectedEndpoint.equals(getString(R.string.endpoint_default)) ? "" : selectedEndpoint;
        String selectedModel = String.valueOf(modelSpinner.getSelectedItem());
        String model = selectedModel.equals(getString(R.string.model_default)) ? "" : selectedModel;
        projectMemoryStore.recordMessage(conversationId, "user", prompt, endpoint, model, codingMode);

        apiClient.userKeyExpiry(endpoint, new ApiClient.Callback<JSONObject>() {
            @Override public void onSuccess(JSONObject keyState) {
                sendNativeChat(prompt, endpoint, model, keyState.optString("expiresAt", ""), assistant);
            }
            @Override public void onError(Exception error) {
                // Endpoints backed by a server credential do not need a user-key
                // expiry, so continue without it and let the server choose safely.
                sendNativeChat(prompt, endpoint, model, "", assistant);
            }
        });
    }

    private void sendNativeChat(String prompt, String endpoint, String model, String userKeyExpiry,
                                TextView assistant) {
        apiClient.sendMessage(prompt, conversationId, parentMessageId, endpoint, model, userKeyExpiry,
                new ApiClient.StreamCallback() {
                    @Override public void onChunk(String text) {
                        assistant.append(text);
                        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
                    }
                    @Override public void onComplete(String fullText) {
                        projectMemoryStore.recordMessage(conversationId, "assistant", fullText, endpoint, model, codingMode);
                        sendButton.setEnabled(true);
                        chatProgress.setVisibility(View.GONE);
                        messageScroll.post(() -> messageScroll.fullScroll(View.FOCUS_DOWN));
                    }
                    @Override public void onError(Exception error) {
                        sendButton.setEnabled(true);
                        chatProgress.setVisibility(View.GONE);
                        assistant.setText(friendlyError(error));
                        chatError.setText(friendlyError(error));
                        chatError.setVisibility(View.VISIBLE);
                    }
                });
    }

    private TextView appendMessage(String sender, String content, boolean user) {
        TextView message = text(content, 16, false);
        message.setTextColor(getColor(R.color.tezgpt_text));
        message.setBackgroundResource(user ? R.drawable.bg_message_user : R.drawable.bg_message_assistant);
        message.setTextIsSelectable(true);
        message.setContentDescription(sender + ": " + content);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        messageContainer.addView(message, params);
        return message;
    }

    private void clearConversation() {
        conversationId = "new";
        parentMessageId = "";
        if (messageContainer != null) messageContainer.removeAllViews();
        if (emptyChatText != null) emptyChatText.setVisibility(View.VISIBLE);
        if (chatError != null) chatError.setVisibility(View.GONE);
    }

    private LinearLayout verticalRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 48, 32, 48);
        root.setBackgroundColor(getColor(R.color.tezgpt_background));
        return root;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(getColor(R.color.tezgpt_text));
        if (bold) view.setTypeface(null, android.graphics.Typeface.BOLD);
        return view;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setBackgroundResource(primary ? R.drawable.bg_primary_button : R.drawable.bg_secondary_button);
        button.setTextColor(getColor(primary ? R.color.tezgpt_white : R.color.tezgpt_text));
        return button;
    }

    private LinearLayout.LayoutParams marginParams(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private LinearLayout.LayoutParams matchParams() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private String normalizeServerUrl(String raw) {
        String url = raw == null ? "" : raw.trim();
        while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.isEmpty() || !url.startsWith("https://")) return null;
        if (url.contains("your-tezgpt-domain.example") || url.contains("your-real-server.example")) return null;
        try {
            java.net.URI parsed = new java.net.URI(url);
            if (parsed.getHost() == null || parsed.getUserInfo() != null || parsed.getQuery() != null || parsed.getFragment() != null) return null;
        } catch (Exception ignored) {
            return null;
        }
        return url;
    }

    private String validateCredentials(String email, String password) {
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return "Email and password are required.";
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Enter a valid email address.";
        if (password.length() < 8) return "Password must be at least 8 characters.";
        return null;
    }

    private String validateRegistration(String name, String username, String email, String password) {
        String credentialsError = validateCredentials(email, password);
        if (credentialsError != null) return credentialsError;
        if (name.length() > 100) return "Name is too long.";
        if (username.length() < 3 || username.length() > 40) return "Username must be 3 to 40 characters.";
        if (!username.matches("[A-Za-z0-9._-]+")) return "Username contains unsupported characters.";
        return null;
    }

    private void showError(TextView target, String value) {
        target.setText(value);
        target.setVisibility(View.VISIBLE);
    }

    private String friendlyError(Exception e) {
        if (e == null || e.getMessage() == null) return getString(R.string.network_error);
        return e.getMessage();
    }
}
