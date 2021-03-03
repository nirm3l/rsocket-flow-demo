package com.flow.demo.taskservice.controller;

import com.flow.demo.taskservice.generated.api.TaskApi;
import com.flow.demo.taskservice.generated.api.model.Task;
import com.flow.demo.taskservice.service.TaskService;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import java.util.UUID;

@RestController
public class TaskController implements TaskApi {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @RequestMapping(value = "/tasks/{taskId}",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public Mono<ResponseEntity<Task>> getTask(
            @PathVariable("taskId") UUID taskId, ServerWebExchange exchange) {
        return taskService.getTask(taskId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @RequestMapping(value = "/tasks",
            produces = {"application/json"},
            consumes = {"application/json"},
            method = RequestMethod.POST)
    public Mono<ResponseEntity<Task>> createTask(
            @Valid @RequestBody Mono<Task> taskMono, ServerWebExchange exchange) {
        return taskMono.flatMap(taskService::createTask).map(ResponseEntity::ok);
    }

    @RequestMapping(value = "/tasks",
            produces = {"application/json"},
            method = RequestMethod.GET)
    public Mono<ResponseEntity<Flux<Task>>> getTasks(ServerWebExchange exchange) {
        return Mono.just(ResponseEntity.ok(Flux.fromIterable(taskService.getTasks())));
    }

    @RequestMapping(value = "/tasks/{taskId}",
            produces = { "application/json" },
            method = RequestMethod.DELETE)
    public Mono<ResponseEntity<Task>> deleteTask(
            @PathVariable("taskId") UUID taskId, ServerWebExchange exchange) {
        return taskService.deleteTask(taskId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @MessageMapping("/tasks/{taskId}")
    public Mono<Task> task(
            @DestinationVariable UUID taskId, @Header(
                    value = "method", required = false) HttpMethod method) {
        return (HttpMethod.DELETE == method ? taskService.deleteTask(taskId) : taskService.getTask(taskId))
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    @MessageMapping("/tasks")
    public Mono<?> tasks(@Payload(required = false) Task task) {
        if(task != null) {
            return taskService.createTask(task);
        }

        return Mono.just(taskService.getTasks());
    }

    @MessageMapping("/stream/tasks")
    public Flux<Task> tasks() {
        return Flux.fromIterable(taskService.getTasks());
    }
}
