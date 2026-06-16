package com.YOUNES.AI;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageModel {

    public static final int TYPE_USER    = 1;
    public static final int TYPE_BOT     = 2;
    public static final int TYPE_LOADING = 3;

    private String message;
    private int    type;
    private long   timestamp;
    private String timeFormatted;
    private String modelName;
    private String emotion;

    public MessageModel(String message, int type) {
        this.message       = message;
        this.type          = type;
        this.timestamp     = System.currentTimeMillis();
        this.timeFormatted = new SimpleDateFormat(
                "HH:mm", Locale.getDefault())
                .format(new Date(this.timestamp));
    }

    public MessageModel(String message, int type, String modelName) {
        this(message, type);
        this.modelName = modelName;
    }

    public String getMessage()       { return message; }
    public int    getType()          { return type; }
    public long   getTimestamp()     { return timestamp; }
    public String getTimeFormatted() { return timeFormatted; }
    public String getModelName()     { return modelName; }
    public String getEmotion()       { return emotion; }

    public void setMessage(String message)   { this.message   = message; }
    public void setModelName(String name)    { this.modelName = name; }
    public void setEmotion(String emotion)   { this.emotion   = emotion; }
}
