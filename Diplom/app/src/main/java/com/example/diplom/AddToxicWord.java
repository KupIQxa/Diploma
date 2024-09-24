package com.example.diplom;

import org.json.JSONException;

public interface AddToxicWord {
    void onServerResponseGood(String response) throws JSONException;
    void onServerNotAdd(String errorMessage) throws JSONException;
}
