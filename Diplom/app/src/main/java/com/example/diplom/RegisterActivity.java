package com.example.diplom;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class RegisterActivity extends AppCompatActivity implements OnServerResponseListener {

    private EditText editTextFullName;
    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonRegister;
    private TextView textViewLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Привязываем виджеты к соответствующим переменным
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        // Добавление обработчика нажатия на кнопку регистрации
        buttonRegister.setOnClickListener(view -> {
            String fullName = editTextFullName.getText().toString();
            String email = editTextEmail.getText().toString();
            String password = editTextPassword.getText().toString();

            // Проверка на пустые строки
            if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showToast("Пожалуйста, заполните все поля.");
            } else {
                HTTPRequest request = new HTTPRequest(this);
                request.sendRegisterRequest(fullName, email, password, RegisterActivity.this);
                // Отправляем запрос на сервер
            }
        });

        // Обработчик для текстового поля для перехода к странице входа
        textViewLogin.setOnClickListener(view -> {
            finish();
        });
    }

    @Override
    public void onServerResponse(String response) {
        // Обработка ответа от сервера
        Log.d("TAG", "Ответ: " + response);
        showToast(response);
    }

    @Override
    public void onServerError(String errorMessage) {
        // Обработка ошибки при запросе к серверу
        Log.d("TAG", "Ошибка: " + errorMessage);
        showToast(errorMessage);
    }

    private void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
