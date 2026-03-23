package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="LINK_CLICKED", topic="link.clicked", description="User clicked a tracked link")
public class LinkClickedEvent extends BaseEvent {
    public String messageId; public String url;
    public LinkClickedEvent() {}
    public LinkClickedEvent(String userId, String messageId, String url) {
        super(userId); this.messageId = messageId; this.url = url;
    }
}
