package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;

@KafkaEvent(name = "USER_REGISTERED", topic = "user.registered", description = "New user signed up")
public class UserRegisteredEvent extends BaseEvent {
    public String email;
    public String channel; // email | push | sms
    public UserRegisteredEvent() {}
    public UserRegisteredEvent(String userId, String email, String channel) {
        super(userId); this.email = email; this.channel = channel;
    }
}
