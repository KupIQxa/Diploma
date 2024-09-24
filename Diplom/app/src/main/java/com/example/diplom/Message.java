package com.example.diplom;

public class Message {
    private String content;
    private String sender;
    private String dats;

    private String emotion;

    public Message(String content, String sender, String data, String emotion) {
        this.content = content;
        this.sender = sender;
        this.dats = data;
        this.emotion = emotion;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public String getDats() {
        return dats;
    }

    public String getEmotion(){return emotion;}
}
