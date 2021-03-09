package com.flow.demo.gateway.services;

import com.flow.demo.gateway.generated.model.BaseEvent;
import com.flow.demo.gateway.generated.model.NotificationChannelEvent;
import com.flow.demo.gateway.generated.model.PingEvent;
import io.rsocket.routing.client.spring.RoutingRSocketRequester;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.UUID;

@Service
public class EventService {

    private final RoutingRSocketRequester requester;

    public EventService(RoutingRSocketRequester requester) {
        this.requester = requester;
    }

    public Flux<ServerSentEvent<BaseEvent>> subscribeEvents(UUID userId) {
        Sinks.Many<NotificationChannelEvent> replaySink = Sinks.many().unicast().onBackpressureBuffer();

        replaySink.tryEmitNext(NotificationChannelEvent.OPENED);

        return Flux.merge(getHeartbeatEvents(replaySink),
                requester.route("/notification-channel/{userId}", userId)
                    .address("notification-service")
                    .data(replaySink.asFlux())
                    .retrieveFlux(BaseEvent.class).retryWhen(Retry.indefinitely()))
                                .map(value -> ServerSentEvent.<BaseEvent>builder()
                                        .data(value)
                                        .build());
    }

    public Flux<PingEvent> getHeartbeatEvents(Sinks.Many<NotificationChannelEvent> replaySink) {
        return Flux.interval(Duration.ofSeconds(5))
                .map(i -> new PingEvent())
                .doFinally(signalType -> replaySink.tryEmitNext(NotificationChannelEvent.CLOSED));
    }
}
