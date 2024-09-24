package com.example.diplom;

import org.json.JSONException;

public interface OnServerToxicCheck {
    void onServerResponseToxicCheck(String response);
    void onServerErrorMsg(String errorMessage);
}
