package com.example.diplom.ui.statistics;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.diplom.HTTPRequest;
import com.example.diplom.R;
import com.example.diplom.databinding.FragmentStatisticsBinding;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class StatisticsFragment extends Fragment {
    private PieChart pieChart;
    private FragmentStatisticsBinding binding;

    private List<String> Senders = new ArrayList<>();

    private List<String> Emotions = new ArrayList<>();

    private static final String PREFS_NAME1 = "WorkInfoPrefs";
    private static final String KEY_START_TIME = "startTime";
    private static final String KEY_DELAY_DURATION = "delayDuration";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        StatisticsViewModel statisticsViewModel =
                new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        Senders.clear();
        Emotions.clear();

        Context context = requireContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);
        if (authToken == null) {
            // Если пользователь не авторизован, скрываем текст перед диаграммой
            binding.chartTitleTextView.setVisibility(View.GONE);
            binding.TextTimer.setVisibility(View.GONE);
            binding.descriptionTextView.setVisibility(View.GONE);
        } else {
            // Если пользователь авторизован, отображаем текст перед диаграммой
            binding.chartTitleTextView.setVisibility(View.VISIBLE);
            binding.TextTimer.setVisibility(View.VISIBLE);
            binding.descriptionTextView.setVisibility(View.VISIBLE);
            long timeInSeconds = getTimeRemaining();
            String remainingTime = formatRemainingTime(timeInSeconds);
            binding.chartTitleTextView.setText(remainingTime);
        }

        pieChart = binding.pieChart;
        pieChart.setNoDataTextColor(Color.rgb(153, 102, 255)); // Фиолетовый
        // Вызовите метод загрузки данных при создании фрагмента
        pieChart.setNoDataText("Нет данных для статистики");

        loadSmsDataFromServer();

        requireActivity().runOnUiThread(() -> {
            createPieEntries(Emotions);
        });

        return root;
    }

    private String formatRemainingTime(long millis) {
        long days = TimeUnit.SECONDS.toDays(millis);
        long hours = TimeUnit.SECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(millis) % 60;

        return String.format(Locale.getDefault(), "%d дней, %02d часов, %02d минут", days, hours, minutes);
    }

    public long getTimeRemaining() {
        // Извлекаем сохраненную информацию из SharedPreferences
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME1, Context.MODE_PRIVATE);
        long startTimeMillis = sharedPreferences.getLong(KEY_START_TIME, 0);
        long delayDurationMillis = sharedPreferences.getLong(KEY_DELAY_DURATION, 0);

        // Вычисляем время, когда работа должна завершиться
        long endTimeMillis = startTimeMillis + delayDurationMillis;
        // Вычисляем оставшееся время до завершения работы в секундах
        long currentTimeMillis = System.currentTimeMillis();
        long timeRemainingSeconds = TimeUnit.MILLISECONDS.toSeconds(endTimeMillis - currentTimeMillis);

        // Возвращаем оставшееся время в секундах
        return Math.max(0, timeRemainingSeconds);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupPieChart(List<PieEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            // Нет данных для отображения, выход из метода
            Log.e("PieChart Error", "Нет данных для отображения.");
            return;
        }

        // Создаем набор данных PieDataSet
        PieDataSet dataSet = new PieDataSet(entries, "Эмоции");

        // Создаем список эмоций и соответствующих цветов
        Map<String, Integer> emotionColorMapRussian = new HashMap<>();
        emotionColorMapRussian.put("Радость", Color.rgb(255, 205, 86)); // Желтый
        emotionColorMapRussian.put("Нейтральность", Color.rgb(54, 162, 235)); // Синий
        emotionColorMapRussian.put("Гнев", Color.rgb(255, 99, 132)); // Красный
        emotionColorMapRussian.put("Удивление", Color.rgb(255, 159, 64)); // Оранжевый
        emotionColorMapRussian.put("Энтузиазм", Color.rgb(153, 102, 255)); // Фиолетовый
        emotionColorMapRussian.put("Грусть", Color.rgb(75, 192, 192)); // Зеленый
        emotionColorMapRussian.put("Страх", Color.rgb(201, 203, 207)); // Серый
        emotionColorMapRussian.put("Отвращение", Color.rgb(99, 255, 132)); // Лаймовый
        emotionColorMapRussian.put("Стыд", Color.rgb(255, 140, 0)); // Темно-оранжевый
        emotionColorMapRussian.put("Вина", Color.rgb(0, 191, 255)); // Голубой

        // Устанавливаем цвета в наборе данных на основе эмоций
        List<Integer> colors = new ArrayList<>();
        for (PieEntry entry : entries) {
            String emotion = entry.getLabel();
            if (emotionColorMapRussian.containsKey(emotion)) {
                colors.add(emotionColorMapRussian.get(emotion));
            } else {
                colors.add(Color.GRAY); // Цвет по умолчанию для нераспознанных эмоций
            }
        }

        dataSet.setColors(colors);

        // Настройка внешнего вида набора данных
        dataSet.setValueTextSize(14f);

        // Настройка форматирования значений на диаграмме
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%d%%", Math.round(value));
            }
        });

        // Создаем объект PieData
        PieData pieData = new PieData(dataSet);

        // Устанавливаем данные в круговую диаграмму
        try {
            pieChart.setData(pieData);
        } catch (Exception e) {
            Log.e("PieChart Error", "Ошибка при установке данных для круговой диаграммы.", e);
        }

        // Настройка параметров диаграммы
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setEntryLabelColor(Color.BLACK);
        pieChart.setEntryLabelTextSize(14f);
        pieChart.setScaleX(0.9f);
        pieChart.setScaleY(0.9f);
        // Настройка легенды
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL); // Изменено на горизонтальное
        legend.setTextSize(10f);

        // Обновление диаграммы
        try {
            pieChart.invalidate();
        } catch (Exception e) {
            Log.e("PieChart Error", "Ошибка при обновлении круговой диаграммы.", e);
        }
    }


    private void createPieEntries(List<String> emotions) {
        Map<String, Integer> emotionCount = new HashMap<>();

        // Подсчитываем количество каждой эмоции
        for (String emotion : emotions) {
            if (emotionCount.containsKey(emotion)) {
                emotionCount.put(emotion, emotionCount.get(emotion) + 1);
            } else {
                emotionCount.put(emotion, 1);
            }
        }

        // Создаем словарь для перевода эмоций с английского на русский язык
        Map<String, String> emotionTranslationMap = new HashMap<>();
        emotionTranslationMap.put("joy", "Радость");
        emotionTranslationMap.put("neutral", "Нейтральность");
        emotionTranslationMap.put("anger", "Гнев");
        emotionTranslationMap.put("surprise", "Удивление");
        emotionTranslationMap.put("enthusiasm", "Энтузиазм");
        emotionTranslationMap.put("sadness", "Грусть");
        emotionTranslationMap.put("fear", "Страх");
        emotionTranslationMap.put("disgust", "Отвращение");
        emotionTranslationMap.put("shame", "Стыд");
        emotionTranslationMap.put("guilt", "Вина");

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : emotionCount.entrySet()) {
            // Получаем русскую метку для текущей эмоции
            String emotionEnglish = entry.getKey();
            String emotionRussian = emotionTranslationMap.get(emotionEnglish);

            // Если нет русского перевода, используем английскую метку по умолчанию
            if (emotionRussian == null) {
                emotionRussian = emotionEnglish;
            }

            // Добавляем новую запись в список для круговой диаграммы
            entries.add(new PieEntry(entry.getValue(), emotionRussian));
        }

        // Настроим круговую диаграмму
        requireActivity().runOnUiThread(() -> setupPieChart(entries)); // Обновление UI только в основном потоке
    }


    private void loadSmsDataFromServer() {
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
                        String responseBody = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(responseBody);
                            Log.d("TAG", "Ответ: " + responseBody);

                            JSONArray notificationsArray = jsonObject.getJSONArray("notifications");

                            List<String> newEmotions = new ArrayList<>();
                            List<String> newSenders = new ArrayList<>();

                            for (int i = 0; i < notificationsArray.length(); i++) {
                                JSONObject notificationObject = notificationsArray.getJSONObject(i);
                                String encodedAuthor = notificationObject.getString("author");
                                String author = new String(encodedAuthor.getBytes(), "UTF-8");

                                String encodedText = notificationObject.getString("text");
                                String text = new String(encodedText.getBytes(), "UTF-8");
                                int id = notificationObject.getInt("id");

                                String emotion = notificationObject.getString("emotion");
                                String createdAt = notificationObject.getString("created_at");

                                newSenders.add(author);
                                newEmotions.add(emotion);

                                Log.d("TAG", "ID: " + id);
                                Log.d("TAG", "Автор: " + author);
                                Log.d("TAG", "Текст: " + text);
                                Log.d("TAG", "Эмоция: " + emotion);
                                Log.d("TAG", "Создано: " + createdAt);
                            }

                            requireActivity().runOnUiThread(() -> {
                                Senders.addAll(newSenders);
                                Emotions.addAll(newEmotions);
                                createPieEntries(Emotions);
                                setStatistics();
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            request.ResultsFromFlaskServer(authToken, callback);
        }
    }

    private void setStatistics() {
        TextView descriptionTextView2 = requireView().findViewById(R.id.descriptionTextView2);

        // Создаем HashMap для подсчета количества повторений каждой комбинации отправителя и эмоции
        Map<String, Integer> statisticsMap = new HashMap<>();

        // Создаем словарь для перевода эмоций с английского на русский язык
        Map<String, String> emotionTranslationMap = new HashMap<>();
        emotionTranslationMap.put("joy", "Радость");
        emotionTranslationMap.put("neutral", "Нейтральность");
        emotionTranslationMap.put("anger", "Гнев");
        emotionTranslationMap.put("surprise", "Удивление");
        emotionTranslationMap.put("enthusiasm", "Энтузиазм");
        emotionTranslationMap.put("sadness", "Грусть");
        emotionTranslationMap.put("fear", "Страх");
        emotionTranslationMap.put("disgust", "Отвращение");
        emotionTranslationMap.put("shame", "Стыд");
        emotionTranslationMap.put("guilt", "Вина");

        // Заполняем HashMap
        for (int i = 0; i < Senders.size(); i++) {
            String emotionEnglish = Emotions.get(i);
            String emotionRussian = emotionTranslationMap.get(emotionEnglish);
            // Если нет русского перевода, используем английскую метку по умолчанию
            if (emotionRussian == null) {
                emotionRussian = emotionEnglish;
            }
            String senderAndEmotion = Senders.get(i) + ", " + emotionRussian;
            if (statisticsMap.containsKey(senderAndEmotion)) {
                statisticsMap.put(senderAndEmotion, statisticsMap.get(senderAndEmotion) + 1);
            } else {
                statisticsMap.put(senderAndEmotion, 1);
            }
        }

        // Создаем строку для вывода информации
        StringBuilder statisticsText = new StringBuilder();

        // Добавляем информацию из HashMap в строку
        for (Map.Entry<String, Integer> entry : statisticsMap.entrySet()) {
            String senderAndEmotion = entry.getKey();
            int count = entry.getValue();
            statisticsText.append("-Отправитель и эмоция: ").append(senderAndEmotion).append(";\n");
            statisticsText.append("-Количество повторений: ").append(count).append(".\n\n");
        }

        // Устанавливаем строку с информацией в текстовый элемент
        descriptionTextView2.setText(statisticsText.toString());
    }

}