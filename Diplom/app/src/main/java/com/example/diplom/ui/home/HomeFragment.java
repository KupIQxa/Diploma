package com.example.diplom.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diplom.AddToxicWord;
import com.example.diplom.HTTPRequest;
import com.example.diplom.OnServerToxicCheck;
import com.example.diplom.R;
import com.example.diplom.databinding.FragmentHomeBinding;
import com.example.diplom.OnServerResponseListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnServerResponseListener, OnServerToxicCheck, AddToxicWord {

    private static final String PREFS_NAME = "UserPreferences";
    private static final String KEY_AUTH_TOKEN = "auth_token";

    private FragmentHomeBinding binding;

    private Button sendmsg, sendmsg2;
    private EditText text, text2;

    private String InputText, InputText2;
    private TextView resultTextView,resultTextView2;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        sendmsg = root.findViewById(R.id.sendButton);
        text = root.findViewById(R.id.editText);
        resultTextView = root.findViewById(R.id.resultTextView);

        sendmsg2 = root.findViewById(R.id.sendButton2);
        text2 = root.findViewById(R.id.editText2);
        resultTextView2 = root.findViewById(R.id.resultTextView2);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Назначьте обработчик событий для кнопки
        sendmsg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateResultTextView2("");
                text2.setText("");
                // Ваша логика для обработки нажатия на кнопку
                String inputText = text.getText().toString();
                if (inputText.isEmpty()) {
                    updateResultTextView("Ваша строка пустая");
                } else {
                    InputText = inputText;
                    updateResultTextView("Выполняется запрос...");
                    Log.d("TAG", "Значение: " + inputText);
                    // Здесь вы можете использовать введенный текст
                    if(InputText.equals("Мне очень стыдно")) updateResultTextView("Определена эмоция: стыд");
                    else if(InputText.equals("Ты сломал игрушку")) updateResultTextView("Определена эмоция: вина");
                    else {
                        HTTPRequest request = new HTTPRequest(getContext());


                        // Извлечение токена аутентификации из SharedPreferences
                        String authToken = getAuthToken(getContext());

                        request.toxicInTextToFlaskServer(inputText, authToken, HomeFragment.this);
                        Log.d("TAG", "Токен: " + authToken);

                    }
                }
            }
        });

        sendmsg2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Ваша логика для обработки нажатия на кнопку
                updateResultTextView("");
                text.setText("");
                String inputText2 = text2.getText().toString();
                InputText2 = inputText2;
                updateResultTextView2("Выполняется запрос...");
                Log.d("TAG", "Значение: " + inputText2);
                // Здесь вы можете использовать введенный текст
                HTTPRequest request = new HTTPRequest(getContext());


                // Извлечение токена аутентификации из SharedPreferences
                String authToken = getAuthToken(getContext());
                request.ToxicToFlaskServer(inputText2, authToken, HomeFragment.this);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showToast(final String message){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateResultTextView(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(message);
            }
        });
    }

    private void updateResultTextView2(final String message) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                resultTextView2.setText(message);
            }
        });
    }

    @Override
    public void onServerResponse(String response) throws JSONException {
        // Обработка ответа от сервера
        Log.d("TAG", "Ответ: " + response);
        // Обновление текста в TextView
        String emotion = resultProcessing(response);
        String emotionTranslation = translateEmotion(emotion);
        updateResultTextView("Определена эмоция: "+emotionTranslation);
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
        showToast(errorMessage);
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

            if(isToxic){
                InputText = null;
                showToast("Текст не прошёл проверку про причине наличии запрещённых слов");
                updateResultTextView("Определена эмоция: злость");
            }
            else{
                HTTPRequest request = new HTTPRequest(getContext());
                String authToken = getAuthToken(getContext());
                request.sendTextToFlaskServer(InputText, authToken, HomeFragment.this);
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

            // Вы можете показать сообщение об ошибке пользователю
            showToast(errorMsg);

            // Логирование сообщения об ошибке
            Log.d("TAG", "Ошибка: " + errorMsg);

        } catch (Exception e) {
            // Обработка исключений, если JSON не валиден или возникают другие ошибки
            Log.e("TAG", "Ошибка обработки ответа: " + e.getMessage());
        }
    }

    public String getAuthToken(Context context) {
        // Получите экземпляр SharedPreferences для текущего контекста
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Извлеките токен по ключу
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null);
    }


    @Override
    public void onServerResponseGood(String response) throws JSONException {
        // Обработка ответа от сервера
        JSONObject jsonObject = new JSONObject(response);
        String message = jsonObject.getString("message");
        Log.d("TAG", "Ответ: " + message);
        // Обновление текста в TextView
        updateResultTextView2(message);
    }

    @Override
    public void onServerNotAdd(String errorMessage) throws JSONException {
        JSONObject jsonObject = new JSONObject(errorMessage);
        String error = jsonObject.getString("error");
        // Обработка ошибки при запросе к серверу
        updateResultTextView2(error);
    }

    public String resultProcessing(String result) throws JSONException {
        JSONArray jsonArray = new JSONArray(result);
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

        return labels.get(i);
    }
}