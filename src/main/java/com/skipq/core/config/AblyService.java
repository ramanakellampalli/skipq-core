package com.skipq.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.ably.lib.rest.AblyRest;
import io.ably.lib.rest.Channel;
import io.ably.lib.types.AblyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AblyService {

    private final AblyRest ably;
    private final ObjectMapper objectMapper;

    public AblyService(@Value("${ABLY_API_KEY}") String apiKey, ObjectMapper objectMapper) throws AblyException {
        this.ably = new AblyRest(apiKey);
        this.objectMapper = objectMapper;
    }

    public void publish(String channelName, String eventName, Object data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            Channel channel = ably.channels.get(channelName);
            channel.publish(eventName, json);
        } catch (Exception e) {
            log.error("Ably publish failed on channel {}: {}", channelName, e.getMessage());
        }
    }
}
