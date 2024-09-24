package com.example.diplom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SenderListAdapter extends RecyclerView.Adapter<SenderListAdapter.ViewHolder> {

    private List<Sender> senderList;
    private OnSenderClickListener listener;

    public SenderListAdapter(List<Sender> senderList, OnSenderClickListener listener) {
        this.senderList = senderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sender_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Sender sender = senderList.get(position);
        holder.senderNameTextView.setText(sender.getName());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSenderClick(sender);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return senderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView senderNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            senderNameTextView = itemView.findViewById(R.id.senderNameTextView);
        }
    }
}