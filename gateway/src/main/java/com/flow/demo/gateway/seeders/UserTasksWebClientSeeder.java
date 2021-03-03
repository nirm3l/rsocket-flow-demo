package com.flow.demo.gateway.seeders;

import com.flow.demo.gateway.generated.model.Task;
import com.flow.demo.gateway.generated.model.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

//@Service
public class UserTasksWebClientSeeder extends UserTasksBaseSeeder {
    private final WebClient userServiceClient;

    private final WebClient taskServiceClient;

    public UserTasksWebClientSeeder(
            @Qualifier("loadBalancedWebClientBuilder") WebClient.Builder loadBalancedWebClientBuilder) {
        this.userServiceClient = loadBalancedWebClientBuilder
                .baseUrl("lb://user-service")
                .defaultHeader("Accept", "application/stream+json, application/json")
                .build();

        this.taskServiceClient = loadBalancedWebClientBuilder
                .baseUrl("lb://task-service")
                .defaultHeader("Accept", "application/stream+json, application/json")
                .build();
    }

    public Mono<User> seedUser(User user) {
        return userServiceClient.post().uri("/users")
                .bodyValue(user).retrieve().bodyToMono(User.class);
    }

    public Mono<Task> seedTask(Task task) {
        return taskServiceClient.post().uri("/tasks")
                .bodyValue(task).retrieve().bodyToMono(Task.class);
    }
}
