package com.example.diplom;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class ChatActivity extends AppCompatActivity {
    private static final String EXTRA_SENDER_NAME = "SENDER_NAME";
    private TextView senderNameTextView;
    private TextView senderTexts;

    private TextView status;
    private String name;

    private Button blockButton;
    private Button unblockButton;
    List<Message> Messages = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        senderTexts = findViewById(R.id.senderTexts);
        senderNameTextView = findViewById(R.id.senderTextView);
        blockButton = findViewById(R.id.blocking);
        unblockButton = findViewById(R.id.unBlocking);
        status = findViewById(R.id.statusSender);
        Messages.clear();
        // Получаем имя отправителя из интента
        name = getIntent().getStringExtra(EXTRA_SENDER_NAME);
        Log.d("TAG", "Автор2: " + name);
        // Устанавливаем имя отправителя в текстовое представление
        senderNameTextView.setText("Отправитель: " + name);

        loadMsgFromServer();

        isSenderBlocked(name, isBlocked -> {
            if (isBlocked) {
                status.setText("Статус отправителя: Заблокирован");
                blockButton.setEnabled(false);
                unblockButton.setEnabled(true);
            } else {
                status.setText("Статус отправителя: Не заблокирован");
                blockButton.setEnabled(true);
                unblockButton.setEnabled(false);
            }
        });



        blockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                blockSender();
            }
        });

        unblockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unblockSender();
            }
        });
    }

    private void isSenderBlocked(String sender, Consumer<Boolean> callback) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

        if (authToken == null) {
            Log.d("TAG", "Токен отсутствует");
            callback.accept(false);
            return;
        }

        HTTPRequest request = new HTTPRequest(this);
        request.blockingListOfSenders(authToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> callback.accept(false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray blockedSenders = jsonResponse.getJSONArray("blocked_senders");

                        for (int i = 0; i < blockedSenders.length(); i++) {
                            JSONObject SENDER = blockedSenders.getJSONObject(i);
                            String encodedSender = SENDER.getString("sender");
                            if (sender.equals(encodedSender)) {
                                runOnUiThread(() -> callback.accept(true));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                runOnUiThread(() -> callback.accept(false));
            }
        });
    }


    private void loadMsgFromServer() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

        if (authToken == null) {
            // Обработка случая, когда токен отсутствует
            Log.d("TAG", "Токен отсутствует");
            return;
        }

        HTTPRequest request = new HTTPRequest(this);

        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Обработка успешного ответа
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);

                        JSONArray notificationsArray = jsonObject.getJSONArray("notifications");

                        List<Message> newMessages = new ArrayList<>();

                        for (int i = 0; i < notificationsArray.length(); i++) {
                            JSONObject notificationObject = notificationsArray.getJSONObject(i);
                            String encodedAuthor = notificationObject.getString("author");
                            String author = new String(encodedAuthor.getBytes(), "UTF-8");

                            String encodedText = notificationObject.getString("text");
                            String text = new String(encodedText.getBytes(), "UTF-8");
                            String createdAt = notificationObject.getString("created_at");

                            String emotion = notificationObject.getString("emotion");
                            String emotionTranslation = translateEmotion(emotion);

                            if(author.equals(name)){
                                Message message = new Message(text, author, createdAt, emotionTranslation);
                                newMessages.add(message);

                                Log.d("TAG", "Автор: " + author);
                                Log.d("TAG", "Текст: " + text);
                                Log.d("TAG", "Создано: " + createdAt);
                                Log.d("TAG", "Эмоция: " + emotionTranslation);
                            }
                        }

                        runOnUiThread(() -> {
                            Messages.addAll(newMessages);
                            PrintMsgs();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        request.ResultsFromFlaskServer(authToken, callback);
    }

    private String translateEmotion(String emotion) {
        switch (emotion) {
            case "neutral":
                return "нейтральное состояние";
            case "joy":
                return "радость";
            case "sadness":
                return "грусть";
            case "anger":
                return "злость";
            case "enthusiasm":
                return "энтузиазм";
            case "surprise":
                return "удивление";
            case "disgust":
                return "отвращение";
            case "fear":
                return "страх";
            case "guilt":
                return "вина";
            case "shame":
                return "стыд";
            default:
                return "неизвестная эмоция";
        }
    }

    private void PrintMsgs() {
        StringBuilder messagesText = new StringBuilder();
        for (Message message : Messages) {
            messagesText.append("Автор: ").append(message.getSender())
                    .append("\nСообщение: ").append(message.getContent())
                    .append("\nЭмоция: ").append(message.getEmotion())
                    .append("\nСоздано: ").append(message.getDats())
                    .append("\n\n");
        }
        senderTexts.setText(messagesText.toString());
    }

    private void blockSender() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

        if (authToken == null) {
            // Обработка случая, когда токен отсутствует
            Log.d("TAG", "Токен отсутствует");
            return;
        }

        HTTPRequest request = new HTTPRequest(this);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Log.d("TAG", "Отправитель заблокирован: " + name);
                        status.setText("Статус отправителя: Заблокирован");
                        blockButton.setEnabled(false);
                        unblockButton.setEnabled(true);
                    });
                }
            }
        };
        request.blockSender(authToken, name, callback);
    }

    private void unblockSender() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

        if (authToken == null) {
            // Обработка случая, когда токен отсутствует
            Log.d("TAG", "Токен отсутствует");
            return;
        }

        HTTPRequest request = new HTTPRequest(this);
        Callback callback = new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Log.d("TAG", "Отправитель разблокирован: " + name);
                        status.setText("Статус отправителя: Не заблокирован");
                        blockButton.setEnabled(true);
                        unblockButton.setEnabled(false);
                    });
                }
            }
        };
        request.unblockSender(authToken, name, callback);
    }
}
