package com.tezgpt.app.tools;

import org.json.JSONObject;

public final class ToolDefinition {
    public final String id;
    public final String name;
    public final String description;
    public final String risk;
    public final boolean approvalRequired;
    public final JSONObject inputSchema;

    public ToolDefinition(String id, String name, String description, String risk,
                          boolean approvalRequired, JSONObject inputSchema) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.risk = risk;
        this.approvalRequired = approvalRequired;
        this.inputSchema = inputSchema == null ? new JSONObject() : inputSchema;
    }

    public static ToolDefinition fromJson(JSONObject value) {
        return new ToolDefinition(
                value.optString("id", value.optString("name", "")),
                value.optString("name", "Unnamed tool"),
                value.optString("description", ""),
                value.optString("risk", "unknown"),
                value.optBoolean("approvalRequired", true),
                value.optJSONObject("inputSchema"));
    }
}
