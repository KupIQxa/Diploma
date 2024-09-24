package com.example.diplom;

import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.diplom.ui.home.HomeFragment;
import com.example.diplom.ui.statistics.SmsDataListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SmsReceiver extends BroadcastReceiver implements OnServerResponseListener, OnServerToxicCheck {

    private Context context;

    private String Message;

    private String Sender;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            // Получите SMS сообщения из интента
            Object[] pdus = (Object[]) intent.getExtras().get("pdus");

            // Обрабатывайте каждое SMS сообщение
            for (Object pdu : pdus) {
                SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
                String sender = sms.getOriginatingAddress();
                String message = sms.getMessageBody();
                Message = message;
                this.context = context;

                // Получаем имя контакта по номеру телефона
                String contactName = getContactName(context, sender);
                Sender = contactName != null ? contactName : sender;

                sendSmsForAnalysis(sender, message);
            }
        }
    }

    private void deleteSms(Context context, String sender, String message) {
        try {
            sendNotification(context,"1");
            Uri uriSms = Uri.parse("content://sms/inbox");
            ContentResolver contentResolver = context.getContentResolver();

            Cursor cursor = contentResolver.query(uriSms, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                    if (address.equals(sender) && body.equals(message)) {
                        // Получаем ID сообщения для удаления
                        int id = cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Sms._ID));
                        context.getContentResolver().delete(
                                Uri.parse("content://sms/" + id), null, null);
                        Log.d("TAG", "SMS удалено: " + body);
                        sendNotification(context,"1");
                        break;
                    }
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.e("TAG", "Ошибка при удалении SMS: " + e.getMessage());
        }
    }

    private String getContactName(Context context, String phoneNumber) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = contentResolver.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                if (nameIndex != -1) {
                    String contactName = cursor.getString(nameIndex);
                    cursor.close();
                    return contactName;
                }
            }
            cursor.close();
        }
        return phoneNumber; // Возвращаем номер, если контакт не найден
    }


    private void sendSmsForAnalysis(String sender, String message) {
        isSenderBlocked(sender, isBlocked -> {
            if (isBlocked) {
                Log.d("TAG", "Отправитель заблокирован: " + sender);
                sendNotification(context, "Заблокированный");
                deleteSms(context, Sender, Message);
                return; // Если отправитель заблокирован, не отправляем SMS на анализ
            }

            HTTPRequest request = new HTTPRequest(context);
            SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
            String authToken = sharedPreferences.getString("auth_token", null);

            request.toxicInTextToFlaskServer(message, authToken, SmsReceiver.this);
        });
    }

    private void isSenderBlocked(String sender, Consumer<Boolean> callback) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
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


    @Override
    public void onServerResponse(String response) throws JSONException {
        // Обработка ответа от сервера
        Log.d("TAG", "Ответ: " + response);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        boolean blockAngryMessages = preferences.getBoolean("delete_notifications", false);
        boolean blockAngrySender = preferences.getBoolean("block_senders", false);
        String emotion = resultProcessing(response);
        String TranslateEmotion = translateEmotion(emotion);
        if (TranslateEmotion.equals("злость") && blockAngryMessages) {
            deleteSms(context,Sender,Message);
            if (blockAngrySender) {
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
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
                request.blockSender(authToken, Sender, callback);
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
        Log.d("TAG", "Ошибка: " + errorMessage);
        String[] s = errorMessage.split(" ");
        String author = s[0];
        String sms = s[1];
        String authToken = s[2];
        HTTPRequest request = new HTTPRequest(context);
        request.analyzeTextToFlaskServer(sms, author, authToken, SmsReceiver.this);
    }

    public String resultProcessing(String result) throws JSONException {
        String[] parts = result.split(" смс: | результат: ");

        String author = parts[0].substring("Отправитель: ".length()); // Убирает префикс "Отправитель: "
        String textvalue = parts[1];
        String responseData = parts[2];

        JSONArray jsonArray = new JSONArray(responseData);
        List<String> labels = new ArrayList<>();
        List<Double> scores = new ArrayList<>();
        double max= 0;
        // Пройдитесь по массиву
        for (int i = 0; i < jsonArray.length(); i++) {
            // Получите каждый объект JSON из массива
            JSONObject jsonObject = jsonArray.getJSONObject(i);

            // Извлеките значения "label" и "score"
            String label = jsonObject.getString("label");
            double score = jsonObject.getDouble("score");
            labels.add(label);
            scores.add(score);
            if(score>max) max = score;
        }
        int i = 0;
        for (double score: scores) {
            if(max == score) break;
            i++;
        }
        HTTPRequest request = new HTTPRequest(context);
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);
        request.ResultToFlaskServer(Sender, Message, labels.get(i), authToken);
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
        if(emotion.equals("1")){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText("Удаление")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        }
        else if(emotion.equals("Блокировка")){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText("Отправитель - "+Sender+", успешно было заблокирован")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        }
        else if(emotion.equals("Заблокированный")){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText("Пришло смс сообщение от заблокированного отправителя")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

            // Отправляем уведомление
            notificationManager.notify(notificationId, builder.build());
        }
        else {
            String full = "Отправитель смс: "+Sender+ ", Эмоция: ";
            // Создаем уведомление с использованием NotificationCompat.Builder
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "my_channel_id")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Родительский смс-контроль")
                    .setContentText(full+emotion)
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

            if(isToxic) {
                sendNotification(context, "злость");
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
                boolean blockAngryMessages = preferences.getBoolean("delete_notifications", false);
                if (blockAngryMessages) {
                    deleteSms(context, Sender, Message);
                }
                boolean blockAngrySender = preferences.getBoolean("block_senders", false);
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                String authToken = sharedPreferences.getString("auth_token", null);
                HTTPRequest request = new HTTPRequest(context);
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
                    request.blockSender(authToken, Sender, callback);
                }
                request.ResultToFlaskServer(Sender, Message, "anger", authToken);
            }
            else
            {
                HTTPRequest request = new HTTPRequest(context);
                SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                String authToken = sharedPreferences.getString("auth_token", null);
                request.analyzeTextToFlaskServer(Message, Sender, authToken, SmsReceiver.this);
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

