package com.tezgpt.app.agents;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.tezgpt.app.api.ApiClient;
import com.tezgpt.app.storage.SessionStore;

public final class AgentSyncWorker extends Worker {
    public AgentSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String conversationId = getInputData().getString("conversationId");
        if (conversationId == null) conversationId = "";
        if (conversationId.isEmpty()) return Result.failure();
        SessionStore store = new SessionStore(getApplicationContext());
        if (!store.isAuthenticated()) return Result.failure();
        ApiClient client = new ApiClient(store);
        try {
            client.agentStatus(conversationId, new ApiClient.Callback<org.json.JSONObject>() {
                @Override public void onSuccess(org.json.JSONObject value) { }
                @Override public void onError(Exception error) { }
            });
            return Result.success();
        } catch (Exception error) {
            return Result.retry();
        }
    }
}
