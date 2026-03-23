package com.demo.scenario.temporal.workflows;

public class IncomingEvent {
    private String eventType;
    private String topic;
    private String userId;
    private String payload;

    public IncomingEvent() {}

    public IncomingEvent(String eventType, String topic, String userId, String payload) {
        this.eventType = eventType;
        this.topic     = topic;
        this.userId    = userId;
        this.payload   = payload;
    }

    public String getEventType() { return eventType; }
    public String getTopic()     { return topic; }
    public String getUserId()    { return userId; }
    public String getPayload()   { return payload; }

    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setTopic(String topic)         { this.topic = topic; }
    public void setUserId(String userId)       { this.userId = userId; }
    public void setPayload(String payload)     { this.payload = payload; }
}
