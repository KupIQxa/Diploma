package com.example.diplom;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HTTPRequest {

    private Context context;
    private String baseUrl;

    public void setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
    }

    public void backBaseUrl(){
        this.baseUrl = "http://192.168.0.104:5000";
    }

    public HTTPRequest(Context context) {
        this.context = context;
        loadBaseUrlFromPreferences();
    }

    private void loadBaseUrlFromPreferences() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.baseUrl = preferences.getString("url_preference", "http://192.168.0.104:5000");
    }

    private void saveBaseUrlToPreferences(String url) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("url_preference", url);
        editor.apply();
    }

    public HTTPRequest(String baseUrl){
        this.baseUrl = baseUrl;
    }
    public void sendTextToFlaskServer(String textvalue, String authToken, final OnServerResponseListener listener) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/analyze";

        String json = "{\"text\":\"" + textvalue + "\"}";
        // Создание тела запроса с текстом
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        // Создание POST-запроса
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .build();


        // Отправка запроса асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки
                e.printStackTrace();
                Log.d("TAG", "Ошибка" + e);
                //showToast("Ошибка" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа от сервера
                    final String responseData = response.body().string();
                    Log.d("TAG", "Хорошо: " + responseData);
                    try {
                        listener.onServerResponse(responseData);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    // Вывод результата анализа через Toast
                    //showToast(responseData);
                } else {
                    // Обработка неуспешного ответа
                    // Вывод ошибки через Toast
                    listener.onServerError("Неудача: " + response.code());
                    Log.d("TAG", "Неудача: " + response.code());
                    //showToast("Неудача: " +response.code());
                }
            }
        });
    }

    public void sendRegisterRequest(String fullName, String email, String password, final OnServerResponseListener listener) {

        final OkHttpClient client = new OkHttpClient();
        // Создаем JSON-объект для отправки на сервер
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        String json = "{\"full_name\":\"" + fullName + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(baseUrl + "/register")
                .post(body)
                .build();

        // Отправляем запрос
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                JSONObject jsonResponse = null;
                try {
                    jsonResponse = new JSONObject(responseData);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if (response.isSuccessful()) {
                    // Обработка успешного ответа от сервера
                    Log.d("TAG", "Успех: " + responseData);
                    try {
                        listener.onServerResponse(jsonResponse.getString("message"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Обработка неуспешного ответа
                    Log.d("TAG", "Неудача: " + response.code());
                    String errorMessage = "Ошибка: " + response.code() + " - " + responseData;  // включите тело ответа в сообщение об ошибке
                    try {
                        listener.onServerError(jsonResponse.getString("error"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void login(String email, String password, final OnServerResponseListener listener) {
        final String AUTH_ENDPOINT = baseUrl + "/login";
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.get("application/json; charset=utf-8");

        // Создаем JSON-объект для запроса
        String jsonBody = "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}";
        RequestBody body = RequestBody.create(JSON, jsonBody);

        // Создаем запрос POST
        Request request = new Request.Builder()
                .url(AUTH_ENDPOINT)
                .post(body)
                .build();

        // Отправляем запрос и обрабатываем ответ
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                JSONObject jsonResponse = null;
                try {
                    jsonResponse = new JSONObject(responseData);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                if (response.isSuccessful()) {
                    // Обработка успешного ответа от сервера
                    Log.d("TAG", "Успех: " + responseData);
                    try {
                        String token = jsonResponse.getString("token");
                        listener.onServerResponse(token);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    // Обработка неуспешного ответа
                    Log.d("TAG", "Неудача: " + response.code());
                    String errorMessage = "Ошибка: " + response.code() + " - " + responseData;  // включите тело ответа в сообщение об ошибке
                    try {
                        listener.onServerError(jsonResponse.getString("error"));
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    public void analyzeTextToFlaskServer(String textvalue, String author, String authToken, final OnServerResponseListener listener) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl+ "/analyze";

        String json = "{\"text\":\"" + textvalue + "\"}";
        // Создание тела запроса с текстом
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        // Создание POST-запроса
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .build();


        // Отправка запроса асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки
                e.printStackTrace();
                Log.d("TAG", "Ошибка" + e);
                //showToast("Ошибка" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа от сервера
                    final String responseData = response.body().string();
                    Log.d("TAG", "Хорошо: " + responseData);
                    String finalres = "Отправитель: "+ author+" смс: "+textvalue+" результат: "+responseData;
                    try {
                        listener.onServerResponse(finalres);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }

                } else {
                    String a = author +" "+textvalue+" "+authToken;
                    listener.onServerError(a);
                    Log.d("TAG", "Неудача: " + response.code());
                }
            }
        });
    }




    public void logout(String authToken, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(baseUrl+"/logout") // Замените на ваш URL
                .post(RequestBody.create("", MediaType.parse("application/json"))) // Пустое тело POST-запроса
                .header("Authorization", "Bearer " + authToken) // Добавьте токен пользователя
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void profile(String authToken, Callback callback){
        OkHttpClient client = new OkHttpClient();

        String url = baseUrl + "/profile";

        // Создаем заголовки запроса, включая токен аутентификации
        Headers headers = new Headers.Builder()
                .add("Authorization", "Bearer " + authToken)
                .build();

        // Создаем запрос
        Request request = new Request.Builder()
                .url(url)
                .get()
                .headers(headers)
                .build();

        client.newCall(request).enqueue(callback);
    }




    public void toxicInTextToFlaskServer(String textvalue, String authToken, final OnServerToxicCheck listener) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/check_banned_words";

        String json = "{\"text\":\"" + textvalue + "\"}";
        // Создание тела запроса с текстом
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        // Создание POST-запроса
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .build();


        // Отправка запроса асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки
                e.printStackTrace();
                Log.d("TAG", "Ошибка" + e);
                //showToast("Ошибка" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("TAG", "ответ" + responseBody);
                    // Вы можете обработать responseBody, например, преобразовать его в JSON и получить нужные данные
                    // Здесь можно вызвать обратный вызов и передать ответ
                    listener.onServerResponseToxicCheck(responseBody);

                } else {
                    listener.onServerErrorMsg("Ошибка: " + response.message());
                }
            }
        });
    }

    public void ResultToFlaskServer(String author, String textvalue, String emotion, String authToken) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/save_sms";

        String json = String.format("{\"author\": \"%s\", \"text\": \"%s\", \"emotion\": \"%s\"}",
                author, textvalue, emotion);
        // Создание тела запроса с текстом
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);

        // Создание POST-запроса
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("Content-Type", "application/json")
                .build();


        // Отправка запроса асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки
                e.printStackTrace();
                Log.d("TAG", "Ошибка" + e);
                //showToast("Ошибка" + e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d("TAG", "ответ" + responseBody);
                    // Вы можете обработать responseBody, например, преобразовать его в JSON и получить нужные данные
                    // Здесь можно вызвать обратный вызов и передать ответ
                } else {

                }
            }
        });
    }

    public void ResultsFromFlaskServer(String authToken, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/get_user_notifications";

        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + authToken) // Добавление заголовка с токеном авторизации, если требуется
                .build();


        client.newCall(request).enqueue(callback);
    }

    public void ToxicToFlaskServer(String toxic, String authToken, final AddToxicWord listener) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/add_banned_word";

        String jsonData = String.format("{\"word\": \"%s\"}", toxic);

        // Создаем тело запроса
        RequestBody body = RequestBody.create(
                jsonData,
                MediaType.parse("application/json")
        );

        // Создаем запрос
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        // Выполняем запрос асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Проверяем успешность запроса
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        listener.onServerResponseGood(responseBody);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    try {
                        listener.onServerNotAdd(responseBody);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void deleteMessageFromFlaskServer(String authToken, final OnServerResponseListener listener) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");
        // URL вашего Flask-приложения
        String url = baseUrl + "/delete_all_messages";


        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        // Выполняем запрос асинхронно
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // Проверяем успешность запроса
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    try {
                        listener.onServerResponse(responseBody);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    listener.onServerError(responseBody);
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    public boolean deleteMessageFromFlaskServerSync(String authToken) {
        OkHttpClient client = new OkHttpClient();
        Log.d("TAG", "Зашёл в функцию");

        // URL вашего Flask-приложения
        String url = baseUrl + "/delete_all_messages";

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        // Выполняем запрос синхронно
        try {
            Response response = client.newCall(request).execute();

            // Проверяем успешность запроса
            if (response.isSuccessful()) {
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String fetchDataFromProfile(String authToken) {
        OkHttpClient client = new OkHttpClient();

        // Создание запроса к эндпоинту /profile
        Request request = new Request.Builder()
                .url(baseUrl +"/profile")
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        try {
            // Выполнение синхронного запроса
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                try {
                    JSONObject jsonObject = new JSONObject(responseData);
                    Log.d("TAG", "ответ: " + responseData);
                    // Извлекайте данные из JSON-объекта
                    String encodedFullName = jsonObject.getString("full_name");
                    // Декодируйте строку full_name в UTF-8
                    String fullName = new String(encodedFullName.getBytes(), "UTF-8");
                    String email = jsonObject.getString("email");
                    // Вы можете извлечь другие данные из JSON-объекта в зависимости от его структуры
                    Log.d("TAG", "имя: " + fullName);
                    Log.d("TAG", "почта: " + email);
                    String Res = fullName+">"+email;
                    return Res;
                } catch (JSONException e) {
                    // Обработка ошибки парсинга JSON
                    e.printStackTrace();
                    return "error";
                }
            } else {
                // Обработка ошибки
                return "false";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String fetchDataFromMessages(String authToken) {
        OkHttpClient client = new OkHttpClient();

        // Создание запроса к эндпоинту /get_user_notifications
        Request request = new Request.Builder()
                .url(baseUrl +"/get_user_notifications")
                .addHeader("Authorization", "Bearer " + authToken)
                .build();

        try {
            // Выполнение синхронного запроса
            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                String responseData = response.body().string();
                return responseData;
            } else {
                return "false";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "error";
        }
    }


    public void blockSender(String authToken, String sender, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sender", sender);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(baseUrl + "/block_sender")
                .addHeader("Authorization", "Bearer " + authToken)
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void unblockSender(String authToken, String sender, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("sender", sender);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(baseUrl + "/unblock_sender")
                .addHeader("Authorization", "Bearer " + authToken)
                .delete(requestBody)  // Изменено на DELETE
                .build();
        client.newCall(request).enqueue(callback);
    }


    public void blockingListOfSenders(String authToken, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(baseUrl + "/blocked_senders")
                .addHeader("Authorization", "Bearer " + authToken)
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void unblockSenders(String authToken, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(baseUrl + "/clear_blocked_senders")
                .addHeader("Authorization", "Bearer " + authToken)
                .delete()  // Изменено на DELETE
                .build();
        client.newCall(request).enqueue(callback);
    }

    public void checkTokenValidity(String authToken, Callback callback) {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(baseUrl+"/check_token")  // Замените URL на ваш
                .addHeader("Authorization", "Bearer " + authToken)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
