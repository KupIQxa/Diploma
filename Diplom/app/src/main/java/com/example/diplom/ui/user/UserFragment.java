package com.example.diplom.ui.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.diplom.HTTPRequest;
import com.example.diplom.R;
import com.example.diplom.databinding.FragmentUserBinding;
import com.example.diplom.SettingsActivity;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;

import org.json.JSONException;
import org.json.JSONObject;

public class UserFragment extends Fragment {

    private ImageButton settingsButton;

    private FragmentUserBinding binding;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    private ImageView imageView;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Создайте экземпляр UserViewModelFactory
        UserViewModelFactory factory = new UserViewModelFactory(requireContext());

        // Используйте ViewModelProvider с фабрикой для создания UserViewModel
        UserViewModel userViewModel =
                new ViewModelProvider(this, factory).get(UserViewModel.class);

        binding = FragmentUserBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        TextView fullNameTextView = binding.tvFullName;
        TextView emailTextView = binding.tvEmail;
        Button logoutButton = binding.btnLogout;
        imageView = binding.profile;
        loadSavedImage();
        settingsButton = binding.ibSettings;

        // Установите обработчик кликов
        settingsButton.setOnClickListener(v -> {
            // Создайте Intent для перехода к новой активности
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            // Запустите новую активность
            startActivity(intent);
        });

        // Установка обработчика нажатия для кнопки выхода из профиля
        logoutButton.setOnClickListener(view -> {
            // Здесь вы можете реализовать логику выхода из профиля
            SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("profile_image_uri");
            editor.apply();
            userViewModel.logout();
            // Можно добавить переход на экран входа или другую логику после выхода
        });

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGallery();  // Открытие галереи для выбора изображения
                    } else {
                        // Обработка случая, если разрешение не предоставлено
                        // Можно показать уведомление или оповестить пользователя
                    }
                }
        );

        // Инициализация ActivityResultLauncher для выбора изображения из галереи
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        // Получение URI выбранного изображения
                        Uri selectedImageUri = result.getData().getData();

                        float density = getResources().getDisplayMetrics().density;
                        int pixels = (int) (200 * density + 0.5f);

                        // Используем Glide для загрузки изображения в ImageView с обрезкой в форме круга
                        Glide.with(this)
                                .load(selectedImageUri)
                                .transform(new CircleCrop()) // Используем круговое обрезание
                                .override(pixels, pixels) // Установите желаемый размер для изображения
                                .into(imageView);

                        // Сохранение URI выбранного изображения
                        saveImageUri(selectedImageUri);
                    }
                }
        );

        // Установка обработчика нажатия для ImageView
        imageView.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Запуск запроса разрешения на чтение из хранилища
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                // Открытие галереи для выбора изображения
                openGallery();
            }
        });

        userViewModel.getUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                Context context = requireContext();

                SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);

                String authToken = sharedPreferences.getString("auth_token", null);

                if (authToken != null) {
                    HTTPRequest request = new HTTPRequest(getContext());


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
                                // Получите тело ответа
                                String responseBody = response.body().string();
                                // Создайте объект JSONObject из строки JSON
                                try {
                                    JSONObject jsonObject = new JSONObject(responseBody);
                                    Log.d("TAG", "ответ: " + responseBody);
                                    // Извлекайте данные из JSON-объекта
                                    String encodedFullName = jsonObject.getString("full_name");
                                    // Декодируйте строку full_name в UTF-8
                                    String fullName = new String(encodedFullName.getBytes(), "UTF-8");
                                    String email = jsonObject.getString("email");
                                    // Вы можете извлечь другие данные из JSON-объекта в зависимости от его структуры
                                    Log.d("TAG", "имя: " + fullName);
                                    Log.d("TAG", "почта: " + email);
                                    // Обработайте извлеченные данные
                                    // Например, вы можете обновить интерфейс пользователя с помощью данных
                                    getActivity().runOnUiThread(() -> {
                                        fullNameTextView.setText(fullName);
                                        emailTextView.setText(email);
                                    });

                                } catch (JSONException e) {
                                    // Обработка ошибки парсинга JSON
                                    e.printStackTrace();
                                }
                            } else {
                                // Обработка неуспешного ответа
                            }
                        }
                    };
                    request.profile(authToken, callback);

                } else {
                    // Обработка случая, когда токен равен null (например, показать сообщение об ошибке)
                    // Например, можно показать уведомление или предпринять другие действия
                }
            }
        });

        return root;
    }


    private void openGallery() {
        // Создание интента для открытия галереи
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Установка типа данных, чтобы фильтровать только изображения
        intent.setType("image/*");
        // Запуск ActivityResultLauncher для выбора изображения из галереи
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Bitmap editImage(Context context, Uri imageUri, int targetWidth, int targetHeight) {
        try {
            // Получите изображение из URI
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);

            // Редактирование изображения (изменение размера в этом примере)
            Bitmap editedBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true);

            return editedBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Обработайте исключение, если возникли проблемы при чтении изображения
        }
    }

    private void saveImageUri(Uri image) {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile_image_uri", image.toString());
        editor.apply();
    }

    private void loadSavedImage() {
        // Получаем сохраненные данные из SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String savedUriString = sharedPreferences.getString("profile_image_uri", null);

        // Проверяем, есть ли сохраненный URI
        if (savedUriString != null) {
            Uri savedImageUri = Uri.parse(savedUriString);

            float density = getResources().getDisplayMetrics().density;
            int pixels = (int) (200 * density + 0.5f);

            // Используем Glide для загрузки изображения в ImageView с обрезкой в форме круга
            Glide.with(this)
                    .load(savedImageUri)
                    .transform(new CircleCrop()) // Используем круговое обрезание
                    .override(pixels, pixels) // Установите желаемый размер для изображения
                    .into(imageView);
        } else {
            // Если URI не найден, можно установить изображение по умолчанию или оставить пустым
            imageView.setImageResource(R.drawable.profile); // Укажите ресурс по умолчанию
        }
    }
}