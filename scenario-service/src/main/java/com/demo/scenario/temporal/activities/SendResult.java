package com.demo.scenario.temporal.activities;

public class SendResult {
    private String messageId;
    private boolean accepted;
    private String channel;

    public SendResult() {}

    public SendResult(String messageId, boolean accepted, String channel) {
        this.messageId = messageId;
        this.accepted  = accepted;
        this.channel   = channel;
    }

    public String getMessageId()  { return messageId; }
    public boolean isAccepted()   { return accepted; }
    public String getChannel()    { return channel; }

    public void setMessageId(String messageId) { this.messageId = messageId; }
    public void setAccepted(boolean accepted)  { this.accepted = accepted; }
    public void setChannel(String channel)     { this.channel = channel; }
}
