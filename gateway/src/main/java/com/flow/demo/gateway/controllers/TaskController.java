package com.flow.demo.gateway.controllers;

import com.flow.demo.gateway.generated.model.Task;
import com.flow.demo.gateway.generated.model.User;
import io.rsocket.routing.client.spring.RoutingRSocketRequester;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class TaskController {

    private final RoutingRSocketRequester requester;

    private final WebClient userServiceClient;

    private final WebClient taskServiceClient;

    public TaskController(
            @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder loadBalancedWebClientBuilder,
            RoutingRSocketRequester requester) {
        this.requester = requester;
        this.userServiceClient = loadBalancedWebClientBuilder
                .baseUrl("lb://user-service")
                .defaultHeader("Accept", "application/stream+json, application/json")
                .build();

        this.taskServiceClient = loadBalancedWebClientBuilder
                .baseUrl("lb://task-service")
                .defaultHeader("Accept", "application/stream+json, application/json")
                .build();
    }

    @RequestMapping(value = "/v2/full-tasks",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public Mono<ResponseEntity<Flux<Task>>> getFullTasksV2() {
        return Mono.just(
                ResponseEntity.ok(requester.route("/stream/tasks")
                        .address("task-service").retrieveFlux(Task.class)
                        //.limitRate(50) apply backpressure by loading and processing max 50 tasks in time
                        .flatMap(task -> {
                            if(task.getCreatedById() != null) {
                                return requester.route("/users/{userId}", task.getCreatedById())
                                        .address("user-service").retrieveMono(User.class)
                                        .map(user -> {
                                            task.setCreatedBy(user);

                                            return task;
                                        });
                            }

                            return Mono.just(task);
                        })
                )
        );
    }

    @RequestMapping(value = "/full-tasks",
            produces = { "application/json" },
            method = RequestMethod.GET)
    public Mono<ResponseEntity<Flux<Task>>> getFullTasks() {
        return Mono.just(
                ResponseEntity.ok(taskServiceClient.get().uri("/tasks")
                        .header("Cache-Control", "no-store no-cache must-revalidate")
                        .retrieve().bodyToFlux(Task.class)
                        .flatMap(task -> {
                            if(task.getCreatedById() != null) {
                                return userServiceClient.get().uri("/users/{userId}", task.getCreatedById())
                                        .header("Cache-Control", "no-store no-cache must-revalidate")
                                        .retrieve().bodyToMono(User.class)
                                        .map(user -> {
                                            task.setCreatedBy(user);

                                            return task;
                                        });
                            }

                            return Mono.just(task);
                        })
                )
        );
    }
}
