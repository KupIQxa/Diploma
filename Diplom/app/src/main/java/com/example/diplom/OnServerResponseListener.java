package com.example.diplom;

import org.json.JSONException;

public interface OnServerResponseListener {
    void onServerResponse(String response) throws JSONException;
    void onServerError(String errorMessage);
}