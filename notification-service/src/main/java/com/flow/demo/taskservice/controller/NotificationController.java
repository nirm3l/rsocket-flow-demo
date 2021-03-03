package com.flow.demo.taskservice.controller;

import com.flow.demo.notificationservice.generated.api.model.BaseEvent;
import com.flow.demo.notificationservice.generated.api.model.NotificationChannelEvent;
import com.flow.demo.taskservice.service.NotificationService;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @MessageMapping("/notification-channel/{userId}")
    public Flux<BaseEvent> newUserNotificationChannel(
            @DestinationVariable UUID userId, Flux<NotificationChannelEvent> event) {
        return notificationService.handleNotifications(userId, event);
    }

    @MessageMapping("/handle-event")
    public void handleEvent(BaseEvent event) {
        notificationService.handleEvent(event);
    }
}
