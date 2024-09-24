package com.example.diplom.ui.chats;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diplom.ChatActivity;
import com.example.diplom.HTTPRequest;
import com.example.diplom.OnSenderClickListener;
import com.example.diplom.Sender;
import com.example.diplom.SenderListAdapter;
import com.example.diplom.databinding.FragmentChatsBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChatsFragment extends Fragment implements OnSenderClickListener {

    private FragmentChatsBinding binding;
    private List<Sender> senderList;
    private SenderListAdapter adapter;

    private static final String EXTRA_SENDER_NAME = "SENDER_NAME";
    private List<String> Senders = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ChatsViewModel chatsViewModel = new ViewModelProvider(this).get(ChatsViewModel.class);

        binding = FragmentChatsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        senderList = new ArrayList<>();
        adapter = new SenderListAdapter(senderList, this::onSenderClick);
        recyclerView.setAdapter(adapter);
        Senders.clear();

        Context context = requireContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

        if (authToken == null) {
            recyclerView.setVisibility(View.GONE); // Скрыть RecyclerView
        } else {
            recyclerView.setVisibility(View.VISIBLE); // Показать RecyclerView
            // Load senders from data source
            loadSmsDataFromServer();
        }

        return root;
    }

    private void loadSmsDataFromServer() {
        Context context = requireContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);
        String authToken = sharedPreferences.getString("auth_token", null);

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

                        // Используем HashSet для проверки уникальности
                        HashSet<String> uniqueSenders = new HashSet<>();

                        for (int i = 0; i < notificationsArray.length(); i++) {
                            JSONObject notificationObject = notificationsArray.getJSONObject(i);
                            String encodedAuthor = notificationObject.getString("author");
                            String author = new String(encodedAuthor.getBytes(), "UTF-8");

                            if (uniqueSenders.add(author)) { // добавляет только уникальные элементы
                                Sender sender = new Sender(author);
                                senderList.add(sender);
                                Log.d("TAG", "Автор: " + author);
                            }
                        }

                        requireActivity().runOnUiThread(() -> {
                            adapter.notifyDataSetChanged();
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        request.ResultsFromFlaskServer(authToken, callback);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onSenderClick(Sender sender) {
        // Обработка клика на отправителе, например, открытие новой страницы с сообщениями отправителя
        openSenderMessagesPage(sender);
    }

    private void openSenderMessagesPage(Sender sender) {
        Intent intent = new Intent(getActivity(), ChatActivity.class);

        // Передаем данные отправителя в активность чата
        intent.putExtra(EXTRA_SENDER_NAME, sender.getName());
        Log.d("TAG", "Автор1: " + sender.getName());
        // Запускаем активность чата
        startActivity(intent);
    }
}