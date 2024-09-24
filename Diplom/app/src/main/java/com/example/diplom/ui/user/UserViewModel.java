package com.example.diplom.ui.user;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.diplom.HTTPRequest;
import com.example.diplom.LoginActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class UserViewModel extends ViewModel{
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private Context context;
    // Инициализация ViewModel
    public UserViewModel(Context context) {
        this.context = context;

        currentUser.setValue(new User("Имя", "Почта"));
    }

    // Метод для получения LiveData текущего пользователя
    public LiveData<User> getUser() {
        return currentUser;
    }

    // Метод для установки текущего пользователя
    public void setUser(User user) {
        currentUser.setValue(user);
    }

    // Метод для выхода из профиля
    public void logout() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);
        Log.d("TAG", "токен: " + authToken);
        HTTPRequest request = new HTTPRequest(context);
        request.logout(authToken, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Обработка ошибки запроса
                e.printStackTrace();
                // Обновите UI или оповестите пользователя об ошибке выхода
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                    final String responseData = response.body().string();
                    response.body().close();
                    sharedPreferences.edit().remove("auth_token").apply();
                    navigateToLoginScreen();
            }
        });
    }

    private void navigateToLoginScreen() {
        // Создайте Intent для запуска активности авторизации
        Intent intent = new Intent(context, LoginActivity.class);
        // Установите флаги для очистки текущей активности и запуска новой активности
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Запустите активность авторизации
        context.startActivity(intent);
    }


}
