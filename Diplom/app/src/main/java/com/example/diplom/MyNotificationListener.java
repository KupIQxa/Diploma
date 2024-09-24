package com.example.diplom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MyNotificationListener extends NotificationListenerService implements OnServerResponseListener, OnServerToxicCheck {

    private String Text;

    private String Title;

    private String Appname;
    private Context context;

    private String Key;

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Получите текст уведомления
        Notification notification = sbn.getNotification();
        String packageName = sbn.getPackageName();
        this.context = getApplicationContext();
        String myPackageName = context.getPackageName();
        Key = sbn.getKey();

        if (packageName.equals(myPackageName)) {
            return; // Останавливаем обработку уведомления
        }

        Bundle extras = notification.extras;
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);

        Title = title != null ? title.toString() : "";
        Text = text != null ? text.toString() : "";

        if (Title.equals("parentalcontrol58ru@yandex.ru")) return;

        if (Title == null || Title.equals("")) {
            cancelNotificationByKey(Key);
            return;
        }

        String appName;
        try {
            // Получаем информацию о приложении по имени пакета
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(packageName, 0);
            // Получаем название приложения
            CharSequence appLabel = getPackageManager().getApplicationLabel(appInfo);
            appName = appLabel != null ? appLabel.toString() : packageName; // Проверяем на null и используем имя пакета, если название не доступно
            Appname = appName;
        } catch (PackageManager.NameNotFoundException e) {
            appName = packageName; // Если приложение не найдено, используем имя пакета
            Appname = appName;
        }

        if (Appname.equals("Сообщения") || Appname.equals("Системный UI")) return;

        if(Appname.equals("com.vkontakte.android")) Appname="Vkontakte";
        if(Appname.equals("org.telegram.messenger")) Appname="Telegram";
        if(Appname.equals("ru.mail.mailapp")) Appname="Mail";

        isSenderBlocked(Title, isBlocked -> {
            if (isBlocked) {
                sendNotification(context, "Заблокированный");
                cancelNotificationByKey(Key);
            } else {
                if (!Title.isEmpty() && !Text.isEmpty()) {
                    sendSmsForAnalysis(Title, Text);
                }
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

        HTTPRequest request = new HTTPRequest(context);
        request.blockingListOfSenders(authToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("TAG", "Ошибка запроса блокировки отправителя", e);
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        JSONArray blockedSenders = jsonResponse.getJSONArray("blocked_senders");

                        for (int i = 0; i < blockedSenders.length(); i++) {
                            JSONObject senderObject = blockedSenders.getJSONObject(i);
                            String encodedSender = senderObject.getString("sender");
                            if (sender.equals(encodedSender)) {
                                new Handler(Looper.getMainLooper()).post(() -> callback.accept(true));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("TAG", "Ошибка обработки JSON ответа", e);
                    }
                }
                new Handler(Looper.getMainLooper()).post(() -> callback.accept(false));
            }
        });
    }


    public void cancelNotificationByKey(String key) {
        cancelNotification(key);
    }


    private void sendSmsForAnalysis(String appname, String text) {
        HTTPRequest request = new HTTPRequest(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        // Получаем токен авторизации из SharedPreferences
        String authToken = sharedPreferences.getString("auth_token", null);
        request.toxicInTextToFlaskServer(text, authToken, MyNotificationListener.this);
    }

    @Override
    public void onServerResponse(String response) throws JSONException {
        // Обработка ответа от сервера
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean blockAngryMessages = preferences.getBoolean("delete_notifications", false);
        boolean blockAngrySender = preferences.getBoolean("block_senders", false);
        Log.d("TAG", "Ответ: " + response);
        String emotion = resultProcessing(response);
        String TranslateEmotion = translateEmotion(emotion);
        if (TranslateEmotion.equals("злость") && blockAngryMessages) {
            cancelNotificationByKey(Key);
            sendNotification(this.context, TranslateEmotion);
            if (blockAngrySender) {
                SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                String authToken = sharedPreferences.getString("auth_token", null);
                HTTPRequest request = new HTTPRequest(context);

                Callback callback = new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        // Обработка ошибки запроса
                        e.printStackTrace();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            sendNotification(context, "Блокировка");
                        }
                    }
                };
                request.blockSender(authToken, Title, callback);
            }
        } else {
            sendNotification(this.context, TranslateEmotion);
        }
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

    @Override
    public void onServerError(String errorMessage) {
        // Обработка ошибки при запросе к серверу
        String[] s = errorMessage.split(" ");
        String author = s[0];
        String sms = s[1];
        String authToken = s[2];
        HTTPRequest request = new HTTPRequest(context);
        request.analyzeTextToFlaskServer(sms, author, authToken, MyNotificationListener.this);
    }

    public String resultProcessing(String result) throws JSONException {
        String[] parts = result.split(" смс: | результат: ");

        String appname = parts[0].substring("Отправитель: ".length()); // Убирает префикс "Отправитель: "
        String textvalue = parts[1];
        String responseData = parts[2];

        JSONArray jsonArray = new JSONArray(responseData);
        List<String> labels = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        double max = 0;
        // Пройдитесь по массиву
        for (int i = 0; i < jsonArray.length(); i++) {
            // Получите каждый объект JSON из массива
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            // Извлеките значения "label" и "score"
            String label = jsonObject.getString("label");
            double score = jsonObject.getDouble("score");
            labels.add(label);
            scores.add(score);
            if (score > max) max = score;
        }
        int i = 0;
        for (double score : scores) {
            if (max == score) break;
            i++;
        }
        HTTPRequest request = new HTTPRequest(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);
        request.ResultToFlaskServer(Title, Text, labels.get(i), authToken);
        return labels.get(i);
    }

    public void sendNotification(Context context, String emotion) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Создаем канал уведомлений (для Android O и выше)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("my_channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Создаем PendingIntent
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        if (emotion.equals("Блокировка")) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText("Отправитель - " + Title + ", успешно было заблокирован")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        } else if (emotion.equals("Заблокированный")) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText("Пришло сообщение от заблокированного отправителя")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        } else {
            String full = "Приложение: " + Appname + " Заголовок: " + Title + " Эмоция: ";

            // Создаем уведомление с использованием NotificationCompat.Builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText(full + emotion)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        }
    }

    @Override
    public void onServerResponseToxicCheck(String response) {
        try {
            Log.d("TAG", "слушатель: " + response);
            // Преобразование строки ответа в JSONObject
            JSONObject jsonResponse = new JSONObject(response);

            // Извлечение данных из JSON-ответа
            boolean isToxic = jsonResponse.getBoolean("contains_banned_words");

            String encodedmsg = jsonResponse.getString("message");
            // Декодируйте строку full_name в UTF-8
            String msg = new String(encodedmsg.getBytes(), "UTF-8");


            // Вы можете обрабатывать данные в соответствии с вашим контекстом
            Log.d("TAG", "Текст токсичный: " + isToxic);
            Log.d("TAG", "Запрещенные слова: " + msg);

            if (isToxic) {
                sendNotification(this.context, "злость");
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
                boolean blockAngryMessages = preferences.getBoolean("delete_notifications", false);
                if (blockAngryMessages) {
                    cancelNotificationByKey(Key);
                }
                SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                String authToken = sharedPreferences.getString("auth_token", null);
                HTTPRequest request = new HTTPRequest(context);
                boolean blockAngrySender = preferences.getBoolean("block_senders", false);
                if (blockAngrySender) {

                    Callback callback = new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            // Обработка ошибки запроса
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                sendNotification(context, "Блокировка");
                            }
                        }
                    };
                    request.blockSender(authToken, Title, callback);
                }
                request.ResultToFlaskServer(Title, Text, "anger", authToken);
            }
            else
            {
                HTTPRequest request = new HTTPRequest(context);
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                String authToken = sharedPreferences.getString("auth_token", null);
                String full = "Приложение: " + Appname + " Заголовок: " + Title;
                request.analyzeTextToFlaskServer(Text, full, authToken, MyNotificationListener.this);
            }

        } catch (Exception e) {
            // Обработка исключений, если JSON не валиден или возникают другие ошибки
            Log.e("TAG", "Ошибка обработки ответа: " + e.getMessage());
        }
    }


    @Override
    public void onServerErrorMsg(String errorMessage) {
        try {
            // Преобразование строки ответа в JSONObject
            JSONObject jsonResponse = new JSONObject(errorMessage);

            // Извлечение сообщения об ошибке из JSON-ответа
            String errorMsg = jsonResponse.getString("message");


        } catch (Exception e) {
            // Обработка исключений, если JSON не валиден или возникают другие ошибки
            Log.e("TAG", "Ошибка обработки ответа: " + e.getMessage());
        }
    }
}
