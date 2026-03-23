package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="USER_UNSUBSCRIBED", topic="user.unsubscribed", description="User opted out of communications")
public class UserUnsubscribedEvent extends BaseEvent {
    public String channel;
    public UserUnsubscribedEvent() {}
    public UserUnsubscribedEvent(String userId, String channel) { super(userId); this.channel = channel; }
}
