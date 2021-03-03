package com.flow.demo.taskservice.service;

import com.flow.demo.taskservice.generated.api.model.BaseEvent;
import io.rsocket.routing.client.spring.RoutingRSocketRequester;
import io.rsocket.routing.common.WellKnownKey;
import io.rsocket.routing.frames.RoutingType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class NotificationService {

    private final RoutingRSocketRequester requester;

    public NotificationService(RoutingRSocketRequester requester) {
        this.requester = requester;
    }

    public Mono<Void> sendEvent(BaseEvent event) {
        return requester.route("/handle-event")
                .address(a -> {
                    a.with(WellKnownKey.SERVICE_NAME, "notification-service");
                    a.routingType(RoutingType.MULTICAST); // Send event to all instances of notification-service
                }).data(event).send();
    }
}
