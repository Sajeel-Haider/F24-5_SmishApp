package com.example.f24_5;

public class Chat {
    private String name;
    private String message;
    private String time;
    private String source;

    public Chat(String name, String message, String time, String source) {
        this.name = name;
        this.message = message;
        this.time = time;
        this.source = source;
    }

    public String getName() { return name; }
    public String getMessage() { return message; }
    public String getTime() { return time; }
    public String getSource() { return source; }
}

