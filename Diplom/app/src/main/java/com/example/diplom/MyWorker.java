package com.example.diplom;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

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

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.AxisPosition;
import org.apache.poi.xddf.usermodel.chart.ChartTypes;
import org.apache.poi.xddf.usermodel.chart.LegendPosition;
import org.apache.poi.xddf.usermodel.chart.XDDFCategoryAxis;
import org.apache.poi.xddf.usermodel.chart.XDDFChartData;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFDataSourcesFactory;
import org.apache.poi.xddf.usermodel.chart.XDDFNumericalDataSource;
import org.apache.poi.xddf.usermodel.chart.XDDFValueAxis;
import org.apache.poi.xssf.usermodel.XSSFChart;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;

import org.apache.poi.ss.usermodel.Workbook;

public class MyWorker extends Worker {

    private Context appContext;

    private String AuthToken;
    private String Email = "kikin-2015@mail.ru";
    private String Fullname = "";

    private List<Integer> Ids = new ArrayList<>();
    private List<String> Senders = new ArrayList<>();

    private List<String> Emotions = new ArrayList<>();
    private List<String> Texts = new ArrayList<>();
    private List<String> CreatedAts = new ArrayList<>();


    public MyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        appContext = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Result doWork() {
        // Отложенное выполнение создания уведомления через 10 секунд


        SharedPreferences sharedPreferences = appContext.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);
        AuthToken = authToken;
        if (AuthToken != null) {
            Ids.clear();
            Senders.clear();
            Emotions.clear();
            Texts.clear();
            CreatedAts.clear();

            //createNotification();
            fetchDataFromProfile();
            fetchDataFromMessages();
            if (!Fullname.isEmpty() && !Email.isEmpty()) {
                createExcelSheet();
                createNotification("На почту отправлен отчёт за неделю");

                SharedPreferences tempPreferences = appContext.getSharedPreferences("temp_preferences", Context.MODE_PRIVATE);
                boolean isDeleteDataEnabled = tempPreferences.getBoolean("delete_data_on_timer", false);

                if (isDeleteDataEnabled) {
                    // Логика для удаления данных
                    clearNotificationData();
                }

                //schedulePeriodicWork();
                return Result.success();
            } else return Result.failure();
        } else return Result.failure();
    }

    private void schedulePeriodicWork() {
        // Создаем периодический запрос работы с интервалом в 7 дней
        PeriodicWorkRequest periodicWorkRequest = new PeriodicWorkRequest.Builder(
                MyWorker.class,
                7,
                TimeUnit.DAYS
        ).build();

        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                MainActivity.WORK_TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWorkRequest
        );
    }

    private void createNotification(String msg) {
        // Получаем доступ к сервису уведомлений
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        // Проверяем, что сервис уведомлений доступен
        if (notificationManager == null) {
            Log.e("MyWorker", "NotificationManager is null");
            return;
        }

        // Создаем канал уведомлений для Android O и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("my_channel_id", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        // Создаем интент для запуска MyWorker
        Intent intent = new Intent(getApplicationContext(), MyWorker.class);
        PendingIntent pendingIntent = PendingIntent.getForegroundService(getApplicationContext(), 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Создаем уведомление
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "my_channel_id")
                .setSmallIcon(R.mipmap.ic_launcher) // Устанавливаем иконку уведомления
                .setContentTitle("Родительский смс-контроль") // Устанавливаем заголовок уведомления
                .setContentText("На почту отправлен отчёт за неделю") // Устанавливаем текст уведомления
                .setContentIntent(pendingIntent) // Устанавливаем действие при нажатии на уведомление
                .setAutoCancel(true); // Устанавливаем автоматическое закрытие уведомления при нажатии на него

        int notificationId = (int) System.currentTimeMillis(); // Используем текущее время в качестве идентификатора

        // Отправляем уведомление
        notificationManager.notify(notificationId, builder.build());
    }

    private void fetchDataFromProfile() {
        HTTPRequest request = new HTTPRequest(appContext);
        String data = request.fetchDataFromProfile(AuthToken);
        String parts[] = data.split(">");
        if (parts.length == 2) {
            Fullname = parts[0];
            Email = parts[1];
        }
    }

    private void fetchDataFromMessages() {
        HTTPRequest request = new HTTPRequest(appContext);
        String data = request.fetchDataFromMessages(AuthToken);
        try {
            JSONObject jsonObject = new JSONObject(data);
            Log.d("TAG", "Ответ: " + data);

            JSONArray notificationsArray = jsonObject.getJSONArray("notifications");

            List<String> newEmotions = new ArrayList<>();
            List<String> newSenders = new ArrayList<>();
            List<String> newTexts = new ArrayList<>();
            List<String> newCratedAts = new ArrayList<>();
            List<Integer> newIds = new ArrayList<>();
            for (int i = 0; i < notificationsArray.length(); i++) {
                JSONObject notificationObject = notificationsArray.getJSONObject(i);
                String encodedAuthor = notificationObject.getString("author");
                String author = new String(encodedAuthor.getBytes(), "UTF-8");

                String encodedText = notificationObject.getString("text");
                String text = new String(encodedText.getBytes(), "UTF-8");
                int id = notificationObject.getInt("id");

                String emotion = notificationObject.getString("emotion");
                String TranslateEmotion = translateEmotion(emotion);
                String createdAt = notificationObject.getString("created_at");

                newSenders.add(author);
                newEmotions.add(TranslateEmotion);
                newTexts.add(text);
                newCratedAts.add(createdAt);
                newIds.add(id);
                Log.d("TAG", "ID: " + id);
                Log.d("TAG", "Автор: " + author);
                Log.d("TAG", "Текст: " + text);
                Log.d("TAG", "Эмоция: " + emotion);
                Log.d("TAG", "Создано: " + createdAt);
            }
            Senders.addAll(newSenders);
            Emotions.addAll(newEmotions);
            Texts.addAll(newTexts);
            CreatedAts.addAll(newCratedAts);
            Ids.addAll(newIds);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
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

    private void createExcelSheet() {
        Workbook workbook = new XSSFWorkbook(); // Используем XSSFWorkbook для формата .xlsx

        // Создаем основной лист со статистикой
        Sheet sheet = workbook.createSheet("Статистика за неделю");

        // Заголовки таблицы
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("№");
        headerRow.createCell(1).setCellValue("Отправитель");
        headerRow.createCell(2).setCellValue("Текст");
        headerRow.createCell(3).setCellValue("Эмоция");
        headerRow.createCell(4).setCellValue("Дата создания");

        // Добавляем данные из списков в таблицу
        for (int i = 0; i < Ids.size(); i++) {
            Row row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(Senders.get(i));
            row.createCell(2).setCellValue(Texts.get(i));
            row.createCell(3).setCellValue(Emotions.get(i));
            row.createCell(4).setCellValue(CreatedAts.get(i));
        }

        // Создаем лист с диаграммой эмоций
        createEmotionChart(workbook);

        // Создаем лист с диаграммой отправителей
        createSenderChart(workbook);

        try {
            // Получаем текущую дату и время
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
            String timestamp = dateFormat.format(new Date());

            // Получаем доступ к внутреннему хранилищу приложения
            File directory = appContext.getFilesDir();

            // Создаем имя файла с датой создания
            String fileName = "Статистика сообщений " + timestamp + ".xlsx";

            // Создаем файл внутри директории приложения
            File file = new File(directory, fileName);

            // Создаем поток для записи данных в файл
            FileOutputStream fileOut = new FileOutputStream(file);

            // Сохраняем рабочую книгу Excel в файл
            workbook.write(fileOut);

            // Закрываем поток
            fileOut.close();
            workbook.close();
            Log.d("TAG", "Файл собран");
            sendExcelToEmail(file);
        } catch (IOException e) {
            e.printStackTrace();
            // В случае ошибки выводим сообщение
            Toast.makeText(appContext, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void createEmotionChart(Workbook workbook) {
        Sheet chartSheet = workbook.createSheet("Диаграмма эмоций");

        // Подсчитываем количество каждой эмоции
        Map<String, Integer> emotionCount = new HashMap<>();
        for (String emotion : Emotions) {
            emotionCount.put(emotion, emotionCount.getOrDefault(emotion, 0) + 1);
        }

        // Записываем данные на лист для диаграммы
        int rownum = 0;
        for (Map.Entry<String, Integer> entry : emotionCount.entrySet()) {
            Row row = chartSheet.createRow(rownum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // Создаем диаграмму
        Drawing<?> drawing = chartSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, rownum + 2, 10, rownum + 20);

        XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
        chart.setTitleText("Диаграмма эмоций");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Эмоции");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Количество");

        XDDFDataSource<String> emotions = XDDFDataSourcesFactory.fromStringCellRange((XSSFSheet) chartSheet, new CellRangeAddress(0, rownum - 1, 0, 0));
        XDDFNumericalDataSource<Double> counts = XDDFDataSourcesFactory.fromNumericCellRange((XSSFSheet) chartSheet, new CellRangeAddress(0, rownum - 1, 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(emotions, counts);
        series.setTitle("Эмоции", null);
        chart.plot(data);
    }

    private void createSenderChart(Workbook workbook) {
        Sheet chartSheet = workbook.createSheet("Диаграмма отправителей");

        // Подсчитываем количество каждого отправителя
        Map<String, Integer> senderCount = new HashMap<>();
        for (String sender : Senders) {
            senderCount.put(sender, senderCount.getOrDefault(sender, 0) + 1);
        }

        // Записываем данные на лист для диаграммы
        int rownum = 0;
        for (Map.Entry<String, Integer> entry : senderCount.entrySet()) {
            Row row = chartSheet.createRow(rownum++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // Создаем диаграмму
        Drawing<?> drawing = chartSheet.createDrawingPatriarch();
        ClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 0, rownum + 2, 10, rownum + 20);

        XSSFChart chart = ((XSSFDrawing) drawing).createChart(anchor);
        chart.setTitleText("Диаграмма отправителей");
        chart.setTitleOverlay(false);

        XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        bottomAxis.setTitle("Отправители");
        XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
        leftAxis.setTitle("Количество");

        XDDFDataSource<String> senders = XDDFDataSourcesFactory.fromStringCellRange((XSSFSheet) chartSheet, new CellRangeAddress(0, rownum - 1, 0, 0));
        XDDFNumericalDataSource<Double> counts = XDDFDataSourcesFactory.fromNumericCellRange((XSSFSheet) chartSheet, new CellRangeAddress(0, rownum - 1, 1, 1));

        XDDFChartData data = chart.createData(ChartTypes.BAR, bottomAxis, leftAxis);
        XDDFChartData.Series series = data.addSeries(senders, counts);
        series.setTitle("Отправители", null);
        chart.plot(data);
    }


    private void sendExcelToEmail(File file) {
        Log.d("TAG", "Отпраааааааавка");
        Log.d("TAG", Email);
        Log.d("TAG", Fullname);


        // Настройка параметров подключения SMTP сервера
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.yandex.ru");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");


        String email = "parentalcontrol58ru@yandex.ru";
        String password = "gevwgejdfqxzzhqx";

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        try {
            // Создание сообщения
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(Email));
            message.setSubject("Отчет о сообщениях за прошедшую неделю");

            // Создание тела сообщения
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText("Добрый день, " + Fullname + ", ваш отчет о сообщениях прикреплен.");

            // Создание вложения
            MimeBodyPart attachmentPart = new MimeBodyPart();
            DataSource source = new FileDataSource(file);
            attachmentPart.setDataHandler(new DataHandler(source));
            attachmentPart.setFileName(file.getName());

            // Создание контейнера для частей сообщения
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            multipart.addBodyPart(attachmentPart);

            // Установка контента сообщения
            message.setContent(multipart);

            // Отправка сообщения
            Transport.send(message);

            // Удаление файла после отправки
            file.delete();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void clearNotificationData() {
            HTTPRequest request = new HTTPRequest(appContext);
            boolean res = request.deleteMessageFromFlaskServerSync(AuthToken);
            if(res) createNotification("Данные успешно удалены");
            else createNotification("Данные из-за ошибки не удалены");
    }
}
