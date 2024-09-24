package com.example.diplom;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences tempPreferences;
    private SharedPreferences.Editor tempEditor;
    private static final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Временные настройки для изменений
        tempPreferences = getSharedPreferences("temp_preferences", MODE_PRIVATE);
        tempEditor = tempPreferences.edit();

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings_container, new SettingsFragment())
                    .commit();
        }

        // Настройте слушатели для кнопок
        Button saveButton = findViewById(R.id.btn_save_settings);
        saveButton.setOnClickListener(v -> confirmSettings());

        Button cancelButton = findViewById(R.id.btn_cancel_settings);
        cancelButton.setOnClickListener(v -> {
            // Очистите временные настройки
            tempEditor.clear().apply();
            Log.d(TAG, "Временные настройки очищены");
            finish();
        });
    }

    private void confirmSettings() {
        new AlertDialog.Builder(this)
                .setTitle("Подтвердить настройки")
                .setMessage("Вы хотите сохранить изменения?")
                .setPositiveButton("Да", (dialog, which) -> {
                    SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor defaultEditor = defaultPreferences.edit();

                    // Сохраните временные настройки в постоянные
                    for (String key : tempPreferences.getAll().keySet()) {
                        Object value = tempPreferences.getAll().get(key);

                        if (value instanceof String) {
                            defaultEditor.putString(key, (String) value);
                        } else if (value instanceof Boolean) {
                            defaultEditor.putBoolean(key, (Boolean) value);
                        } else if (value instanceof Integer) {
                            defaultEditor.putInt(key, (Integer) value);
                        } else if (value instanceof Float) {
                            defaultEditor.putFloat(key, (Float) value);
                        } else if (value instanceof Long) {
                            defaultEditor.putLong(key, (Long) value);
                        }

                        Log.d(TAG, "Сохранение настройки: " + key + " = " + value);
                    }
                    defaultEditor.apply();
                    Log.d(TAG, "Настройки сохранены");
                    finish();
                })
                .setNegativeButton("Нет", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Обработка результата разрешений
        if (requestCode == 1) {
            Log.d(TAG, "Результат разрешения для READ_SMS");
            // Результат для чтения SMS
        } else if (requestCode == 2) {
            Log.d(TAG, "Результат разрешения для POST_NOTIFICATIONS");
            // Результат для отправки уведомлений
        } else if (requestCode == 3) {
            Log.d(TAG, "Результат разрешения для READ_CONTACTS");
            // Результат для доступа к контактам
        } else if (requestCode == 4) {
            Log.d(TAG, "Результат разрешения для READ_EXTERNAL_STORAGE/WRITE_EXTERNAL_STORAGE");
            // Результат для доступа к файлам и медиаконтенту
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements OnServerResponseListener {
        private static final String TAG = "SettingsFragment";

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);

            Preference readSmsPermission = findPreference("read_sms_permission");
            Preference postNotificationPermission = findPreference("post_notification_permission");
            Preference readNotificationPermission = findPreference("read_notification_permission");
            Preference contactsPermission = findPreference("contacts_permission");
            Preference filesPermission = findPreference("files_permission");
            Preference deleteServerData = findPreference("delete_server_data");
            Preference sendDemoEmail = findPreference("send_demo_email");
            Preference clearBlockedSenders = findPreference("clear_blocked_senders");
            SwitchPreferenceCompat deleteDataOnTimerPref = findPreference("delete_data_on_timer");

            if (readSmsPermission != null) {
                readSmsPermission.setOnPreferenceClickListener(preference -> {
                    requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, 1);
                    Log.d(TAG, "Запрос разрешений RECEIVE_SMS и READ_SMS");
                    return true;
                });
            }

            if (postNotificationPermission != null) {
                postNotificationPermission.setOnPreferenceClickListener(preference -> {
                    // Запрос разрешения на отправку уведомлений
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
                    Log.d(TAG, "Запрос разрешения POST_NOTIFICATIONS");

                    // Переход в настройки уведомлений приложения
                    Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getActivity().getPackageName());
                    startActivity(intent);
                    Log.d(TAG, "Переход в настройки уведомлений приложения");
                    return true;
                });
            }

            if (readNotificationPermission != null) {
                readNotificationPermission.setOnPreferenceClickListener(preference -> {
                    // Откроем настройки для включения службы уведомлений
                    Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                    startActivity(intent);
                    Log.d(TAG, "Открытие настроек для службы уведомлений");
                    return true;
                });
            }

            if (contactsPermission != null) {
                contactsPermission.setOnPreferenceClickListener(preference -> {
                    requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, 3);
                    Log.d(TAG, "Запрос разрешения READ_CONTACTS");
                    return true;
                });
            }

            if (filesPermission != null) {
                filesPermission.setOnPreferenceClickListener(preference -> {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                    Log.d(TAG, "Запрос разрешений READ_EXTERNAL_STORAGE и WRITE_EXTERNAL_STORAGE");
                    return true;
                });
            }

            EditTextPreference urlPreference = findPreference("url_preference");
            if (urlPreference != null) {
                urlPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    String newUrl = (String) newValue;
                    // Сохраняем новое значение URL во временные настройки
                    SharedPreferences tempPreferences = getActivity().getSharedPreferences("temp_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor tempEditor = tempPreferences.edit();
                    tempEditor.putString("url_preference", newUrl);
                    tempEditor.apply();
                    Log.d(TAG, "Временная настройка обновлена: url_preference = " + newUrl);
                    return true;
                });

                // Отображение текущего значения из временных настроек, если оно есть
                SharedPreferences tempPreferences = getActivity().getSharedPreferences("temp_preferences", MODE_PRIVATE);
                String tempUrl = tempPreferences.getString("url_preference", null);
                if (tempUrl != null) {
                    urlPreference.setText(tempUrl);
                    Log.d(TAG, "Отображение временного URL: " + tempUrl);
                } else {
                    // Отображение текущего значения из основного хранилища, если временное значение отсутствует
                    SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    String defaultUrl = defaultPreferences.getString("url_preference", "");
                    urlPreference.setText(defaultUrl);
                    Log.d(TAG, "Отображение основного URL: " + defaultUrl);
                }
            }

            SwitchPreferenceCompat deleteNotificationsPref = findPreference("delete_notifications");
            SwitchPreferenceCompat blockSendersPref = findPreference("block_senders");

            if (deleteNotificationsPref != null) {
                deleteNotificationsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (Boolean) newValue;
                    // Сохраняем временные настройки для удаления уведомлений
                    SharedPreferences tempPreferences = getActivity().getSharedPreferences("temp_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor tempEditor = tempPreferences.edit();
                    tempEditor.putBoolean("delete_notifications", isEnabled);
                    tempEditor.apply();
                    Log.d(TAG, "Временная настройка обновлена: delete_notifications = " + isEnabled);
                    return true;
                });
            }

            if (blockSendersPref != null) {
                blockSendersPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (Boolean) newValue;
                    // Сохраняем временные настройки для блокировки отправителей
                    SharedPreferences tempPreferences = getActivity().getSharedPreferences("temp_preferences", MODE_PRIVATE);
                    SharedPreferences.Editor tempEditor = tempPreferences.edit();
                    tempEditor.putBoolean("block_senders", isEnabled);
                    tempEditor.apply();
                    Log.d(TAG, "Временная настройка обновлена: block_senders = " + isEnabled);
                    return true;
                });
            }

            if (deleteServerData != null) {
                deleteServerData.setOnPreferenceClickListener(preference -> {
                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                    String authToken = sharedPreferences.getString("auth_token", null);

                    if (authToken != null) {
                        HTTPRequest request = new HTTPRequest(requireContext());
                        request.deleteMessageFromFlaskServer(authToken, SettingsFragment.this);
                        Log.d(TAG, "Удаление данных с сервера");
                    } else {
                        Log.d(TAG, "Токен авторизации не найден");
                        //Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }

            if (sendDemoEmail != null) {
                sendDemoEmail.setOnPreferenceClickListener(preference -> {
                    // Запуск MyWorker для отправки письма
                    OneTimeWorkRequest sendEmailWorkRequest = new OneTimeWorkRequest.Builder(OneSendToEmail.class)
                            .setInitialDelay(10, TimeUnit.SECONDS) // Установите необходимую задержку
                            .build();

                    WorkManager.getInstance(requireContext()).enqueue(sendEmailWorkRequest);

                    Log.d(TAG, "Работа по отправке письма запланирована");
                    return true;
                });
            }

            if (clearBlockedSenders != null) {
                clearBlockedSenders.setOnPreferenceClickListener(preference -> {
                    // Очистка всех заблокированных отправителей
                    SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
                    String authToken = sharedPreferences.getString("auth_token", null);

                    if (authToken == null) {
                        // Обработка случая, когда токен отсутствует
                        Log.d("TAG", "Токен отсутствует");
                        return false;
                    }

                    HTTPRequest request = new HTTPRequest(requireContext());
                    Callback callback = new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            // Обработка ошибки запроса
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "Все заблокированные отправители очищены");
                                Toast.makeText(getActivity(), "Все заблокированные отправители очищены", Toast.LENGTH_SHORT).show();
                            }
                        }
                    };
                    request.unblockSenders(authToken, callback);
                    return true;
                });
            }

            if (deleteDataOnTimerPref != null) {
                deleteDataOnTimerPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean isEnabled = (Boolean) newValue;
                    // Сохраняем временные настройки для удаления данных по истечении таймера
                    SharedPreferences tempPreferences = getActivity().getSharedPreferences("temp_preferences", Context.MODE_PRIVATE);
                    SharedPreferences.Editor tempEditor = tempPreferences.edit();
                    tempEditor.putBoolean("delete_data_on_timer", isEnabled);
                    tempEditor.apply();

                    Log.d(TAG, "Временная настройка обновлена: delete_data_on_timer = " + isEnabled);
                    return true;
                });
            }
        }

        @Override
        public void onServerResponse(String response) throws JSONException {
            Log.d(TAG, "Server response: " + response);
            // Обработка успешного ответа сервера
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Данные успешно удалены", Toast.LENGTH_SHORT).show()
            );
        }

        @Override
        public void onServerError(String errorMessage) {
            Log.d(TAG, "Server error: " + errorMessage);
            // Обработка ошибки сервера
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), "Ошибка удаления данных", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
