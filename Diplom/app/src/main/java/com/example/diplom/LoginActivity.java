package com.example.diplom;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity implements OnServerResponseListener {
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonLogin;
    private TextView textViewRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.authorization);

        // Привязываем виджеты к соответствующим переменным
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonRegister);
        textViewRegister = findViewById(R.id.textViewRegister);

        // Добавление обработчика нажатия на кнопку входа
        buttonLogin.setOnClickListener(view -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Проверка на пустые строки
            if (email.isEmpty() || password.isEmpty()) {
                showToast("Пожалуйста, заполните все поля.");
            } else {
                HTTPRequest request = new HTTPRequest(this);
                request.login(email, password, LoginActivity.this);
            }
        });

        // Обработчик для перехода к активности регистрации
        textViewRegister.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onServerError(String errorMessage) {
        // Обработка ошибки при запросе к серверу
        Log.d("TAG", "Ошибка: " + errorMessage);
        showToast(errorMessage);
    }

    @Override
    public void onServerResponse(String response) {
        // Обработка ответа от сервера
        Log.d("TAG", "Ответ: " + response);
        showToast("Вы успешно авторизовались");
        saveAuthToken(response);

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void saveAuthToken(String token) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("auth_token", token);
        editor.apply();
    }
}
