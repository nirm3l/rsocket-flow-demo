package com.flow.demo.taskservice.service;

import com.fasterxml.uuid.Generators;
import com.flow.demo.taskservice.generated.api.model.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TaskService {

    private final Map<UUID, Task> tasks = new ConcurrentSkipListMap<>();

    private final NotificationService notificationService;

    public TaskService(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    public Mono<Task> createTask(Task task) {
        UUID taskId = Generators.timeBasedGenerator().generate();

        task.setId(taskId);

        //Mock tags and comments for benchmark purpose
        task.setTags(IntStream.range(0, 10).mapToObj(
                i -> new Tag().id(UUID.randomUUID()).value("Tag " + i)).collect(Collectors.toList()));
        task.setComments(IntStream.range(0, 10).mapToObj(
                i -> new Comment().id(UUID.randomUUID())
                        .userId(UUID.randomUUID())
                        .value("Comment " + i)).collect(Collectors.toList()));

        tasks.put(taskId, task);

        return notificationService.sendEvent(new NewTaskEvent()
                .task(task)
                .userId(task.getCreatedById())
                .eventType(NewTaskEvent.class.getSimpleName())).thenReturn(task);
    }

    public Collection<Task> getTasks() {
        return tasks.values();
    }

    public Mono<Task> getTask(UUID taskId) {
        return Mono.justOrEmpty(tasks.get(taskId));
    }

    public Mono<Task> deleteTask(UUID taskId) {
        Task task = tasks.remove(taskId);

        if(task == null) {
            return Mono.empty();
        }

        return notificationService.sendEvent(new TaskDeletedEvent()
                .task(task)
                .userId(task.getCreatedById())
                .eventType(TaskDeletedEvent.class.getSimpleName())).thenReturn(task);
    }
}
