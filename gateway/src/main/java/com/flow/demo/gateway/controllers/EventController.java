package com.flow.demo.gateway.controllers;

import com.flow.demo.gateway.generated.model.BaseEvent;
import com.flow.demo.gateway.services.EventService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @RequestMapping(
            value = "/v2/realtime-stream/{userId}",
            produces = { MediaType.TEXT_EVENT_STREAM_VALUE },
            method = RequestMethod.GET)
    public Mono<ResponseEntity<Flux<ServerSentEvent<BaseEvent>>>> getRealtimeEvents(
            @PathVariable("userId") UUID userId) {
        return Mono.just(ResponseEntity.ok(eventService.subscribeEvents(userId)));
    }
}
