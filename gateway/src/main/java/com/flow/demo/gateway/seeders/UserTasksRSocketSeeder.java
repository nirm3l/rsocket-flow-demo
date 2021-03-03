package com.flow.demo.gateway.seeders;

import com.flow.demo.gateway.generated.model.Task;
import com.flow.demo.gateway.generated.model.User;
import io.rsocket.routing.client.spring.RoutingRSocketRequester;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Random;
import java.util.UUID;


@Service
public class UserTasksRSocketSeeder extends UserTasksBaseSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserTasksRSocketSeeder.class);

    private final RoutingRSocketRequester requester;

    public UserTasksRSocketSeeder(RoutingRSocketRequester requester) {
        this.requester = requester;
    }

    public Mono<User> seedUser(User user) {
        return requester.route("/users")
                .address("user-service")
                .data(user)
                .retrieveMono(User.class);
    }

    public Mono<Task> seedTask(Task task) {
        return requester.route("/tasks")
                .address("task-service")
                .data(task)
                .retrieveMono(Task.class);
    }

    public void seed() {
        super.seed();

        String random = UUID.randomUUID().toString();

        seedUser(getUser(String.format("User%s", random), String.format("user%s@test.com", random)))
                .flatMapMany(user -> {
                    LOGGER.info("Seeding tasks for user: {}", user.getId());

                    return Flux.interval(Duration.ofMillis(1000))
                            .flatMap(period -> seedTask(getTask(user, new Random().nextInt(100)))
                                    .onErrorResume(e -> {
                                        LOGGER.info("Dropped task: " + e);

                                        return Mono.empty();
                                    })
                            );
                }).subscribe();
    }
}
