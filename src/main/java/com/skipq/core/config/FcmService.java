package com.skipq.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.skipq.core.auth.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class FcmService {

    @Value("${app.firebase.project-id:}")
    private String projectId;

    private boolean initialized = false;

    @PostConstruct
    public void init() {
        if (projectId.isBlank()) {
            log.warn("Firebase not configured — push notifications disabled");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
            }
            initialized = true;
            log.info("Firebase initialized for project '{}'", projectId);
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }

    public void sendToUser(User user, String title, String body) {
        if (!initialized || user.getFcmToken() == null || user.getFcmToken().isBlank()) return;
        try {
            Message message = Message.builder()
                    .setToken(user.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.error("Failed to send push notification to user {}: {}", user.getId(), e.getMessage());
        }
    }
}
