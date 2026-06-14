package com.YOUNES.AI;

public class MessageModel {

    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT  = 2;
    public static final int TYPE_LOADING = 3;

    private String message;
    private int type;
    private long timestamp;

    public MessageModel(String message, int type) {
        this.message   = message;
        this.type      = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getMessage()   { return message; }
    public int    getType()      { return type; }
    public long   getTimestamp() { return timestamp; }

    public void setMessage(String message) { this.message = message; }
}
