package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import com.example.diplom.databinding.ActivityMainBinding;
import com.example.diplom.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "UserPreferences";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private static final String PREFS_NAME1 = "WorkInfoPrefs";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_DELAY_DURATION = "delayDuration";

    public static final String WORK_TAG = "MyPeriodicWork";

    private static final String TIMER_SET_KEY = "timerSet";
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Очистка токена при первом запуске после установки
        clearDataIfFirstRun();

        // Проверка статуса авторизации
        if (!isUserAuthenticated()) {
            // Если пользователь не авторизован, перенаправляем на LoginActivity
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            // Завершение MainActivity, чтобы пользователь не мог вернуться назад
            finish();
            return; // Останавливаем дальнейшее выполнение кода
        }

        // Если пользователь авторизован, продолжаем инициализацию главной активности
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_chats, R.id.navigation_statistics, R.id.navigation_user)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME1, Context.MODE_PRIVATE);
        boolean timerSet = sharedPreferences.getBoolean(TIMER_SET_KEY, false);

        if (!timerSet) {
            // Если таймер еще не установлен, устанавливаем его
            setTimer();

            // Сохраняем флаг в SharedPreferences, чтобы помнить, что таймер был установлен
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(TIMER_SET_KEY, true);
            editor.apply();
        }

    }

    private void clearDataIfFirstRun() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasRunBefore = sharedPreferences.getBoolean("hasRunBefore", false);

        if (!hasRunBefore) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(KEY_AUTH_TOKEN);
            editor.putBoolean("hasRunBefore", true);
            editor.apply();

            // Очистка данных, связанных с таймером
            SharedPreferences workInfoPreferences = getSharedPreferences(PREFS_NAME1, Context.MODE_PRIVATE);
            SharedPreferences.Editor workInfoEditor = workInfoPreferences.edit();
            workInfoEditor.clear();
            workInfoEditor.apply();
        }
    }

    // Функция для проверки, авторизован ли пользователь
    private boolean isUserAuthenticated() {
        // Используем SharedPreferences для хранения статуса авторизации пользователя
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString(KEY_AUTH_TOKEN, null);

        if (authToken != null) {
            HTTPRequest request = new HTTPRequest(MainActivity.this);

            Callback callback = new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Обработка ошибки запроса
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        // Обработка успешного ответа
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            // Выполняем действия с полученным JSON объектом
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else if (response.code() == 401) {
                        // Если получен код 401, очищаем токен и перенаправляем на LoginActivity
                        clearAuthTokenAndRedirect();
                    }
                }
            };
            request.profile(authToken, callback);

            // Если токен существует и не равен null, считаем пользователя авторизованным
            return authToken != null && !authToken.isEmpty();
        }
        return false;
    }

    private void clearAuthTokenAndRedirect() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_AUTH_TOKEN);
        editor.apply();

        // Очистка данных, связанных с таймером
        SharedPreferences workInfoPreferences = getSharedPreferences(PREFS_NAME1, Context.MODE_PRIVATE);
        SharedPreferences.Editor workInfoEditor = workInfoPreferences.edit();
        workInfoEditor.clear();
        workInfoEditor.apply();

        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void setTimer() {
        // Сохраняем время начала работы и задержку в SharedPreferences
        long startTimeMillis = System.currentTimeMillis();
        long delayDurationMillis = TimeUnit.DAYS.toMillis(7); // Задержка в 7 дней

        saveWorkInfo(startTimeMillis, delayDurationMillis);

        // Создаем запрос на одноразовую работу с задержкой в 7 дней
        OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInitialDelay(7, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(getApplicationContext()).enqueue(oneTimeWorkRequest);
    }

    private void saveWorkInfo(long startTimeMillis, long delayDurationMillis) {
        // Сохраняем информацию в SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME1, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(KEY_START_TIME, startTimeMillis);
        editor.putLong(KEY_DELAY_DURATION, delayDurationMillis);
        editor.apply();
    }
}
