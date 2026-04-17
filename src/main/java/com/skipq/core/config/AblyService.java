package com.skipq.core.config;

import com.google.gson.Gson;
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
    private final Gson gson = new Gson();

    public AblyService(@Value("${ABLY_API_KEY}") String apiKey) throws AblyException {
        this.ably = new AblyRest(apiKey);
    }

    public void publish(String channelName, String eventName, Object data) {
        try {
            Channel channel = ably.channels.get(channelName);
            channel.publish(eventName, gson.toJson(data));
        } catch (AblyException e) {
            log.error("Ably publish failed on channel {}: {}", channelName, e.getMessage());
        }
    }
}
