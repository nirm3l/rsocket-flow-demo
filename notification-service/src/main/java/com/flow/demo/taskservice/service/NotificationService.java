package com.flow.demo.taskservice.service;

import com.flow.demo.notificationservice.generated.api.model.BaseEvent;
import com.flow.demo.notificationservice.generated.api.model.NotificationChannelEvent;
import com.flow.demo.notificationservice.generated.api.model.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final Map<UUID, Set<Sinks.Many<BaseEvent>>> userNotificationConnections = new ConcurrentHashMap<>();

    public Flux<BaseEvent> handleNotifications(UUID userId, Flux<NotificationChannelEvent> events) {
        LOGGER.info("Connection added for user {}.", userId);

        Sinks.Many<BaseEvent> replaySink = Sinks.many().unicast().onBackpressureBuffer();

        if (!userNotificationConnections.containsKey(userId)) {
            userNotificationConnections.put(userId, new HashSet<>());
        }

        userNotificationConnections.get(userId).add(replaySink);

        return events.switchMap(
                event -> {
                    if(event == NotificationChannelEvent.OPENED) {
                        return replaySink.asFlux()
                                .doOnError(e -> {
                                   removeSink(userId, replaySink);
                                });
                    }

                    removeSink(userId, replaySink);

                    return Mono.empty();
                }
        );
    }

    private void removeSink(UUID userId, Sinks.Many<BaseEvent> replaySink) {
        Set<Sinks.Many<BaseEvent>> userConnections = userNotificationConnections.get(userId);

        userConnections.remove(replaySink);

        replaySink.tryEmitComplete();

        if(userConnections.isEmpty()) {
            userNotificationConnections.remove(userId);
        }

        LOGGER.info("Connection removed for user {}.", userId);
    }

    public void handleEvent(BaseEvent event) {
        if(event instanceof UserEvent) {
            UUID userId = ((UserEvent) event).getUserId();

            Set<Sinks.Many<BaseEvent>> notificationConnections = userNotificationConnections.get(userId);

            if(notificationConnections != null) {
                notificationConnections.forEach(sink -> {
                    sink.tryEmitNext(event);
                });
            }
        }
    }
}
