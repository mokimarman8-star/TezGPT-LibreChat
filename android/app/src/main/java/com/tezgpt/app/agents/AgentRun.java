package com.tezgpt.app.agents;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class AgentRun {
    public enum Status { PLANNED, RUNNING, WAITING_FOR_APPROVAL, PAUSED, COMPLETED, FAILED, CANCELLED, EXPIRED }

    public final String id;
    public final String conversationId;
    public final String title;
    public final Status status;
    public final int completedSteps;
    public final int totalSteps;
    public final List<String> events;
    public final List<String> artifacts;

    public AgentRun(String id, String conversationId, String title, Status status,
                    int completedSteps, int totalSteps, List<String> events, List<String> artifacts) {
        this.id = id;
        this.conversationId = conversationId;
        this.title = title;
        this.status = status;
        this.completedSteps = completedSteps;
        this.totalSteps = totalSteps;
        this.events = events == null ? new ArrayList<>() : events;
        this.artifacts = artifacts == null ? new ArrayList<>() : artifacts;
    }

    public static AgentRun fromJson(JSONObject json) {
        String statusText = json.optString("status", "running").toUpperCase().replace('-', '_');
        Status status;
        try { status = Status.valueOf(statusText); }
        catch (Exception ignored) { status = Status.RUNNING; }
        List<String> events = readStrings(json.optJSONArray("events"));
        List<String> artifacts = readStrings(json.optJSONArray("artifacts"));
        return new AgentRun(
                json.optString("id", json.optString("runId", "")),
                json.optString("conversationId", ""),
                json.optString("title", "TezGPT agent run"),
                status,
                json.optInt("completedSteps", json.optInt("completed", 0)),
                json.optInt("totalSteps", json.optInt("total", 0)),
                events,
                artifacts);
    }

    private static List<String> readStrings(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) return values;
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof String) values.add((String) value);
            else if (value instanceof JSONObject) values.add(((JSONObject) value).toString());
        }
        return values;
    }
}
